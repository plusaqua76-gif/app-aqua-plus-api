package com.acua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.acua.plus.commons.dtos.FacturaDTO;
import com.acua.plus.commons.dtos.ResponseDTO;

public interface IFacturaService {
    
	ResponseEntity<ResponseDTO> save(FacturaDTO facturaDTO);
	
	ResponseEntity<ResponseDTO> update(FacturaDTO facturaDTO); 
	
    ResponseEntity<ResponseDTO> findById(Integer id);
    
    ResponseEntity<ResponseDTO> findByEnterpriseId(Integer idEmpresa);
    
    ResponseEntity<ResponseDTO> findAll();
    
    ResponseEntity<ResponseDTO> deleteById(Integer id);

}
