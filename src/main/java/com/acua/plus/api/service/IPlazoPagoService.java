package com.acua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.acua.plus.commons.dtos.PlazoPagoDTO;
import com.acua.plus.commons.dtos.ResponseDTO;

public interface IPlazoPagoService {
	
ResponseEntity<ResponseDTO> save(PlazoPagoDTO plazoPagoDTO);
	
    ResponseEntity<ResponseDTO> findById(Integer id);
    
    ResponseEntity<ResponseDTO> findAll();
    
    ResponseEntity<ResponseDTO> deleteById(Integer id);

}
