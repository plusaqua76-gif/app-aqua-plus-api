package com.aqua.plus.api.service.external;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IListaDianService {
	
	public ResponseEntity<ResponseDTO> getLista(String endPoint,String departamento);
}
