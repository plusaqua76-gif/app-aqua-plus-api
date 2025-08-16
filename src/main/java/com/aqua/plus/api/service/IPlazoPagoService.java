package com.aqua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.PlazoPagoDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IPlazoPagoService {
	
ResponseEntity<ResponseDTO> save(PlazoPagoDTO plazoPagoDTO);
	
    ResponseEntity<ResponseDTO> findById(Integer id);
    
    ResponseEntity<ResponseDTO> findAll();
    
    ResponseEntity<ResponseDTO> deleteById(Integer id);

}
