package com.aqua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ClienteNovedadDTO;
import com.aqua.plus.commons.dtos.ClienteNovedadRequestDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IClienteNovedadService {

	ResponseEntity<ResponseDTO> save(ClienteNovedadRequestDTO req);

	ResponseEntity<ResponseDTO> update(ClienteNovedadDTO clienteNovedadDTO);

	ResponseEntity<ResponseDTO> findById(Integer id);

	ResponseEntity<ResponseDTO> findAll();

	ResponseEntity<ResponseDTO> deleteById(Integer id);
}
