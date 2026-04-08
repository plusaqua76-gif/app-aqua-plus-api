package com.aqua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IFcmTokenService {

    ResponseEntity<ResponseDTO> guardarToken(Long usuarioId, String token, String dispositivo, String usuarioCreacion);

}
