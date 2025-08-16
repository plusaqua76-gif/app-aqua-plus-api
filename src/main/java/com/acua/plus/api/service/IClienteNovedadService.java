package com.acua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.acua.plus.commons.dtos.ClienteNovedadDTO;
import com.acua.plus.commons.dtos.ResponseDTO;

public interface IClienteNovedadService {
	
	ResponseEntity<ResponseDTO> save(ClienteNovedadDTO clienteNovedadDTO);
	
	ResponseEntity<ResponseDTO> update(ClienteNovedadDTO clienteNovedadDTO);
	
    ResponseEntity<ResponseDTO> findById(Integer id);
    
    ResponseEntity<ResponseDTO> findAll();
    
    ResponseEntity<ResponseDTO> deleteById(Integer id);
}
