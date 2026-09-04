package com.aqua.plus.api.service.wompi;

import com.aqua.plus.api.service.IFacturaService;
import com.aqua.plus.api.wompi.WompiEmpresaConfig;
import com.aqua.plus.api.wompi.WompiWebhookSecurityService;
import com.aqua.plus.commons.dtos.FacturaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.WebhookEventDTO;
import com.aqua.plus.commons.entities.EstadoEntity;
import com.aqua.plus.commons.entities.PagoEntity;
import com.aqua.plus.commons.exceptions.SecureRequestException;
import com.aqua.plus.commons.repositories.EstadoRepository;
import com.aqua.plus.commons.repositories.PagoRepository;
import com.aqua.plus.commons.utils.Constantes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WompiWebhookServiceTest {

    @Mock PagoRepository pagoRepository;
    @Mock EstadoRepository estadoRepository;
    @Mock IFacturaService facturaService;
    @Mock CheckoutPagoService checkoutPagoService;
    @Mock WompiWebhookSecurityService webhookSecurityService;

    @InjectMocks WompiWebhookService service;

    private WompiEmpresaConfig config;

    @BeforeEach
    void setUp() {
        config = new WompiEmpresaConfig(
                "pub_test", "integrity", "event-secret",
                "https://checkout.wompi.co/p/",
                "https://front/pagos/resultado");
    }

    @Test
    void checksumInvalidoLanzaUnauthorized() {
        PagoEntity pago = pagoPendiente();
        when(pagoRepository.findByReferencia("FAC-1")).thenReturn(Optional.of(pago));
        when(checkoutPagoService.cargarConfigEmpresa(1)).thenReturn(config);
        when(webhookSecurityService.validarChecksum(any(), anyString(), anyString())).thenReturn(false);

        assertThrows(SecureRequestException.class,
                () -> service.procesar(evento("APPROVED", 21500000L, "COP"), "bad"));
        verify(pagoRepository, never()).actualizarEstadoSiPendiente(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void approvedActualizaPagoYFacturaViaFacturaService() {
        PagoEntity pago = pagoPendiente();
        when(pagoRepository.findByReferencia("FAC-1")).thenReturn(Optional.of(pago));
        when(checkoutPagoService.cargarConfigEmpresa(1)).thenReturn(config);
        when(webhookSecurityService.validarChecksum(any(), anyString(), anyString())).thenReturn(true);
        when(pagoRepository.actualizarEstadoSiPendiente(eq("FAC-1"), eq("APPROVED"), anyString(), anyString(), anyString()))
                .thenReturn(1);

        EstadoEntity pag = new EstadoEntity();
        pag.setId(12);
        pag.setCodigo("PAG");
        pag.setNombre("PAGADA");
        when(estadoRepository.findByCodigoIgnoreCaseAndActivoTrue("PAG")).thenReturn(Optional.of(pag));
        when(facturaService.update(any(FacturaDTO.class))).thenReturn(
                ResponseEntity.ok(ResponseDTO.builder().success(true).message("ok").build()));

        service.procesar(evento("APPROVED", 21500000L, "COP"), "ok");

        ArgumentCaptor<FacturaDTO> captor = ArgumentCaptor.forClass(FacturaDTO.class);
        verify(facturaService).update(captor.capture());
        FacturaDTO dto = captor.getValue();
        assertEquals(84521, dto.getId());
        assertEquals(12, dto.getEstado().getId());
        assertEquals("PAG", dto.getEstado().getCodigo());
        assertEquals("cliente1", dto.getUsuarioModificacion());
    }

    @Test
    void declinedNoTocaFactura() {
        stubTerminalSinFactura("DECLINED");
        service.procesar(evento("DECLINED", 21500000L, "COP"), "ok");
        verify(facturaService, never()).update(any());
    }

    @Test
    void errorNoTocaFactura() {
        stubTerminalSinFactura("ERROR");
        service.procesar(evento("ERROR", 21500000L, "COP"), "ok");
        verify(facturaService, never()).update(any());
    }

    @Test
    void voidedNoTocaFactura() {
        stubTerminalSinFactura("VOIDED");
        service.procesar(evento("VOIDED", 21500000L, "COP"), "ok");
        verify(facturaService, never()).update(any());
    }

    @Test
    void webhookDuplicadoEsIdempotente() {
        PagoEntity pago = pagoPendiente();
        pago.setEstado("APPROVED");
        pago.setIdTransaccionWompi("tx_123");
        when(pagoRepository.findByReferencia("FAC-1")).thenReturn(Optional.of(pago));
        when(checkoutPagoService.cargarConfigEmpresa(1)).thenReturn(config);
        when(webhookSecurityService.validarChecksum(any(), anyString(), anyString())).thenReturn(true);

        assertDoesNotThrow(() -> service.procesar(evento("APPROVED", 21500000L, "COP"), "ok"));
        verify(pagoRepository, never()).actualizarEstadoSiPendiente(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(facturaService, never()).update(any());
    }

    @Test
    void montoDiferenteNoAprueba() {
        PagoEntity pago = pagoPendiente();
        when(pagoRepository.findByReferencia("FAC-1")).thenReturn(Optional.of(pago));
        when(checkoutPagoService.cargarConfigEmpresa(1)).thenReturn(config);
        when(webhookSecurityService.validarChecksum(any(), anyString(), anyString())).thenReturn(true);

        service.procesar(evento("APPROVED", 999L, "COP"), "ok");

        verify(pagoRepository, never()).actualizarEstadoSiPendiente(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void currencyDiferenteNoAprueba() {
        PagoEntity pago = pagoPendiente();
        when(pagoRepository.findByReferencia("FAC-1")).thenReturn(Optional.of(pago));
        when(checkoutPagoService.cargarConfigEmpresa(1)).thenReturn(config);
        when(webhookSecurityService.validarChecksum(any(), anyString(), anyString())).thenReturn(true);

        service.procesar(evento("APPROVED", 21500000L, "USD"), "ok");

        verify(pagoRepository, never()).actualizarEstadoSiPendiente(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void referenciaInexistenteNoFalla() {
        when(pagoRepository.findByReferencia("FAC-1")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.procesar(evento("APPROVED", 21500000L, "COP"), "ok"));
        verify(webhookSecurityService, never()).validarChecksum(any(), any(), any());
    }

    private void stubTerminalSinFactura(String estado) {
        PagoEntity pago = pagoPendiente();
        when(pagoRepository.findByReferencia("FAC-1")).thenReturn(Optional.of(pago));
        when(checkoutPagoService.cargarConfigEmpresa(1)).thenReturn(config);
        when(webhookSecurityService.validarChecksum(any(), anyString(), anyString())).thenReturn(true);
        when(pagoRepository.actualizarEstadoSiPendiente(eq("FAC-1"), eq(estado), anyString(), anyString(), anyString()))
                .thenReturn(1);
    }

    private PagoEntity pagoPendiente() {
        return PagoEntity.builder()
                .id(1)
                .idUsuario(10)
                .idFactura(84521)
                .idEmpresa(1)
                .referencia("FAC-1")
                .montoCentavos(21500000L)
                .moneda("COP")
                .estado(Constantes.PAGO_ESTADO_PENDING)
                .metodoPago(Constantes.PAGO_METODO_WEB_CHECKOUT)
                .usuarioCreacion("cliente1")
                .build();
    }

    private WebhookEventDTO evento(String status, Long amount, String currency) {
        Map<String, Object> tx = new LinkedHashMap<>();
        tx.put("id", "tx_123");
        tx.put("status", status);
        tx.put("amount_in_cents", amount);
        tx.put("currency", currency);
        tx.put("reference", "FAC-1");
        tx.put("payment_method_type", "CARD");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transaction", tx);

        Map<String, Object> signature = new LinkedHashMap<>();
        signature.put("properties", List.of("transaction.id", "transaction.status", "transaction.amount_in_cents"));
        signature.put("checksum", "ok");

        WebhookEventDTO evento = new WebhookEventDTO();
        evento.setEvent("transaction.updated");
        evento.setData(data);
        evento.setSignature(signature);
        evento.setTimestamp(1L);
        return evento;
    }
}
