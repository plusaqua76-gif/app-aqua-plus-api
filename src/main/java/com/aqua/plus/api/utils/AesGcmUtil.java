package com.aqua.plus.api.utils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class AesGcmUtil {

    private static final String ALGORITHM    = "AES/GCM/NoPadding";
    private static final int    IV_LENGTH    = 12;   
    private static final int    TAG_BITS     = 128;  

    @Value("${secure.aes-key}")
    private String aesKeyBase64;

    public String decrypt(String base64Ciphertext) {
        try {
            byte[] combined   = Base64.getDecoder().decode(base64Ciphertext);
            byte[] iv         = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

            SecretKey key = buildKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));

            byte[] plain = cipher.doFinal(ciphertext);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error descifrando payload AES-GCM: " + e.getMessage(), e);
        }
    }

    private SecretKey buildKey() {
        byte[] keyBytes = Base64.getDecoder().decode(aesKeyBase64);
        if (keyBytes.length != 32) {
            throw new IllegalStateException("secure.aes-key debe ser exactamente 32 bytes en base64");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}
