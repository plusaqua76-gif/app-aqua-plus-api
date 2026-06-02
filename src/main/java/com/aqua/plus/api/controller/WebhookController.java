package com.aqua.plus.api.controller;

import com.aqua.plus.api.service.IWebhookService;
import com.aqua.plus.commons.dtos.external.WebhookEventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final IWebhookService webhookService;

    @PostMapping("/wompi")
    public ResponseEntity<Void> recibirEvento(
            @RequestBody WebhookEventDTO evento,
            @RequestHeader(value = "X-Event-Checksum", required = false) String firma) {

        webhookService.recibirEventoWompi(evento, firma);
        return ResponseEntity.ok().build();
    }
}
