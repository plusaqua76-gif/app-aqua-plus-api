package com.aqua.plus.api.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
@RequiredArgsConstructor
public class WompiIntegrityUtil {

    private final UtilsWompi utilsWompi;

    public String generarFirmaTransaccion(String referencia,
                                           Long montoCentavos,
                                           String moneda) {
        String cadena = referencia + montoCentavos + moneda
                        + utilsWompi.getSecretoIntegridad();
        return sha256(cadena);
    }

    public String generarFirmaTransaccion(String referencia,
                                           Long montoCentavos,
                                           String moneda,
                                           String secretoIntegridad) {
        String cadena = referencia + montoCentavos + moneda + secretoIntegridad;
        return sha256(cadena);
    }

    public String generarFirmaWebhook(String idTransaccion,
                                       String estado,
                                       Long montoCentavos,
                                       Long timestamp) {
        String cadena = idTransaccion + estado + montoCentavos
                        + utilsWompi.getSecretoEventos() + timestamp;
        return sha256(cadena);
    }

    public boolean validarFirmaWebhook(String idTransaccion,
                                        String estado,
                                        Long montoCentavos,
                                        Long timestamp,
                                        String firmaRecibida) {
        String firmaEsperada = generarFirmaWebhook(idTransaccion, estado, montoCentavos, timestamp);
        return firmaEsperada.equals(firmaRecibida);
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