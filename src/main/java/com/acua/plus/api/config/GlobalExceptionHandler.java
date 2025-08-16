package com.acua.plus.api.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.utils.Constantes;

/**
 * @author nicope
 * @version 1.0
 * 
 */

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ResponseDTO> handleUserNotFound(UserNotFoundException ex) {
        ResponseDTO response = ResponseDTO.builder()
                .success(false)
                .message(ex.getMessage())
                .code(HttpStatus.NOT_FOUND.value())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ResponseDTO> handleInvalidCredentials(InvalidCredentialsException ex) {
        ResponseDTO response = ResponseDTO.builder()
                .success(false)
                .message(ex.getMessage())
                .code(HttpStatus.UNAUTHORIZED.value())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO> handleGenericException(Exception ex) {
        ResponseDTO response = ResponseDTO.builder()
                .success(false)
                .message(Constantes.INTERNAL_SERVER_ERROR)
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
