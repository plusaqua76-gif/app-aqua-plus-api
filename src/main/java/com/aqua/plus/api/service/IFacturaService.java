package com.aqua.plus.api.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.FacturaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IFacturaService {

	ResponseEntity<ResponseDTO> save(FacturaDTO facturaDTO);

	ResponseEntity<ResponseDTO> update(FacturaDTO facturaDTO);

	ResponseEntity<ResponseDTO> findById(Integer id);

	ResponseEntity<ResponseDTO> findByEnterpriseId(Integer idEmpresa, String codigo, String clienteNombreCompleto,
			Integer consumo, String fechaEmision, String fechaFin, String estadoNombre, Boolean consumoAnormal,
			Double precioMin, Double precioMax, Pageable pageable);

	ResponseEntity<ResponseDTO> findAll();

	ResponseEntity<ResponseDTO> deleteById(Integer id);

}
