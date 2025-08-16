package com.acua.plus.api.configs.security.handler;

import java.io.IOException;
import java.io.Serializable;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.utils.Constantes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * @author dchavarro.ext
 * @version 1.0
 * Clase encargada de la autenticacion personalizada
 */
@Component
@Slf4j
public class AuthenticationEntryPointCustom implements AuthenticationEntryPoint, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Metodo encargado de generar la excepcion personaliza de la autenticación
	 * @author dchavarro.ext
	 * @since 21-08-2024
	 * @version 1.0
	 * @param HttpServletRequest
	 * @param HttpServletResponse
	 * @param AuthenticationException
	 * @return Devuelve respuesta exitosa si el proceso se completa correctamente de lo contrario error
	 */
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		
		ObjectMapper mapper = new ObjectMapper();
		log.info("Inicio metodo commence:{} ",  authException.getMessage());
		
		mapper.registerModule(new JavaTimeModule());
	    String data = mapper.writeValueAsString(ResponseDTO.builder().code(HttpStatus.UNAUTHORIZED.value()).success(Boolean.FALSE).message(Constantes.TOKEN_NO_EXIST_NOT_GENERATED_SYSTEM).build());
	    
	    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
	    response.getWriter().write(data);
	}
}