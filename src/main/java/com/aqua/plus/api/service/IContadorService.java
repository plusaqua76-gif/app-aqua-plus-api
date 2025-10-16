package com.aqua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ContadorDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

/**
 * @author nicope
 * @version 1.0
 * 
 *          Esta interfaz es la capa intermedia entre la capa de presentación y
 *          la capa de acceso a datos. Esta oculta los detalles de
 *          implementación de la capa de acceso a datos.
 * 
 */

public interface IContadorService {

	ResponseEntity<ResponseDTO> save(ContadorDTO contadorDTO);

	ResponseEntity<ResponseDTO> findById(Integer id);

	ResponseEntity<ResponseDTO> findAll();

	ResponseEntity<ResponseDTO> deleteById(Integer id);

	ResponseEntity<ResponseDTO> findContadorPorSerial(String serial);

}
