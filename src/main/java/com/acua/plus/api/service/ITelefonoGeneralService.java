package com.acua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.dtos.TelefonoGeneralDTO;

public interface ITelefonoGeneralService {
	
	ResponseEntity<ResponseDTO> save(TelefonoGeneralDTO telefonoGeneralDTO);
	
    ResponseEntity<ResponseDTO> findById(Integer id);
    
    ResponseEntity<ResponseDTO> findAll();
    
    ResponseEntity<ResponseDTO> deleteById(Integer id);
}
