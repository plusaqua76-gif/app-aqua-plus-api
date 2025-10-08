package com.aqua.plus.api.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;

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

public interface IReporteFiltroService {

	ResponseEntity<ResponseDTO> findByReporteId(Integer idReporte);

	List<Map<String, Object>> ejecutarFuncionJsonLista(
            String schema, String functionName, Map<String, Object> filtrosJson);
	
}
