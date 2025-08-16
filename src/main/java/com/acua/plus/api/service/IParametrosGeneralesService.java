package com.acua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.acua.plus.commons.dtos.ParametrosGeneralesDTO;
import com.acua.plus.commons.dtos.ResponseDTO;

public interface IParametrosGeneralesService {
	
	ResponseEntity<ResponseDTO> save(ParametrosGeneralesDTO parametrosGeneralesDTO);
	
    ResponseEntity<ResponseDTO> findById(Integer id);
    
    ResponseEntity<ResponseDTO> findAll();
    
    ResponseEntity<ResponseDTO> deleteById(Integer id);
}
