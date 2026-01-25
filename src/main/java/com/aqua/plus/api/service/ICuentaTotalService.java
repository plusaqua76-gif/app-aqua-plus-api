package com.aqua.plus.api.service;

import java.util.Date;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ResponseDTO;

public interface ICuentaTotalService {
	
	ResponseEntity<ResponseDTO> findCuentasTotales(
            Integer idEmpresa,
            Date fechaInicio,
            Date fechaFin,
            Integer page,
            Integer size); 

}
