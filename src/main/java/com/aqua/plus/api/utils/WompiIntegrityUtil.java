package com.aqua.plus.api.utils;

import com.aqua.plus.commons.dtos.external.WebhookEventDTO;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class WompiIntegrityUtil {

    public String generarFirmaTransaccion(String referencia,
                                           Long montoCentavos,
                                           String moneda,
                                           String secretoIntegridad) {
        String cadena = referencia + montoCentavos + moneda + secretoIntegridad;
        return sha256(cadena);
    }

    public String generarFirmaWebhook(WebhookEventDTO evento, String secretoEventos) {
        String cadena = obtenerPropiedadesFirma(evento).stream()
                .map(propiedad -> String.valueOf(resolverValor(evento.getData(), propiedad)))
                .collect(Collectors.joining())
                + evento.getTimestamp()
                + secretoEventos;
        return sha256(cadena);
    }

    public boolean validarFirmaWebhook(WebhookEventDTO evento,
                                        String firmaRecibida,
                                        String secretoEventos) {
        if (firmaRecibida == null || firmaRecibida.isBlank()
                || secretoEventos == null || secretoEventos.isBlank()) {
            return false;
        }

        String firmaEsperada = generarFirmaWebhook(evento, secretoEventos);
        return firmaEsperada.equalsIgnoreCase(firmaRecibida);
    }

    private List<String> obtenerPropiedadesFirma(WebhookEventDTO evento) {
        if (evento.getSignature() == null) {
            throw new IllegalArgumentException("El evento Wompi no trae signature.properties");
        }

        Object properties = evento.getSignature().get("properties");
        if (!(properties instanceof List<?> lista) || lista.isEmpty()) {
            throw new IllegalArgumentException("El evento Wompi no trae signature.properties");
        }

        return lista.stream()
                .map(String::valueOf)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Object resolverValor(Map<String, Object> data, String ruta) {
        Object actual = data;
        for (String parte : ruta.split("\\.")) {
            if (!(actual instanceof Map<?, ?> mapa) || !mapa.containsKey(parte)) {
                throw new IllegalArgumentException("No se encontró la propiedad firmada por Wompi: " + ruta);
            }
            actual = ((Map<String, Object>) mapa).get(parte);
        }
        if (actual == null) {
            throw new IllegalArgumentException("La propiedad firmada por Wompi es nula: " + ruta);
        }
        return actual;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generando firma SHA-256", e);
        }
    }
}
