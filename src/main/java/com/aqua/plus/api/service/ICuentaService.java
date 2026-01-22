package com.aqua.plus.api.service;

import java.util.Date;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.CuentaDTO;
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
public interface ICuentaService {

	ResponseEntity<ResponseDTO> save(CuentaDTO cuentaDTO);
    ResponseEntity<ResponseDTO> findById(Integer id);
    ResponseEntity<ResponseDTO> findAll();
    ResponseEntity<ResponseDTO> findByEmpresa(Integer idEmpresa, String cuentaCodigo, String cuentaNombre,
			Double cuentaValor, String tipoNombre, String tipoNaturaleza, Pageable pageable);
    ResponseEntity<ResponseDTO> deleteById(Integer id);
    
    ResponseEntity<ResponseDTO> findCuentas(Integer idEmpresa, Date fechaInicio, Date fechaFin,
			Integer page, Integer size);
}
