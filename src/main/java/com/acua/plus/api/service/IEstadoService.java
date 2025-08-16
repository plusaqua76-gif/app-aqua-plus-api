package com.acua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.acua.plus.commons.dtos.EstadoDTO;
import com.acua.plus.commons.dtos.ResponseDTO;

public interface IEstadoService {
	
	ResponseEntity<ResponseDTO> save(EstadoDTO estadoDTO);
    ResponseEntity<ResponseDTO> findById(Integer id);
    ResponseEntity<ResponseDTO> findAll();
    ResponseEntity<ResponseDTO> deleteById(Integer id);
}
