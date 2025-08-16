package com.aqua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.EstadoDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IEstadoService {
	
	ResponseEntity<ResponseDTO> save(EstadoDTO estadoDTO);
    ResponseEntity<ResponseDTO> findById(Integer id);
    ResponseEntity<ResponseDTO> findAll();
    ResponseEntity<ResponseDTO> deleteById(Integer id);
}
