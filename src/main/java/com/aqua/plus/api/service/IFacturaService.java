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
			String fechaEmision, String fechaFin, String estadoNombre, Boolean consumoAnormal, Integer consumo, Double precioMin,
			Double precioMax, String tipoPagoNombre, String corregimientoNombre, Pageable pageable);

	ResponseEntity<ResponseDTO> findAll();

	ResponseEntity<ResponseDTO> deleteById(Integer id);
	
	ResponseEntity<ResponseDTO> obtenerFacturaDetalle(Integer idFactura, Integer idEmpresa);

    // Interface
    ResponseEntity<ResponseDTO> findFacturasByPersona(
            Integer idPersona, String codigo, String fechaEmision, String fechaFin, String estadoNombre,
            Boolean consumoAnormal, Double precio, Pageable pageable
    );

	/**
	 * Obtiene las métricas de cartera agrupadas por antigüedad de deudas.
	 * 
	 * @param empresaId ID de la empresa
	 * @return ResponseEntity con las métricas de cartera por rangos de antigüedad
	 */
	ResponseEntity<ResponseDTO> obtenerMetricasCarteraPorAntiguedad(Integer empresaId);


	ResponseEntity<ResponseDTO> obtenerMetricasFinancieras(Integer empresaId, Integer mes, Integer anio);
	
	ResponseEntity<ResponseDTO> findByEmpresaClienteContadorAndCodigo(Integer idEmpresaClienteContador, String codigo);

}
