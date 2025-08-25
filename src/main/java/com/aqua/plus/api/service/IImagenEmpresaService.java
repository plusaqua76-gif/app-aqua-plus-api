package com.aqua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IImagenEmpresaService {

	ResponseEntity<ResponseDTO> findAll(Integer page, Integer size);
	
}
