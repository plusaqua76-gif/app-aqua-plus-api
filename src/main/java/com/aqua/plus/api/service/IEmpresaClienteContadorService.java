package com.aqua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.EmpresaClienteContadorDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IEmpresaClienteContadorService {
    
	ResponseEntity<ResponseDTO> save(EmpresaClienteContadorDTO empresaClienteContadorDTO);
	
	ResponseEntity<ResponseDTO> update(EmpresaClienteContadorDTO empresaClienteContadorDTO);
	
	ResponseEntity<ResponseDTO> findByEmpresaId(Integer idEmpresa);
	
	ResponseEntity<ResponseDTO> findByEmpresaIdResponseId(Integer idEmpresa);
	
    ResponseEntity<ResponseDTO> findById(Integer id);
    
    ResponseEntity<ResponseDTO> findAll();
    
    ResponseEntity<ResponseDTO> deleteById(Integer id);
}
