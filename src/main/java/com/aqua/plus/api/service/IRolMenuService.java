package com.aqua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.RolMenuDTO;

public interface IRolMenuService {

	ResponseEntity<ResponseDTO> save(RolMenuDTO rolMenuDTO);
	
	ResponseEntity<ResponseDTO> findAll();
}
