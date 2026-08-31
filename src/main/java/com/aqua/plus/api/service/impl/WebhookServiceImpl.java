package com.aqua.plus.api.service.impl;

import com.aqua.plus.api.service.IWebhookService;
import com.aqua.plus.api.service.wompi.WompiWebhookService;
import com.aqua.plus.commons.dtos.external.WebhookEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements IWebhookService {

    private static final String CAMPO_CHECKSUM = "checksum";

    private final WompiWebhookService wompiWebhookService;

    @Override
    public void recibirEventoWompi(WebhookEventDTO evento, String firma) {
        log.info("Webhook recibido - evento: {}", evento != null ? evento.getEvent() : null);
        wompiWebhookService.procesar(evento, obtenerFirmaEfectiva(evento, firma));
    }

    private String obtenerFirmaEfectiva(WebhookEventDTO evento, String firma) {
        if (firma != null && !firma.isBlank()) {
            return firma;
        }
        if (evento == null || evento.getSignature() == null) {
            return null;
        }
        Object checksum = evento.getSignature().get(CAMPO_CHECKSUM);
        return checksum instanceof String s ? s : null;
    }
}
