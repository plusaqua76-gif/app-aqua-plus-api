package com.aqua.plus.api.service.wompi;

import com.aqua.plus.api.wompi.WompiEmpresaConfig;
import com.aqua.plus.api.wompi.WompiWebhookSecurityService;
import com.aqua.plus.commons.dtos.external.WebhookEventDTO;
import com.aqua.plus.commons.entities.PagoEntity;
import com.aqua.plus.commons.exceptions.SecureRequestException;
import com.aqua.plus.commons.repositories.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WompiWebhookService {

    private static final String EVENTO_TRANSACCION_ACTUALIZADA = "transaction.updated";
    private static final String KEY_STATUS = "status";
    private static final String KEY_PAYMENT_METHOD_TYPE = "payment_method_type";
    private static final String ORIGEN = "WOMPI_WEBHOOK";

    private final PagoRepository pagoRepository;
    private final CheckoutPagoService checkoutPagoService;
    private final WompiWebhookSecurityService webhookSecurityService;
    private final WompiPagoConfirmacionService confirmacionService;

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

        confirmacionService.aplicar(pago, estado, idWompi, metodoPago, amountInCents, currency, ORIGEN);
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
