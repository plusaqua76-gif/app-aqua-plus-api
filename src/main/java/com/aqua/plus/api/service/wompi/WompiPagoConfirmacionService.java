package com.aqua.plus.api.service.wompi;

import com.aqua.plus.api.service.IFacturaService;
import com.aqua.plus.commons.dtos.EstadoDTO;
import com.aqua.plus.commons.dtos.FacturaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.EstadoEntity;
import com.aqua.plus.commons.entities.PagoEntity;
import com.aqua.plus.commons.repositories.EstadoRepository;
import com.aqua.plus.commons.repositories.PagoRepository;
import com.aqua.plus.commons.utils.Constantes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Aplica un estado terminal de Wompi al pago y, si fue aprobado, marca la factura como PAGADA.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WompiPagoConfirmacionService {

    private static final Set<String> ESTADOS_TERMINALES = Set.of(
            Constantes.PAGO_ESTADO_APPROVED,
            Constantes.PAGO_ESTADO_DECLINED,
            Constantes.PAGO_ESTADO_ERROR,
            Constantes.PAGO_ESTADO_VOIDED
    );

    private final PagoRepository pagoRepository;
    private final EstadoRepository estadoRepository;
    private final IFacturaService facturaService;

    @Transactional
    public void aplicar(PagoEntity pago,
                        String estado,
                        String idWompi,
                        String metodoPago,
                        Long amountInCents,
                        String currency,
                        String origen) {
        if (pago == null || pago.getReferencia() == null) {
            return;
        }

        String referencia = pago.getReferencia();
        String estadoNormalizado = estado != null ? estado.trim().toUpperCase() : null;

        if (!ESTADOS_TERMINALES.contains(estadoNormalizado)) {
            log.info("Estado Wompi no terminal {} referencia={}", estadoNormalizado, referencia);
            return;
        }

        if (amountInCents == null || !amountInCents.equals(pago.getMontoCentavos())
                || currency == null || !currency.equalsIgnoreCase(pago.getMoneda())) {
            log.warn("Monto/currency Wompi no coinciden — referencia={} esperado={} {} recibido={} {}",
                    referencia, pago.getMontoCentavos(), pago.getMoneda(), amountInCents, currency);
            return;
        }

        if (ESTADOS_TERMINALES.contains(pago.getEstado())
                && pago.getEstado().equals(estadoNormalizado)
                && idWompi != null
                && idWompi.equals(pago.getIdTransaccionWompi())) {
            if (Constantes.PAGO_ESTADO_APPROVED.equals(estadoNormalizado)) {
                actualizarFacturaAlAprobado(referencia, origen);
            }
            log.info("Confirmación idempotente — referencia={} estado={} origen={}",
                    referencia, estadoNormalizado, origen);
            return;
        }

        if (ESTADOS_TERMINALES.contains(pago.getEstado())
                && !Constantes.PAGO_ESTADO_PENDING.equals(pago.getEstado())) {
            if (Constantes.PAGO_ESTADO_APPROVED.equals(pago.getEstado())) {
                actualizarFacturaAlAprobado(referencia, origen);
            }
            log.info("Pago ya procesado — referencia={} estadoActual={} evento={} origen={}",
                    referencia, pago.getEstado(), estadoNormalizado, origen);
            return;
        }

        String metodo = metodoPago != null ? metodoPago : pago.getMetodoPago();
        int updated = pagoRepository.actualizarEstadoSiPendiente(
                referencia, estadoNormalizado, idWompi, metodo, origen);

        if (updated == 0) {
            log.info("No se actualizó pago PENDING — posible carrera/idempotencia referencia={} origen={}",
                    referencia, origen);
            if (Constantes.PAGO_ESTADO_APPROVED.equals(estadoNormalizado)) {
                actualizarFacturaAlAprobado(referencia, origen);
            }
            return;
        }

        log.info("Pago actualizado — referencia={} estado={} origen={}", referencia, estadoNormalizado, origen);

        if (Constantes.PAGO_ESTADO_APPROVED.equals(estadoNormalizado)) {
            actualizarFacturaAlAprobado(referencia, origen);
        }
    }

    /**
     * Reintenta marcar la factura PAGADA si el pago ya está APPROVED (p. ej. falló el update anterior).
     */
    @Transactional
    public void asegurarFacturaPagada(PagoEntity pago, String origen) {
        if (pago == null || !Constantes.PAGO_ESTADO_APPROVED.equals(pago.getEstado())) {
            return;
        }
        actualizarFacturaAlAprobado(pago.getReferencia(), origen);
    }

    private void actualizarFacturaAlAprobado(String referencia, String origen) {
        try {
            PagoEntity pago = pagoRepository.findByReferencia(referencia)
                    .orElseThrow(() -> new IllegalStateException(
                            "Pago no encontrado para referencia: " + referencia));

            if (pago.getIdFactura() == null) {
                log.warn("Pago referencia: {} sin idFactura — se omite actualización de factura", referencia);
                return;
            }

            EstadoEntity estadoPagada = estadoRepository
                    .findByCodigoIgnoreCaseAndActivoTrue(Constantes.ESTADO_PAGADA)
                    .orElseThrow(() -> new IllegalStateException(
                            "Estado PAG no encontrado en configuracion.estado"));

            EstadoDTO estadoDto = EstadoDTO.builder()
                    .id(estadoPagada.getId())
                    .codigo(estadoPagada.getCodigo())
                    .nombre(estadoPagada.getNombre())
                    .build();

            FacturaDTO facturaDTO = new FacturaDTO();
            facturaDTO.setId(pago.getIdFactura());
            facturaDTO.setEstado(estadoDto);
            facturaDTO.setUsuarioModificacion(
                    pago.getUsuarioCreacion() != null ? pago.getUsuarioCreacion() : origen);

            ResponseEntity<ResponseDTO> respuesta = facturaService.update(facturaDTO);
            ResponseDTO body = respuesta.getBody();

            if (body != null && Boolean.TRUE.equals(body.getSuccess())) {
                log.info("Factura id={} marcada como PAGADA — referencia: {} origen={}",
                        pago.getIdFactura(), referencia, origen);
            } else {
                String msg = body != null ? body.getMessage() : "sin respuesta";
                log.error("Error al marcar factura como PAGADA — referencia: {} factura id={} motivo: {}",
                        referencia, pago.getIdFactura(), msg);
            }
        } catch (Exception e) {
            log.error("Error inesperado actualizando factura — referencia: {} — {}",
                    referencia, e.getMessage(), e);
        }
    }
}
