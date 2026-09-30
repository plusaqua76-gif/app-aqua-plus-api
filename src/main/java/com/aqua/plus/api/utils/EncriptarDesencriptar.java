package com.aqua.plus.api.utils;

import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EncriptarDesencriptar {
    
    @Value("${seguridad.llave}")
    private String llave;
    
    public String encriptar(String texto) {
        if (texto == null || texto.trim().isEmpty()) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digestOfPassword = md.digest(this.llave.getBytes("utf-8"));
            byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);

            SecretKey key = new SecretKeySpec(keyBytes, "DESede");
            Cipher cipher = Cipher.getInstance("DESede");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] plainTextBytes = texto.getBytes("utf-8");
            byte[] buf = cipher.doFinal(plainTextBytes);
            return new String(Base64.encodeBase64(buf));

        } catch (Exception e) {
            log.error("Error al encriptar: {}", e.getMessage(), e);
            throw new RuntimeException("Error encriptando clave", e);
        }
    }

    public String desencriptar(String textoEncriptado) {
        if (textoEncriptado == null || textoEncriptado.trim().isEmpty()) return "";
        try {
            byte[] message = Base64.decodeBase64(textoEncriptado.getBytes("utf-8"));
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            // Validar que la llave no llegue nula desde Spring
            if (this.llave == null) {
                throw new IllegalStateException("La propiedad 'seguridad.llave' no ha sido cargada por Spring.");
            }

            byte[] digestOfPassword = md.digest(this.llave.getBytes("utf-8"));
            byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);
            SecretKey key = new SecretKeySpec(keyBytes, "DESede");

            Cipher decipher = Cipher.getInstance("DESede");
            decipher.init(Cipher.DECRYPT_MODE, key);

            byte[] plainText = decipher.doFinal(message);

            return new String(plainText, "UTF-8");

        } catch (Exception e) {
            log.error("Error al desencriptar clave: {}", e.getMessage(), e);
            throw new RuntimeException("Error desencriptando clave de correo", e);
        }
    }
}