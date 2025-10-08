package com.aqua.plus.api.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.aqua.plus.api.service.impl.ReporteFiltroServiceImpl;
import com.aqua.plus.commons.dtos.ResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * @author nicope
 * @version 1.0
 * 
 *          Controlador que expone los servicios para trabajar con objeto(s) de
 *          tipo (RolMenu).
 */

@RestController
@RequestMapping("/api/v1/reporte-filtro")
@Tag(name = "ReporteFiltro - Controller", description = "Controller encargado de gestionar las operaciones de los reportes y sus filtros")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
public class ReporteFiltroController {

	private final ReporteFiltroServiceImpl reporteFiltroServiceImpl;

	@Operation(summary = "Listar filtros activos por reporte")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Consulta exitosa", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "400", description = "Petición inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "404", description = "No se encontraron datos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error inesperado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@GetMapping("/reporte/{id}")
	public ResponseEntity<ResponseDTO> getFiltrosByReporte(@PathVariable("id") Integer idReporte) {
		return reporteFiltroServiceImpl.findByReporteId(idReporte);
	}


	@PostMapping("/fn-list/{schema}/{nombre}")
	public ResponseEntity<List<Map<String, Object>>> ejecutarFnJsonLista(@PathVariable String schema,
			@PathVariable String nombre, @RequestBody(required = false) Map<String, Object> filtros) {

		List<Map<String, Object>> out = reporteFiltroServiceImpl.ejecutarFuncionJsonLista(
				(schema == null || schema.isBlank()) ? "reportes" : schema, nombre, filtros == null ? Map.of() : filtros);
		return ResponseEntity.ok(out);
	}

}
