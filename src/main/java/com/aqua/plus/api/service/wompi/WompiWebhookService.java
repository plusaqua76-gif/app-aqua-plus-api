package com.aqua.plus.api.service.wompi;

import com.aqua.plus.api.wompi.WompiEmpresaConfig;
import com.aqua.plus.api.wompi.WompiTransaction;
import com.aqua.plus.api.wompi.WompiTransactionClient;
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
    private static final String ORIGEN = "WOMPI_WEBHOOK";

    private final PagoRepository pagoRepository;
    private final CheckoutPagoService checkoutPagoService;
    private final WompiWebhookSecurityService webhookSecurityService;
    private final WompiTransactionClient wompiTransactionClient;
    private final WompiPagoConfirmacionService confirmacionService;

    @Transactional
    public void procesar(WebhookEventDTO evento, String firmaRecibida) {
        if (evento == null || !"transaction.updated".equals(evento.getEvent())) {
            log.info("Webhook ignorado — evento: {}", evento != null ? evento.getEvent() : null);
            return;
        }

        Map<String, Object> data = evento.getData();
        if (data == null || !(data.get("transaction") instanceof Map<?, ?> rawTx)) {
            log.warn("Webhook sin data.transaction — se ignora");
            return;
        }

        Map<?, ?> txMap = rawTx;
        String idWompi = txMap.get("id") != null ? String.valueOf(txMap.get("id")) : null;
        String referencia = txMap.get("reference") != null ? String.valueOf(txMap.get("reference")) : null;

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

        if (idWompi == null || idWompi.isBlank()) {
            log.warn("Webhook sin id de transacción — se ignora");
            return;
        }

        WompiTransaction tx = wompiTransactionClient.consultar(config.publicKey(), idWompi).orElse(null);
        if (tx == null) {
            log.warn("No se pudo consultar GET /v1/transactions/{} — no se marca PAG", idWompi);
            return;
        }

        confirmacionService.confirmar(pago.getIdFactura(), tx, ORIGEN);
    }
}
