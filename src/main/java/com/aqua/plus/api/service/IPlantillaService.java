package com.aqua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.PlantillaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IPlantillaService {

	ResponseEntity<ResponseDTO> save(PlantillaDTO plantillaDTO);

	ResponseEntity<ResponseDTO> findByEmpresa(Integer idEmpresa);
}
