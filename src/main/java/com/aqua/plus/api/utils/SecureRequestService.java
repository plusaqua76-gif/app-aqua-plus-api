package com.aqua.plus.api.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.aqua.plus.commons.dtos.SecureRequestDTO;
import com.aqua.plus.commons.exceptions.SecureRequestException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio que centraliza la lógica de seguridad para peticiones cifradas:
 * firma HMAC-SHA256, verificación y validación completa del sobre seguro.
 *
 * El descifrado del payload se delega a {@link EncriptarDesencriptar} (DESede).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecureRequestService {

    static final long   MAX_DIFF_SECONDS = 300L;
    private static final String HMAC_ALGORITHM   = "HmacSHA256";
    private static final String HMAC_SECRET      = "keyacuaplus-hmac-secure-2025!!";

    private final EncriptarDesencriptar encriptarDesencriptar;
    private final NonceStore            nonceStore;

    /**
     * Genera la firma HMAC-SHA256 con el formato: {@code nonce.timestamp.payload}
     */
    public String firmar(String nonce, Long timestamp, String payload) {
        String data = nonce + "." + timestamp + "." + payload;
        return computeHmac(data);
    }

    /**
     * Verifica que la firma recibida coincida con la calculada localmente.
     * Usa comparación de tiempo constante para evitar timing attacks.
     */
    public boolean verificarFirma(String received, String nonce, Long timestamp, String payload) {
        String expected = firmar(nonce, timestamp, payload);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                received.getBytes(StandardCharsets.UTF_8));
    }

    private String computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(HMAC_SECRET.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error generando firma HMAC-SHA256", e);
        }
    }

    /**
     * Orquesta la validación completa:
     * <ol>
     *   <li>Timestamp dentro del rango permitido (máx {@value #MAX_DIFF_SECONDS}s).</li>
     *   <li>Nonce no utilizado previamente (anti-replay).</li>
     *   <li>Firma HMAC-SHA256 válida.</li>
     *   <li>Payload descifrado con DESede.</li>
     * </ol>
     *
     * @param request sobre seguro recibido del frontend
     * @return JSON plano descifrado, listo para deserializar en el controlador
     * @throws SecureRequestException si cualquiera de las validaciones falla
     */
    public String validarPeticion(SecureRequestDTO request) {

        long now  = Instant.now().getEpochSecond();
        long diff = Math.abs(now - request.getTimestamp());
        if (diff > MAX_DIFF_SECONDS) {
            log.warn("[SecureRequest] Timestamp inválido — diff={}s", diff);
            throw new SecureRequestException(
                    "Petición expirada o timestamp inválido", HttpStatus.UNAUTHORIZED);
        }

        if (!nonceStore.registerIfNew(request.getNonce())) {
            log.warn("[SecureRequest] Nonce repetido detectado: {}", request.getNonce());
            throw new SecureRequestException(
                    "Replay attack detectado: nonce ya utilizado", HttpStatus.UNAUTHORIZED);
        }

        if (!verificarFirma(request.getSignature(),
                request.getNonce(),
                request.getTimestamp(),
                request.getPayload())) {
            log.warn("[SecureRequest] Firma HMAC inválida — nonce={}", request.getNonce());
            throw new SecureRequestException(
                    "Firma de la petición inválida", HttpStatus.FORBIDDEN);
        }

        String decrypted = encriptarDesencriptar.desencriptar(request.getPayload());
        if (decrypted == null || decrypted.isEmpty()) {
            log.error("[SecureRequest] Error descifrando payload — resultado vacío o inválido");
            throw new SecureRequestException(
                    "No se pudo descifrar el payload", HttpStatus.BAD_REQUEST);
        }
        return decrypted;
    }
}
