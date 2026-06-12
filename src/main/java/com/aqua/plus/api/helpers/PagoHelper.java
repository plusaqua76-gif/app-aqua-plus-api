package com.aqua.plus.api.helpers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Helper de la capa web para pagos Wompi.
 * Contiene utilidades que dependen del contexto HTTP (HttpServletRequest).
 */
@Slf4j
@Component
public class PagoHelper {

    /**
     * Resuelve la IP real del cliente considerando proxies y balanceadores.
     */
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
}
