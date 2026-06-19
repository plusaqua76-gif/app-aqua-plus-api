package com.aqua.plus.api.service;

import com.aqua.plus.commons.dtos.external.WebhookEventDTO;

public interface IWebhookService {

    void recibirEventoWompi(WebhookEventDTO evento, String firma);
}
