package com.aqua.plus.api.utils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


@Slf4j
@Component
public class JwePseService {

    @Value("${wompi.public-key}")
    private String publicKeyPem;

    private PublicKey publicKey;

    private static final String PLACEHOLDER = "REPLACE_WITH_WOMPI_RSA_PUBLIC_KEY_PEM";

    @PostConstruct
    void init() {
        if (publicKeyPem == null || publicKeyPem.isBlank() || publicKeyPem.equals(PLACEHOLDER)) {
            log.warn("[JwePseService] wompi.public-key no configurada — " +
                     "los campos de referencia PSE se enviarán en PLANO (solo desarrollo).");
            return;
        }
        try {
            publicKey = parsePem(publicKeyPem);
            log.info("[JwePseService] Llave pública de Wompi cargada correctamente.");
        } catch (Exception e) {
            log.error("[JwePseService] Error cargando llave pública de Wompi: {}", e.getMessage());
        }
    }

    public String cifrar(String valor) {
        if (publicKey == null) {
            return valor;
        }
        try {
            JsonWebEncryption jwe = new JsonWebEncryption();
            jwe.setPayload(valor);
            jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.RSA_OAEP);
            jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_256_GCM);
            jwe.setKey(publicKey);
            return jwe.getCompactSerialization();
        } catch (Exception e) {
            log.error("[JwePseService] Error cifrando valor para PSE: {}", e.getMessage());
            return valor;
        }
    }
    public boolean isReady() {
        return publicKey != null;
    }

    private PublicKey parsePem(String pem) throws Exception {
        String stripped = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(stripped);
        KeyFactory kf  = KeyFactory.getInstance("RSA");
        return kf.generatePublic(new X509EncodedKeySpec(decoded));
    }
}
