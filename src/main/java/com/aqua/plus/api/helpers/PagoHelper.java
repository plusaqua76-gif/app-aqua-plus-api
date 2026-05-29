package com.aqua.plus.api.helpers;

import com.aqua.plus.api.service.external.IWompiService;
import com.aqua.plus.commons.repositories.PagoRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Helper de pagos Wompi.
 * Contiene lógica auxiliar que no pertenece al servicio principal:
 * - Obtención asíncrona de la redirect_url de PSE/Bancolombia (evita bloquear el hilo HTTP).
 * - Resolución segura de la IP del cliente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PagoHelper {

    @Value("${wompi.redirect-vigencia-minutos:15}")
    private int redirectVigenciaMinutos;

    private final IWompiService   wompiService;
    private final PagoRepository  pagoRepo;


    /**
     * Consulta Wompi en segundo plano hasta obtener la async_payment_url (PSE/Bancolombia).
     * Se lanza con @Async → el hilo HTTP del caller retorna de inmediato.
     * Máximo 5 intentos × 3 s = hasta 15 s en background.
     *
     * @param idWompi   identificador de la transacción en Wompi
     * @param referencia referencia interna del pago (AQP-XXXXXXXXXXXXXXXX)
     * @param clavePrivada clave privada Wompi de la empresa dueÃ±a del pago
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
                    LocalDateTime expiraEn = LocalDateTime.now().plusMinutes(redirectVigenciaMinutos);
                    pagoRepo.actualizarRedirectUrl(referencia, url, expiraEn);
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


    public String resolverIpCliente(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String candidato = xff.split(",")[0].trim();
            if (candidato.matches("[\\d.:\\[\\]a-fA-F]+")) {
                return candidato;
            }
        }
        return request.getRemoteAddr();
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
