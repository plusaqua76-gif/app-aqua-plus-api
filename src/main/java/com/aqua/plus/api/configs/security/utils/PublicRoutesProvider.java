package com.aqua.plus.api.configs.security.utils;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import com.aqua.plus.commons.entities.ParametrosSistemaEntity;
import com.aqua.plus.commons.repositories.ParametrosSistemaRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PublicRoutesProvider {

	private final ParametrosSistemaRepository parametrosSistemaRepository;
    private final AntPathMatcher matcher = new AntPathMatcher();

    private volatile List<String> whitelist = List.of();

    @EventListener(ApplicationReadyEvent.class)
    public void load() {
        try {
            ParametrosSistemaEntity parametro =
                    parametrosSistemaRepository.findByLlave(Constantes.SECURITY_ROUTES)
                            .orElseThrow(() -> new IllegalStateException("Parametro SEGURIDAD_RUTAS no encontrado"));

            whitelist = Arrays.stream(parametro.getValorParametro().split("\\s*,\\s*"))
                    .map(r -> r.replace("\"", "").trim())
                    .filter(s -> !s.isEmpty())
                    .toList();

            log.info("[SECURITY] Whitelist cargada con {} rutas", whitelist.size());
        } catch (Exception e) {
            log.error("[SECURITY] Error cargando whitelist, usando lista vacía", e);
            whitelist = List.of();
        }
    }

    public boolean isPublic(String uri) {
        if (uri == null) return false;
        return whitelist.stream().anyMatch(p -> matcher.match(p, uri));
    }

}
