package com.aqua.plus.api.configs.security.authorization;

import java.util.Arrays;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
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

import com.aqua.plus.api.config.AquaPlusServerProperties;
import com.aqua.plus.api.configs.security.filter.JwtAuthenticationFilter;
import com.aqua.plus.api.configs.security.filter.SecurePayloadFilter;
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
	private final SecurePayloadFilter securePayloadFilter;
	private final ParametrosSistemaRepository parametrosSistemaRepository;
	private final AquaPlusServerProperties properties;
	
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
	            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
	            .authorizeHttpRequests( auth -> auth
	                    .requestMatchers(getOperationAllow()).permitAll()
	                    .requestMatchers(loadWhitelistFromDB()).permitAll()
	                    .anyRequest().authenticated()
	            )
	            .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
	            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
	            .addFilterBefore(securePayloadFilter, UsernamePasswordAuthenticationFilter.class)
	            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
	            .build();
	}
	
	/**
	 * Desactiva el registro automático de JwtAuthenticationFilter como filtro de servlet.
	 * Solo debe correr dentro de la cadena de Spring Security (addFilterBefore).
	 */
	@Bean
	FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration() {
		FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(jwtRequestFilter);
		registration.setEnabled(false);
		return registration;
	}

	/**
	 * Desactiva el registro automático de SecurePayloadFilter como filtro de servlet.
	 * Solo debe correr dentro de la cadena de Spring Security (addFilterBefore).
	 */
	@Bean
	FilterRegistrationBean<SecurePayloadFilter> securePayloadFilterRegistration() {
		FilterRegistrationBean<SecurePayloadFilter> registration = new FilterRegistrationBean<>(securePayloadFilter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}
	
	private String[] getOperationAllow() {
		return new String[] {
			"/webhook/wompi"
		};
	}
	
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
	    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	    CorsConfiguration config = new CorsConfiguration();
	    
	    config.setAllowedOrigins(properties.getCors());
	    config.setAllowCredentials(true);
	    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
	    config.setAllowedHeaders(Arrays.asList("*"));
	    config.setExposedHeaders(Arrays.asList("Content-Disposition", "Authorization"));
	    config.setMaxAge(3600L);
	    
	    source.registerCorsConfiguration("/**", config);
	    return source;
	}
	
}