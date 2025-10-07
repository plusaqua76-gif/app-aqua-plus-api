package com.aqua.plus.api.configs.security.filter;

import java.io.IOException;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.aqua.plus.api.configs.security.utils.JwtUtil;
import com.aqua.plus.api.configs.security.utils.PublicRoutesProvider;
import com.aqua.plus.api.service.impl.AutenticacionServiceImpl;
import com.aqua.plus.commons.utils.Constantes;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final @Lazy AutenticacionServiceImpl usuarioService;
    private final JwtUtil jwtTokenUtil;
    private final PublicRoutesProvider routesProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return routesProvider.isPublic(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        final String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String jwtToken = header.substring(7);
            String username = jwtTokenUtil.getUsernameFromToken(jwtToken, Constantes.KEY_TOKEN);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = usuarioService.loadUserByUsername(username);

                if (jwtTokenUtil.validateToken(jwtToken, userDetails, Constantes.KEY_TOKEN)) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception e) {
            log.error("[SECURITY] Error procesando JWT: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }


}
