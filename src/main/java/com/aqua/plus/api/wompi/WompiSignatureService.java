package com.aqua.plus.api.wompi;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class WompiSignatureService {

    /**
     * SHA-256(reference + amountInCents + currency + integritySecret)
     */
    public String generarFirmaIntegridad(String reference,
                                         Long amountInCents,
                                         String currency,
                                         String integritySecret) {
        if (reference == null || amountInCents == null || currency == null || integritySecret == null) {
            throw new IllegalArgumentException("Parámetros incompletos para firma de integridad Wompi");
        }
        return sha256(reference + amountInCents + currency + integritySecret);
    }

    public String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
