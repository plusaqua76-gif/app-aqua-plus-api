package com.aqua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.NominaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

public interface INominaService {
	
	ResponseEntity<ResponseDTO> save(NominaDTO nominaDTO);

	ResponseEntity<ResponseDTO> findByEmpleadoId(Integer idEmpleado);

	ResponseEntity<ResponseDTO> findAll();

	ResponseEntity<ResponseDTO> deleteById(Integer id);

}
