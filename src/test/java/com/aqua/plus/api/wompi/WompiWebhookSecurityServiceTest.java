package com.aqua.plus.api.wompi;

import com.aqua.plus.commons.dtos.external.WebhookEventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WompiWebhookSecurityServiceTest {

    private WompiWebhookSecurityService securityService;
    private WompiSignatureService signatureService;

    @BeforeEach
    void setUp() {
        signatureService = new WompiSignatureService();
        securityService = new WompiWebhookSecurityService(signatureService);
    }

    @Test
    void aceptaChecksumValido() {
        WebhookEventDTO evento = eventoPrueba();
        String secret = "event-secret";
        String checksum = securityService.generarChecksum(evento, secret);

        assertTrue(securityService.validarChecksum(evento, checksum, secret));
    }

    @Test
    void rechazaChecksumInvalido() {
        WebhookEventDTO evento = eventoPrueba();
        assertFalse(securityService.validarChecksum(evento, "checksum-falso", "event-secret"));
    }

    @Test
    void rechazaSinFirma() {
        assertFalse(securityService.validarChecksum(eventoPrueba(), null, "event-secret"));
    }

    private WebhookEventDTO eventoPrueba() {
        Map<String, Object> transaction = new LinkedHashMap<>();
        transaction.put("id", "tx_123");
        transaction.put("status", "APPROVED");
        transaction.put("amount_in_cents", 21500000);
        transaction.put("currency", "COP");
        transaction.put("reference", "FAC-84521-20260826-A8F31");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transaction", transaction);

        Map<String, Object> signature = new LinkedHashMap<>();
        signature.put("properties", List.of(
                "transaction.id",
                "transaction.status",
                "transaction.amount_in_cents"));
        signature.put("checksum", "placeholder");

        WebhookEventDTO evento = new WebhookEventDTO();
        evento.setEvent("transaction.updated");
        evento.setData(data);
        evento.setSignature(signature);
        evento.setTimestamp(1690000000L);
        return evento;
    }
}
