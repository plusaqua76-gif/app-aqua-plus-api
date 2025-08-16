package com.acua.plus.api.configs.security.authorization;

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

import com.acua.plus.api.configs.security.filter.JwtAuthenticationFilter;
import com.acua.plus.api.configs.security.handler.AuthenticationEntryPointCustom;

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
	
	private static final String[] WHITELIST = {
	        "/api/v1/Usuario/Autentication",
	        "/api/v1/Usuario/**",
	        "/v3/api-docs",
	        "/v3/api-docs/**",
	        "/swagger-ui/**",
	        "/swagger-ui.html"
	 };

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
	                    .requestMatchers(WHITELIST).permitAll()
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
	
}