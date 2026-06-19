package com.aqua.plus.api.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException.BadRequest;

import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import com.aqua.plus.commons.exceptions.SecureRequestException;
import com.aqua.plus.commons.utils.Constantes;

import lombok.extern.slf4j.Slf4j;

/**
 * @author nicope
 * @version 1.0
 * 
 */

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(BadRequest.class)
    public ResponseEntity<ResponseDTO> badRequest(BadRequest ex) {
		log.error(ex.getLocalizedMessage());
        ResponseDTO response = ResponseDTO.builder()
                .success(false)
                .message(ex.getMessage())
                .code(HttpStatus.BAD_REQUEST.value())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
	
	@ExceptionHandler(ProcessGenericException.class)
    public ResponseEntity<ResponseDTO> processGenericException(ProcessGenericException ex) {
		log.error(ex.getLocalizedMessage());
        ResponseDTO response = ResponseDTO.builder()
                .success(false)
                .message(ex.getMessage())
                .code(HttpStatus.NOT_FOUND.value())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
	
	@ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ResponseDTO> handleUserNotFound(UserNotFoundException ex) {
		log.error(ex.getLocalizedMessage());
        ResponseDTO response = ResponseDTO.builder()
                .success(false)
                .message(ex.getMessage())
                .code(HttpStatus.NOT_FOUND.value())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ResponseDTO> handleInvalidCredentials(InvalidCredentialsException ex) {
    	log.error(ex.getLocalizedMessage());
        ResponseDTO response = ResponseDTO.builder()
                .success(false)
                .message(ex.getMessage())
                .code(HttpStatus.UNAUTHORIZED.value())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ResponseDTO> handleSecurity(SecurityException ex) {
        log.error(ex.getLocalizedMessage());
        ResponseDTO response = ResponseDTO.builder()
                .success(false)
                .message(ex.getMessage())
                .code(HttpStatus.FORBIDDEN.value())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(SecureRequestException.class)
    public ResponseEntity<ResponseDTO> handleSecureRequest(SecureRequestException ex) {
        log.warn("[SecureRequest] {}", ex.getMessage());
        ResponseDTO response = ResponseDTO.builder()
                .success(false)
                .message(ex.getMessage())
                .code(ex.getStatus().value())
                .build();
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseDTO> handleIllegalArgument(IllegalArgumentException ex) {
        log.error(ex.getLocalizedMessage());
        ResponseDTO response = ResponseDTO.builder()
                .success(false)
                .message(ex.getMessage())
                .code(HttpStatus.BAD_REQUEST.value())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO> handleGenericException(Exception ex) {
    	log.error(ex.getLocalizedMessage());
        ResponseDTO response = ResponseDTO.builder()
                .success(false)
                .message(Constantes.INTERNAL_SERVER_ERROR)
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
