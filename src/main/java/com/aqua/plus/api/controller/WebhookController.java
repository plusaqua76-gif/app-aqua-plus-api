package com.aqua.plus.api.controller;

import com.aqua.plus.api.service.impl.external.PagoServiceImpl;
import com.aqua.plus.commons.dtos.external.WebhookEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final PagoServiceImpl pagoService;

    /**
     * Wompi llama este endpoint automáticamente cuando cambia el estado de una transacción.
     *
     * IMPORTANTE:
     * - NO debe tener autenticación JWT
     * - La seguridad es la firma SHA-256 en X-Event-Checksum
     * - Siempre retornar 200 aunque sea un evento que ignores (evita reintentos de Wompi)
     */
    @PostMapping("/wompi")
    public ResponseEntity<Void> recibirEvento(
            @RequestBody WebhookEventDTO evento,
            @RequestHeader(value = "X-Event-Checksum", required = false) String firma) {

        log.info("Webhook recibido - evento: {}", evento.getEvent());

        if (!"transaction.updated".equals(evento.getEvent())) {
            log.info("Evento ignorado: {}", evento.getEvent());
            return ResponseEntity.ok().build();
        }

        // Fallback: si el header no llegó, usar el checksum del body
        String firmaEfectiva = firma;
        if (firmaEfectiva == null && evento.getSignature() != null) {
            firmaEfectiva = (String) evento.getSignature().get("checksum");
        }

        try {
            pagoService.procesarWebhook(evento, firmaEfectiva);
        } catch (Exception e) {
            // Siempre retornar 200 a Wompi para evitar reintentos innecesarios.
            // Los errores de firma o procesamiento ya son logueados en el service.
            log.error("Error procesando webhook de Wompi: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}