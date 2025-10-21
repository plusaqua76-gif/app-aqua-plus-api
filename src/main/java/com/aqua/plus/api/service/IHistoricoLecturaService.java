package com.aqua.plus.api.service;

import org.springframework.http.ResponseEntity;

/**
 * @author nicope
 * @version 1.0
 * 
 *          Esta interfaz es la capa intermedia entre la capa de presentación y
 *          la capa de acceso a datos. Esta oculta los detalles de
 *          implementación de la capa de acceso a datos.
 * 
 */

import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IHistoricoLecturaService {

	ResponseEntity<ResponseDTO> findHistoricoByLecturaId(Integer idLectura);
}
