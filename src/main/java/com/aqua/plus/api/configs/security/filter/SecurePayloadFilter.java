package com.aqua.plus.api.configs.security.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.aqua.plus.api.utils.SecureRequestValidator;
import com.aqua.plus.commons.dtos.SecureRequestDTO;
import com.aqua.plus.commons.exceptions.SecureRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filtro que intercepta rutas protegidas (${secure.path-pattern}),
 * valida el sobre seguro y reemplaza el body de la petición con el
 * payload JSON descifrado antes de pasar al controlador.
 *
 * Flujo:
 *   POST /api/v1/secure/...  →  { payload, timestamp, nonce, signature }
 *        ↓  SecurePayloadFilter
 *   Controlador recibe   →  { ...tu DTO normal... }
 *
 * Los errores de validación se convierten en respuestas JSON sin lanzar
 * excepciones no controladas para evitar información de debug en producción.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurePayloadFilter extends OncePerRequestFilter {

    private final SecureRequestValidator validator;
    private final ObjectMapper           objectMapper;

    /**
     * Patrón Ant de rutas protegidas por este filtro.
     * Ejemplo: /api/v1/secure/**
     */
    @Value("${secure.path-pattern:/api/v1/secure/**}")
    private String pathPattern;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean matchesPath = pathMatcher.match(pathPattern, request.getRequestURI());
        boolean hasSecuredHeader = "true".equalsIgnoreCase(request.getHeader("X-Secured"));
        return !matchesPath && !hasSecuredHeader;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        try {
            //  Leer el sobre cifrado
            SecureRequestDTO envelope = objectMapper.readValue(
                request.getInputStream(), SecureRequestDTO.class);

            //  Validar y descifrar (lanza SecureRequestException si algo falla)
            String decryptedJson = validator.validate(envelope);

            //  Reemplazar el body con el JSON plano descifrado
            CachedBodyRequestWrapper wrappedRequest =
                new CachedBodyRequestWrapper(request, decryptedJson);

            chain.doFilter(wrappedRequest, response);

        } catch (SecureRequestException ex) {
            log.warn("[SecurePayloadFilter] Petición rechazada: {}", ex.getMessage());
            writeErrorResponse(response, ex.getStatus(), ex.getMessage());
        } catch (Exception ex) {
            log.error("[SecurePayloadFilter] Error inesperado procesando sobre seguro", ex);
            writeErrorResponse(response, HttpStatus.BAD_REQUEST,
                "El sobre de la petición no tiene el formato esperado");
        }
    }

    private void writeErrorResponse(HttpServletResponse response,
                                    HttpStatus status,
                                    String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String body = objectMapper.writeValueAsString(
            java.util.Map.of(
                "success", false,
                "code",    status.value(),
                "message", message
            )
        );
        response.getWriter().write(body);
    }
}
