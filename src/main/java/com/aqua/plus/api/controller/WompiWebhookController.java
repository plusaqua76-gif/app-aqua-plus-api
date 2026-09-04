package com.aqua.plus.api.controller;

import com.aqua.plus.api.service.IWebhookService;
import com.aqua.plus.commons.dtos.external.WebhookEventDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wompi")
@Tag(name = "Wompi Webhook - Controller", description = "Webhook Wompi (ruta versionada)")
@CrossOrigin(origins = "*", methods = { RequestMethod.POST })
@RequiredArgsConstructor
public class WompiWebhookController {

    private final IWebhookService webhookService;

    @Operation(summary = "Recibir evento webhook de Wompi")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento procesado"),
            @ApiResponse(responseCode = "401", description = "Checksum inválido") })
    @PostMapping("/webhook")
    public ResponseEntity<Void> recibirEvento(
            @RequestBody WebhookEventDTO evento,
            @RequestHeader(value = "X-Event-Checksum", required = false) String firma) {
        webhookService.recibirEventoWompi(evento, firma);
        return ResponseEntity.ok().build();
    }
}
