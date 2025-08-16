package com.acua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.dtos.TipoNovedadDTO;

public interface ITipoNovedadService {
	
    ResponseEntity<ResponseDTO> save(TipoNovedadDTO tipoNovedadDTO);
	
    ResponseEntity<ResponseDTO> findById(Integer id);
    
    ResponseEntity<ResponseDTO> findAll();
    
    ResponseEntity<ResponseDTO> deleteById(Integer id);

}
