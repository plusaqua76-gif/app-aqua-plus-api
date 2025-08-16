package com.aqua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.TipoNovedadDTO;

public interface ITipoNovedadService {
	
    ResponseEntity<ResponseDTO> save(TipoNovedadDTO tipoNovedadDTO);
	
    ResponseEntity<ResponseDTO> findById(Integer id);
    
    ResponseEntity<ResponseDTO> findAll();
    
    ResponseEntity<ResponseDTO> deleteById(Integer id);

}
