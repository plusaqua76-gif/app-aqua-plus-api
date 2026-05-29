package com.aqua.plus.api.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HmacUtil {

    private static final String ALGORITHM = "HmacSHA256";

    @Value("${secure.hmac-secret}")
    private String hmacSecret;

    public String sign(String nonce, Long timestamp, String payload) {
        String data = nonce + "." + timestamp + "." + payload;
        return compute(data, hmacSecret);
    }

    public boolean verify(String received, String nonce, Long timestamp, String payload) {
        String expected = sign(nonce, timestamp, payload);
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            received.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String compute(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error generando firma HMAC-SHA256", e);
        }
    }
}
