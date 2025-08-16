package com.aqua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ParametrosSistemaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IParametrosSistemaService {
	   
	    ResponseEntity<ResponseDTO> save(ParametrosSistemaDTO parametrosSistemaDTO);
		
	    ResponseEntity<ResponseDTO> findById(Integer id);
	    
	    ResponseEntity<ResponseDTO> findAll();
	    
	    ResponseEntity<ResponseDTO> deleteById(Integer id);
}
