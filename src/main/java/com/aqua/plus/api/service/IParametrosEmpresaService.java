package com.aqua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ParametrosEmpresaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IParametrosEmpresaService {

	ResponseEntity<ResponseDTO> save(ParametrosEmpresaDTO parametrosEmpresaDTO);
	
	ResponseEntity<ResponseDTO> findByIdEnterprise(Integer id);
}
