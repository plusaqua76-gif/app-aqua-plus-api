package com.aqua.plus.api.controller;

import com.aqua.plus.api.service.IWebhookService;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.WebhookEventDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@Tag(name = "Webhook - Controller", description = "Recepción de eventos webhook de Wompi")
@CrossOrigin(origins = "*", methods = { RequestMethod.POST })
@RequiredArgsConstructor
public class WebhookController {

    private final IWebhookService webhookService;

    @Operation(summary = "Recibir evento webhook de Wompi")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento procesado satisfactoriamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
    @PostMapping("/wompi")
    public ResponseEntity<Void> recibirEvento(
            @RequestBody WebhookEventDTO evento,
            @RequestHeader(value = "X-Event-Checksum", required = false) String firma) {

        webhookService.recibirEventoWompi(evento, firma);
        return ResponseEntity.ok().build();
    }
}
