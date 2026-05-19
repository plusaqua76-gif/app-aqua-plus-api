package com.aqua.plus.api.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.SaldoClienteDTO;

/**
 * @author nicope
 * @version 1.0
 * 
 *          Esta interfaz es la capa intermedia entre la capa de presentación y
 *          la capa de acceso a datos. Esta oculta los detalles de
 *          implementación de la capa de acceso a datos.
 * 
 */
public interface ISaldoClienteService {

	ResponseEntity<ResponseDTO> save(SaldoClienteDTO personaDTO);

	ResponseEntity<ResponseDTO> findById(Integer id);

	ResponseEntity<ResponseDTO> findAll();

	ResponseEntity<ResponseDTO> deleteById(Integer id);

	ResponseEntity<ResponseDTO> findAllByEmpresaClienteContadorId(Integer idEmpresaClienteContador, Pageable pageable,
			String nombre, String cedula, String codigo, Boolean estado, Integer nuid, Integer saldoTotal,
			Integer saldoDisponible, Integer cuotas);
}
