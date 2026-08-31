package com.aqua.plus.api.wompi;

import com.aqua.plus.commons.dtos.external.WebhookEventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WompiWebhookSecurityService {

    private final WompiSignatureService signatureService;

    /**
     * Valida el checksum del evento Wompi:
     * SHA-256(concat(signature.properties values) + timestamp + eventSecret)
     */
    public boolean validarChecksum(WebhookEventDTO evento, String firmaRecibida, String eventSecret) {
        if (firmaRecibida == null || firmaRecibida.isBlank()
                || eventSecret == null || eventSecret.isBlank()
                || evento == null || evento.getTimestamp() == null) {
            return false;
        }
        try {
            String esperada = generarChecksum(evento, eventSecret);
            return esperada.equalsIgnoreCase(firmaRecibida);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public String generarChecksum(WebhookEventDTO evento, String eventSecret) {
        String cadena = obtenerPropiedadesFirma(evento).stream()
                .map(propiedad -> String.valueOf(resolverValor(evento.getData(), propiedad)))
                .collect(Collectors.joining())
                + evento.getTimestamp()
                + eventSecret;
        return signatureService.sha256(cadena);
    }

    private List<String> obtenerPropiedadesFirma(WebhookEventDTO evento) {
        if (evento.getSignature() == null) {
            throw new IllegalArgumentException("El evento Wompi no trae signature");
        }
        Object properties = evento.getSignature().get("properties");
        if (!(properties instanceof List<?> lista) || lista.isEmpty()) {
            throw new IllegalArgumentException("El evento Wompi no trae signature.properties");
        }
        return lista.stream().map(String::valueOf).toList();
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
}
