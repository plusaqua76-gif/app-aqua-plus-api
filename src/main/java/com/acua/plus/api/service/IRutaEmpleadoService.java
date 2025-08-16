package com.acua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.dtos.RutaEmpleadoDTO;

public interface IRutaEmpleadoService {
    
	ResponseEntity<ResponseDTO> save(RutaEmpleadoDTO rutaEmpleadoDTO);
	
	ResponseEntity<ResponseDTO> update(RutaEmpleadoDTO rutaEmpleadoDTO); 
	
    ResponseEntity<ResponseDTO> findById(Integer id);
    
    ResponseEntity<ResponseDTO> findAll();
    
    ResponseEntity<ResponseDTO> deleteById(Integer id);
}
