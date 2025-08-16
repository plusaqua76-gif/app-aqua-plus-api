package com.acua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.acua.plus.commons.dtos.ParametrosSistemaDTO;
import com.acua.plus.commons.dtos.ResponseDTO;

public interface IParametrosSistemaService {
	   
	    ResponseEntity<ResponseDTO> save(ParametrosSistemaDTO parametrosSistemaDTO);
		
	    ResponseEntity<ResponseDTO> findById(Integer id);
	    
	    ResponseEntity<ResponseDTO> findAll();
	    
	    ResponseEntity<ResponseDTO> deleteById(Integer id);
}
