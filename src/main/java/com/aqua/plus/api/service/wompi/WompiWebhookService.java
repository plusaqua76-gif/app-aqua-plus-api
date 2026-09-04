package com.aqua.plus.api.service.wompi;

import com.aqua.plus.api.service.IFacturaService;
import com.aqua.plus.api.wompi.WompiEmpresaConfig;
import com.aqua.plus.api.wompi.WompiWebhookSecurityService;
import com.aqua.plus.commons.dtos.EstadoDTO;
import com.aqua.plus.commons.dtos.FacturaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.WebhookEventDTO;
import com.aqua.plus.commons.entities.EstadoEntity;
import com.aqua.plus.commons.entities.PagoEntity;
import com.aqua.plus.commons.exceptions.SecureRequestException;
import com.aqua.plus.commons.repositories.EstadoRepository;
import com.aqua.plus.commons.repositories.PagoRepository;
import com.aqua.plus.commons.utils.Constantes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WompiWebhookService {

    private static final String EVENTO_TRANSACCION_ACTUALIZADA = "transaction.updated";
    private static final String KEY_STATUS = "status";
    private static final String KEY_PAYMENT_METHOD_TYPE = "payment_method_type";
    private static final Set<String> ESTADOS_TERMINALES = Set.of(
            Constantes.PAGO_ESTADO_APPROVED,
            Constantes.PAGO_ESTADO_DECLINED,
            Constantes.PAGO_ESTADO_ERROR,
            Constantes.PAGO_ESTADO_VOIDED
    );

    private final PagoRepository pagoRepository;
    private final EstadoRepository estadoRepository;
    private final IFacturaService facturaService;
    private final CheckoutPagoService checkoutPagoService;
    private final WompiWebhookSecurityService webhookSecurityService;

    @Transactional
    @SuppressWarnings("unchecked")
    public void procesar(WebhookEventDTO evento, String firmaRecibida) {
        if (evento == null || !EVENTO_TRANSACCION_ACTUALIZADA.equals(evento.getEvent())) {
            log.info("Webhook ignorado — evento: {}", evento != null ? evento.getEvent() : null);
            return;
        }

        Map<String, Object> data = evento.getData();
        if (data == null || !(data.get("transaction") instanceof Map<?, ?>)) {
            log.warn("Webhook sin data.transaction — se ignora");
            return;
        }

        Map<String, Object> tx = (Map<String, Object>) data.get("transaction");
        String idWompi = asString(tx.get("id"));
        String estado = asString(tx.get(KEY_STATUS));
        String referencia = asString(tx.get("reference"));
        String metodoPago = asString(tx.get(KEY_PAYMENT_METHOD_TYPE));
        Long amountInCents = asLong(tx.get("amount_in_cents"));
        String currency = asString(tx.get("currency"));

        if (referencia == null || referencia.isBlank()) {
            log.warn("Webhook sin reference — se ignora");
            return;
        }

        PagoEntity pago = pagoRepository.findByReferencia(referencia).orElse(null);
        if (pago == null) {
            log.warn("Webhook referencia desconocida: {} — HTTP 200 sin aprobar", referencia);
            return;
        }

        WompiEmpresaConfig config = checkoutPagoService.cargarConfigEmpresa(pago.getIdEmpresa());

        if (!webhookSecurityService.validarChecksum(evento, firmaRecibida, config.eventSecret())) {
            log.warn("Webhook rechazado — checksum inválido transacción={}", idWompi);
            throw new SecureRequestException("Firma de webhook inválida", HttpStatus.UNAUTHORIZED);
        }

        if (!ESTADOS_TERMINALES.contains(estado)) {
            log.info("Webhook estado no terminal {} referencia={}", estado, referencia);
            return;
        }

        if (amountInCents == null || !amountInCents.equals(pago.getMontoCentavos())
                || currency == null || !currency.equalsIgnoreCase(pago.getMoneda())) {
            log.warn("Webhook monto/currency no coinciden — referencia={} esperado={} {} recibido={} {}",
                    referencia, pago.getMontoCentavos(), pago.getMoneda(), amountInCents, currency);
            return;
        }

        // Idempotencia: mismo estado terminal + mismo id Wompi
        if (ESTADOS_TERMINALES.contains(pago.getEstado())
                && pago.getEstado().equals(estado)
                && idWompi != null
                && idWompi.equals(pago.getIdTransaccionWompi())) {
            log.info("Webhook idempotente — referencia={} estado={}", referencia, estado);
            return;
        }

        if (ESTADOS_TERMINALES.contains(pago.getEstado()) && !Constantes.PAGO_ESTADO_PENDING.equals(pago.getEstado())) {
            log.info("Pago ya procesado — referencia={} estadoActual={} evento={}",
                    referencia, pago.getEstado(), estado);
            return;
        }

        String metodo = metodoPago != null ? metodoPago : pago.getMetodoPago();
        int updated = pagoRepository.actualizarEstadoSiPendiente(
                referencia, estado, idWompi, metodo, "WOMPI_WEBHOOK");

        if (updated == 0) {
            log.info("No se actualizó pago PENDING — posible carrera/idempotencia referencia={}", referencia);
            return;
        }

        log.info("Pago actualizado por webhook — referencia={} estado={}", referencia, estado);

        if (Constantes.PAGO_ESTADO_APPROVED.equals(estado)) {
            actualizarFacturaAlAprobado(referencia);
        }
    }

    /**
     * Misma lógica que la integración Wompi anterior:
     * {@code facturaService.update} sobre {@code public.factura} con estado PAGADA.
     */
    private void actualizarFacturaAlAprobado(String referencia) {
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
                    pago.getUsuarioCreacion() != null ? pago.getUsuarioCreacion() : "WOMPI_WEBHOOK");

            ResponseEntity<ResponseDTO> respuesta = facturaService.update(facturaDTO);
            ResponseDTO body = respuesta.getBody();

            if (body != null && Boolean.TRUE.equals(body.getSuccess())) {
                log.info("Factura id={} marcada como PAGADA — referencia: {}", pago.getIdFactura(), referencia);
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

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
