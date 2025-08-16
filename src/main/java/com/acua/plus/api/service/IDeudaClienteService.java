package com.acua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.acua.plus.commons.dtos.DeudaClienteDTO;
import com.acua.plus.commons.dtos.ResponseDTO;

public interface IDeudaClienteService {
    
	ResponseEntity<ResponseDTO> save(DeudaClienteDTO deudaClienteDTO);
	
    ResponseEntity<ResponseDTO> findById(Integer id);
    
    ResponseEntity<ResponseDTO> findAll();
    
    ResponseEntity<ResponseDTO> deleteById(Integer id);
    
    ResponseEntity<ResponseDTO> updateDeuda(DeudaClienteDTO deudaClienteDTO);

}
