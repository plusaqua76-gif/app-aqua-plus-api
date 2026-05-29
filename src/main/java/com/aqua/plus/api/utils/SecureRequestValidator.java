package com.aqua.plus.api.utils;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.aqua.plus.commons.dtos.SecureRequestDTO;
import com.aqua.plus.commons.exceptions.SecureRequestException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class SecureRequestValidator {

    static final long MAX_DIFF_SECONDS = 300L;

    private final HmacUtil    hmacUtil;
    private final AesGcmUtil  aesGcmUtil;
    private final NonceStore  nonceStore;

    public String validate(SecureRequestDTO request) {

        long now  = Instant.now().getEpochSecond();
        long diff = Math.abs(now - request.getTimestamp());
        if (diff > MAX_DIFF_SECONDS) {
            log.warn("[SecureRequest] Timestamp inválido — diff={}s, ip implícita en payload", diff);
            throw new SecureRequestException(
                "Petición expirada o timestamp inválido", HttpStatus.UNAUTHORIZED);
        }

        if (!nonceStore.registerIfNew(request.getNonce())) {
            log.warn("[SecureRequest] Nonce repetido detectado: {}", request.getNonce());
            throw new SecureRequestException(
                "Replay attack detectado: nonce ya utilizado", HttpStatus.UNAUTHORIZED);
        }

        if (!hmacUtil.verify(request.getSignature(),
                             request.getNonce(),
                             request.getTimestamp(),
                             request.getPayload())) {
            log.warn("[SecureRequest] Firma HMAC inválida — nonce={}", request.getNonce());
            throw new SecureRequestException(
                "Firma de la petición inválida", HttpStatus.FORBIDDEN);
        }

        try {
            return aesGcmUtil.decrypt(request.getPayload());
        } catch (Exception e) {
            log.error("[SecureRequest] Error descifrando payload: {}", e.getMessage());
            throw new SecureRequestException(
                "No se pudo descifrar el payload", HttpStatus.BAD_REQUEST);
        }
    }
}
