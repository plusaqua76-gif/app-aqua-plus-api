package com.aqua.plus.api.service.impl.external;

import com.aqua.plus.api.service.external.IWompiService;
import com.aqua.plus.api.utils.EncriptarDesencriptar;
import com.aqua.plus.commons.repositories.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Tareas asíncronas del módulo de pagos Wompi.
 * Métodos marcados con @Async se ejecutan en el executor "pagoExecutor"
 * sin bloquear el hilo HTTP del caller.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoAsyncService {

    private static final int REDIRECT_VIGENCIA_MINUTOS = 5;

    private final IWompiService         wompiService;
    private final PagoRepository        pagoRepo;
    private final EncriptarDesencriptar encriptarDesencriptar;

    /**
     * Consulta Wompi en segundo plano hasta obtener la async_payment_url (PSE/Bancolombia).
     * La URL se cifra con EncriptarDesencriptar antes de persistirse en BD.
     * Máximo 5 intentos × 3 s = hasta 15 s en background.
     */
    @Async("pagoExecutor")
    @Transactional
    public void obtenerYGuardarRedirectUrl(String idWompi, String referencia, String clavePrivada) {
        int maxIntentos = 5;
        int intervaloMs = 3_000;

        for (int intento = 1; intento <= maxIntentos; intento++) {
            try {
                Map<String, Object> tx = wompiService.consultarTransaccion(idWompi, clavePrivada);
                String url = extraerRedirectUrl(tx);

                if (url != null && !url.isBlank()) {
                    LocalDateTime expiraEn = LocalDateTime.now().plusMinutes(REDIRECT_VIGENCIA_MINUTOS);
                    pagoRepo.actualizarRedirectUrl(referencia, encriptarDesencriptar.encriptar(url), expiraEn);
                    log.info("redirect_url guardada (intento {}/{}) — referencia: {}",
                        intento, maxIntentos, referencia);
                    return;
                }

                if (intento < maxIntentos) {
                    Thread.sleep(intervaloMs);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Tarea async redirect_url interrumpida — referencia: {}", referencia);
                return;
            } catch (Exception e) {
                log.warn("Error en intento {}/{} buscando redirect_url — referencia: {}: {}",
                    intento, maxIntentos, referencia, e.getMessage());
            }
        }

        log.warn("redirect_url no disponible tras {} intentos — referencia: {} (el scheduler reintentará)",
            maxIntentos, referencia);
    }

    @SuppressWarnings("unchecked")
    private String extraerRedirectUrl(Map<String, Object> txData) {
        Map<String, Object> pm = (Map<String, Object>) txData.get("payment_method");
        if (pm == null) return null;
        Map<String, Object> extra = (Map<String, Object>) pm.get("extra");
        if (extra == null) return null;
        return (String) extra.get("async_payment_url");
    }
}
