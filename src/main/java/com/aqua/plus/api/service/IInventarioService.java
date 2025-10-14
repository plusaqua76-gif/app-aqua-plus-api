package com.aqua.plus.api.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.InventarioDTO;
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

public interface IInventarioService {

	ResponseEntity<ResponseDTO> save(InventarioDTO inventarioDTO);

	ResponseEntity<ResponseDTO> findById(Integer id);

	ResponseEntity<ResponseDTO> findByEnterpriseId(Integer idEmpresa, Integer cantidad, Double precioUnitario,
			Double precioVenta, Integer porcentaje, String codigo, String nombre, String descripcion,
			Pageable pageable);

	ResponseEntity<ResponseDTO> findAll();

	ResponseEntity<ResponseDTO> deleteById(Integer id);
}
