package com.acua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.dtos.TipoTarifaDTO;

/**
 * @author nicope
 * @version 1.0
 * 
 *          Esta interfaz es la capa intermedia entre la capa de presentación y
 *          la capa de acceso a datos. Esta oculta los detalles de
 *          implementación de la capa de acceso a datos.
 * 
 */

public interface ITipoTarifaService {

	ResponseEntity<ResponseDTO> save(TipoTarifaDTO tipoTarifaDTO);
    ResponseEntity<ResponseDTO> findById(Integer id);
    ResponseEntity<ResponseDTO> findAll();
    ResponseEntity<ResponseDTO> deleteById(Integer id);
    
}
