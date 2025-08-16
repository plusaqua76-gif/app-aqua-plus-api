package com.acua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.acua.plus.commons.dtos.AbonoDTO;
import com.acua.plus.commons.dtos.ResponseDTO;

public interface IAbonoService {
	
	ResponseEntity<ResponseDTO> save(AbonoDTO abonoDTO);
	
	ResponseEntity<ResponseDTO> update(AbonoDTO abonoDTO);
	
    ResponseEntity<ResponseDTO> findById(Integer id);
    
    ResponseEntity<ResponseDTO> findAll();
    
    ResponseEntity<ResponseDTO> deleteById(Integer id);
}
