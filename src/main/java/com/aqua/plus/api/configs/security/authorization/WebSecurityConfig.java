package com.aqua.plus.api.configs.security.authorization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.aqua.plus.api.configs.security.filter.JwtAuthenticationFilter;
import com.aqua.plus.api.configs.security.handler.AuthenticationEntryPointCustom;
import com.aqua.plus.commons.entities.ParametrosSistemaEntity;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import com.aqua.plus.commons.repositories.ParametrosSistemaRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author dchavarro
 * @version 1.0
 * Clase encargada de la administración de la seguridad
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class WebSecurityConfig {

	private final AuthenticationEntryPointCustom authenticationEntryPoint;
	private final JwtAuthenticationFilter jwtRequestFilter;
	private final ParametrosSistemaRepository parametrosSistemaRepository;
	
	@Value("${app.cors}")
	private String aquaPlusCors;
	
	public ParametrosSistemaEntity getParameter(final String key) {
	    return parametrosSistemaRepository.findByLlave(key)
	        .orElseThrow(() -> new ProcessGenericException(Constantes.PARAM_NOT_FOUND));
	}
	
	/**
	 * Carga las rutas públicas permitidas desde base de datos (whitelist) usando el parámetro {@code SECURITY_ROUTES}.
	 * Elimina comillas y espacios innecesarios.
	 *
	 * @author nicope
	 * @version 1.0
	 * @return String[] con las rutas públicas; vacío si ocurre error.
	 */
	String[] loadWhitelistFromDB() {
	    try {
	        ParametrosSistemaEntity parametro = getParameter(Constantes.SECURITY_ROUTES);
	        String rutas = parametro.getValorParametro();

	        return Arrays.stream(rutas.split("\\s*,\\s*"))
	                .map(ruta -> ruta.replace("\"", "").trim())
	                .toArray(String[]::new);
	    } catch (Exception e) {
	        log.error("Error al cargar rutas públicas desde BD", e);
	        return new String[0];
	    }
	}

    /**
     * Metodo encargado de configurar la seguridad requerida para el microservicio
     * @author dchavarro.ext
     * @since 21-08-2024
     * @modify  16-12-2024-dchavarro.ext
     * @version 2.0
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
		return httpSecurity
	            .csrf(AbstractHttpConfigurer::disable)
	            .authorizeHttpRequests( auth -> auth
	                    .requestMatchers(getOperationAllow()).permitAll()
	                    .requestMatchers(loadWhitelistFromDB()).permitAll()
	                    .anyRequest().authenticated()
	            )
	            .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
	            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
	            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
	            .build();
	}
	
	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}
	
	private String[] getOperationAllow() {
		String[] operations = new String[1];
		
		operations[0] ="validar usuario";
		return operations;
	}
	
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
	    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	    CorsConfiguration config = new CorsConfiguration();
	    if (!aquaPlusCors.isEmpty()) {
	    	List<String> cors = new ArrayList<String>();
	    	cors.add(aquaPlusCors);
	        config.setAllowedOrigins(cors);
	    }
	    config.setAllowCredentials(true);
	    config.addAllowedHeader("*");
	    config.addAllowedMethod("*");
	    config.addExposedHeader("Content-Disposition");
	    source.registerCorsConfiguration("/**", config);
	    return source;
	}
	
}