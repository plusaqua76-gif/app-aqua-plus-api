package com.acua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.acua.plus.commons.dtos.CorreoGeneralDTO;
import com.acua.plus.commons.dtos.ResponseDTO;

public interface ICorreoGeneralService {

	ResponseEntity<ResponseDTO> save(CorreoGeneralDTO correoGeneralDTO);
	
    ResponseEntity<ResponseDTO> findById(Integer id);
    
    ResponseEntity<ResponseDTO> findAll();
    
    ResponseEntity<ResponseDTO> deleteById(Integer id);
}
