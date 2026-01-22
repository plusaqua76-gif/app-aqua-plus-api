package com.aqua.plus.api.service;

import java.util.Date;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IHistoricoCuentaService {
	
	ResponseEntity<ResponseDTO> findHistoricoCuenta(Integer idEmpresa, Date fechaInicio, Date fechaFin, Integer page, Integer size);

	
}
