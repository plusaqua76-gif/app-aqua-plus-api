package com.aqua.plus.api.wompi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Consulta una transacción Wompi por id (endpoint público, no requiere clave privada).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WompiTransactionClient {

    static final String API_SANDBOX = "https://sandbox.wompi.co/v1";
    static final String API_PRODUCCION = "https://production.wompi.co/v1";
    private static final Pattern ID_TRANSACCION = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final RestTemplate restTemplate;

    public Optional<WompiTransaction> consultar(String publicKey, String transactionId) {
        if (!esIdValido(transactionId)) {
            log.warn("Id de transacción Wompi inválido: {}", transactionId);
            return Optional.empty();
        }

        String url = resolverApiBase(publicKey) + "/transactions/" + transactionId;
        try {
            ResponseEntity<Map> respuesta = restTemplate.getForEntity(url, Map.class);
            Map<?, ?> body = respuesta.getBody();
            if (body == null || !(body.get("data") instanceof Map<?, ?> data)) {
                log.warn("Respuesta Wompi sin data — id={}", transactionId);
                return Optional.empty();
            }
            return Optional.of(mapear(data));
        } catch (Exception e) {
            log.warn("No se pudo consultar transacción Wompi id={}: {}", transactionId, e.getMessage());
            return Optional.empty();
        }
    }

    static String resolverApiBase(String publicKey) {
        if (publicKey != null && publicKey.startsWith("pub_prod_")) {
            return API_PRODUCCION;
        }
        return API_SANDBOX;
    }

    static boolean esIdValido(String transactionId) {
        return transactionId != null && ID_TRANSACCION.matcher(transactionId.trim()).matches();
    }

    private WompiTransaction mapear(Map<?, ?> data) {
        return new WompiTransaction(
                asString(data.get("id")),
                asString(data.get("status")),
                asString(data.get("reference")),
                asLong(data.get("amount_in_cents")),
                asString(data.get("currency")),
                asString(data.get("payment_method_type"))
        );
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
