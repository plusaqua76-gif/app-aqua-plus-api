package com.aqua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ParametrosGeneralesDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IParametrosGeneralesService {

	ResponseEntity<ResponseDTO> save(ParametrosGeneralesDTO parametrosGeneralesDTO);

	ResponseEntity<ResponseDTO> findById(Integer id);

	ResponseEntity<ResponseDTO> findAll();

	ResponseEntity<ResponseDTO> deleteById(Integer id);

	ResponseEntity<ResponseDTO> findByCodigoPadre(String codigoPadre, Boolean soloActivos);
}
