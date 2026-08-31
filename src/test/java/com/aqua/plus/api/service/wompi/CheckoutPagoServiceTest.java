package com.aqua.plus.api.service.wompi;

import com.aqua.plus.api.utils.EncriptarDesencriptar;
import com.aqua.plus.api.wompi.WompiReferenceGenerator;
import com.aqua.plus.api.wompi.WompiSignatureService;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.CheckoutPagoRequest;
import com.aqua.plus.commons.dtos.external.CheckoutPagoResponse;
import com.aqua.plus.commons.entities.EmpresaClienteContadorEntity;
import com.aqua.plus.commons.entities.EmpresaEntity;
import com.aqua.plus.commons.entities.EmpresaWompiEntity;
import com.aqua.plus.commons.entities.EstadoEntity;
import com.aqua.plus.commons.entities.FacturaEntity;
import com.aqua.plus.commons.entities.PersonaEntity;
import com.aqua.plus.commons.entities.UsuarioEntity;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import com.aqua.plus.commons.exceptions.SecureRequestException;
import com.aqua.plus.commons.repositories.EmpresaRepository;
import com.aqua.plus.commons.repositories.EmpresaWompiRepository;
import com.aqua.plus.commons.repositories.FacturaRepository;
import com.aqua.plus.commons.repositories.PagoRepository;
import com.aqua.plus.commons.repositories.UsuarioRepository;
import com.aqua.plus.commons.utils.Constantes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutPagoServiceTest {

    @Mock FacturaRepository facturaRepository;
    @Mock PagoRepository pagoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock EmpresaWompiRepository empresaWompiRepository;
    @Mock EncriptarDesencriptar encriptarDesencriptar;
    @Mock WompiSignatureService signatureService;
    @Mock WompiReferenceGenerator referenceGenerator;

    @InjectMocks CheckoutPagoService service;

    @BeforeEach
    void auth() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("cliente1", "n/a"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void facturaInexistente() {
        when(usuarioRepository.findByNombre("cliente1")).thenReturn(Optional.of(usuarioCliente(10, 100)));
        when(facturaRepository.findActivaByIdWithRelations(999)).thenReturn(Optional.empty());

        CheckoutPagoRequest req = new CheckoutPagoRequest();
        req.setFacturaId(999);

        assertThrows(ProcessGenericException.class, () -> service.crearCheckout(req));
    }

    @Test
    void facturaYaPagada() {
        FacturaEntity factura = facturaBase(84521, 1, 100, 215000.0, "PAG", "PAGADA");
        when(usuarioRepository.findByNombre("cliente1")).thenReturn(Optional.of(usuarioCliente(10, 100)));
        when(facturaRepository.findActivaByIdWithRelations(84521)).thenReturn(Optional.of(factura));

        CheckoutPagoRequest req = new CheckoutPagoRequest();
        req.setFacturaId(84521);

        SecureRequestException ex = assertThrows(SecureRequestException.class, () -> service.crearCheckout(req));
        assertTrue(ex.getMessage().toLowerCase().contains("pagada"));
    }

    @Test
    void empresaSinRedirectUrl() {
        FacturaEntity factura = facturaBase(84521, 1, 100, 215000.0, "PEN", "PENDIENTE");
        when(usuarioRepository.findByNombre("cliente1")).thenReturn(Optional.of(usuarioCliente(10, 100)));
        when(facturaRepository.findActivaByIdWithRelations(84521)).thenReturn(Optional.of(factura));

        EmpresaWompiEntity config = new EmpresaWompiEntity();
        config.setActivo(true);
        config.setWompiClavePublica("pub_test_x");
        config.setWompiSecretoIntegridad("enc-int");
        config.setWompiSecretoEventos("enc-evt");
        config.setCheckoutUrl("https://checkout.wompi.co/p/");
        config.setRedirectUrl(null);
        when(empresaWompiRepository.findByEmpresa_Id(1)).thenReturn(Optional.of(config));

        CheckoutPagoRequest req = new CheckoutPagoRequest();
        req.setFacturaId(84521);

        assertThrows(ProcessGenericException.class, () -> service.crearCheckout(req));
    }

    @Test
    void checkoutExitosoIncluyeComisionWompi() {
        FacturaEntity factura = facturaBase(84521, 1, 100, 215000.0, "PEN", "PENDIENTE");
        when(usuarioRepository.findByNombre("cliente1")).thenReturn(Optional.of(usuarioCliente(10, 100)));
        when(facturaRepository.findActivaByIdWithRelations(84521)).thenReturn(Optional.of(factura));

        EmpresaWompiEntity config = new EmpresaWompiEntity();
        config.setActivo(true);
        config.setWompiClavePublica("pub_test_x");
        config.setWompiSecretoIntegridad("enc-int");
        config.setWompiSecretoEventos("enc-evt");
        config.setCheckoutUrl("https://checkout.wompi.co/p/");
        config.setRedirectUrl("https://front.example/pagos/resultado");
        when(empresaWompiRepository.findByEmpresa_Id(1)).thenReturn(Optional.of(config));
        when(encriptarDesencriptar.desencriptar("enc-int")).thenReturn("integrity-secret");
        when(encriptarDesencriptar.desencriptar("enc-evt")).thenReturn("event-secret");
        when(referenceGenerator.generar(84521)).thenReturn("FAC-84521-20260826-A8F31");
        when(signatureService.generarFirmaIntegridad(anyString(), anyLong(), anyString(), anyString()))
                .thenReturn("firma-sha256");
        when(pagoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CheckoutPagoRequest req = new CheckoutPagoRequest();
        req.setFacturaId(84521);

        ResponseEntity<ResponseDTO> response = service.crearCheckout(req);
        CheckoutPagoResponse body = (CheckoutPagoResponse) response.getBody().getResponse();

        assertEquals(22261303L, body.getAmountInCents());
        assertEquals(21500000L, body.getFacturaAmountInCents());
        assertEquals(761303L, body.getFeeTotalInCents());
        assertEquals("FAC-84521-20260826-A8F31", body.getReference());
        assertEquals("pub_test_x", body.getPublicKey());
        assertEquals("firma-sha256", body.getSignatureIntegrity());
        assertTrue(body.getRedirectUrl().contains("facturaId=84521"));
        assertTrue(body.getPaymentUrl() != null && body.getPaymentUrl().contains("public-key="));
        assertTrue(body.getPaymentUrl().contains("amount-in-cents=22261303"));
        assertTrue(body.getPaymentUrl().contains("reference=FAC-84521-20260826-A8F31"));
        assertTrue(body.getPaymentUrl().contains("signature%3Aintegrity="));
        assertFalse(body.getPaymentUrl().contains("&signature:integrity="));

        ArgumentCaptor<com.aqua.plus.commons.entities.PagoEntity> captor =
                ArgumentCaptor.forClass(com.aqua.plus.commons.entities.PagoEntity.class);
        verify(pagoRepository).save(captor.capture());
        assertEquals(Constantes.PAGO_ESTADO_PENDING, captor.getValue().getEstado());
        assertEquals(22261303L, captor.getValue().getMontoCentavos());
    }

    private UsuarioEntity usuarioCliente(Integer userId, Integer personaId) {
        UsuarioEntity u = new UsuarioEntity();
        u.setId(userId);
        u.setNombre("cliente1");
        PersonaEntity p = new PersonaEntity();
        p.setId(personaId);
        u.setPersona(p);
        return u;
    }

    private FacturaEntity facturaBase(Integer id, Integer empresaId, Integer clienteId,
                                      Double precio, String codigoEstado, String nombreEstado) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(empresaId);

        PersonaEntity cliente = new PersonaEntity();
        cliente.setId(clienteId);

        EmpresaClienteContadorEntity ecc = new EmpresaClienteContadorEntity();
        ecc.setEmpresa(empresa);
        ecc.setCliente(cliente);

        EstadoEntity estado = new EstadoEntity();
        estado.setCodigo(codigoEstado);
        estado.setNombre(nombreEstado);
        estado.setActivo(true);

        FacturaEntity factura = new FacturaEntity();
        factura.setId(id);
        factura.setPrecio(precio);
        factura.setActivo(true);
        factura.setEmpresaClienteContador(ecc);
        factura.setEstado(estado);
        return factura;
    }
}
