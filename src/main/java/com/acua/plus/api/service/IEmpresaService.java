package com.acua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.acua.plus.commons.dtos.EmpresaDTO;
import com.acua.plus.commons.dtos.ResponseDTO;

public interface IEmpresaService {
	
	ResponseEntity<ResponseDTO> save(EmpresaDTO empresaDTO);
	
	ResponseEntity<ResponseDTO> update(EmpresaDTO empresaDTO);
	
    ResponseEntity<ResponseDTO> findById(Integer id);
    
    ResponseEntity<ResponseDTO> findByUsuarioId(Integer idUsuario);
    
    ResponseEntity<ResponseDTO> getAllEnterpriseResponseId();
    
    ResponseEntity<ResponseDTO> findAll();
    
    ResponseEntity<ResponseDTO> deleteById(Integer id);
}
