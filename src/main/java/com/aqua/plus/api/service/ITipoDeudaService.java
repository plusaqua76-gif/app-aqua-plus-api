package com.aqua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.TipoDeudaDTO;

public interface ITipoDeudaService {

	ResponseEntity<ResponseDTO> save(TipoDeudaDTO tipoDeudaDTO);
	
    ResponseEntity<ResponseDTO> findById(Integer id);
    
    ResponseEntity<ResponseDTO> findAll();
    
    ResponseEntity<ResponseDTO> deleteById(Integer id);
}
