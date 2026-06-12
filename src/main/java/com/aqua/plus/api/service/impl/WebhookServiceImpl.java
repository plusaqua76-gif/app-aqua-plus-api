package com.aqua.plus.api.service.impl;

import com.aqua.plus.api.service.IWebhookService;
import com.aqua.plus.api.service.external.IPagoService;
import com.aqua.plus.commons.dtos.external.WebhookEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements IWebhookService {

    private static final String EVENTO_TRANSACCION_ACTUALIZADA = "transaction.updated";
    private static final String CAMPO_CHECKSUM = "checksum";

    private final IPagoService pagoService;

    @Override
    public void recibirEventoWompi(WebhookEventDTO evento, String firma) {
        log.info("Webhook recibido - evento: {}", evento.getEvent());

        if (!EVENTO_TRANSACCION_ACTUALIZADA.equals(evento.getEvent())) {
            log.info("Evento ignorado: {}", evento.getEvent());
            return;
        }

        pagoService.procesarWebhook(evento, obtenerFirmaEfectiva(evento, firma));
    }

    private String obtenerFirmaEfectiva(WebhookEventDTO evento, String firma) {
        if (firma != null) {
            return firma;
        }

        if (evento.getSignature() == null) {
            return null;
        }

        Object checksum = evento.getSignature().get(CAMPO_CHECKSUM);
        return checksum instanceof String ? (String) checksum : null;
    }
}
