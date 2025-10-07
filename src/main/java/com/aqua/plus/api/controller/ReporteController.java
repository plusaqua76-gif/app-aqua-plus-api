package com.aqua.plus.api.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aqua.plus.api.service.impl.ReporteServiceImpl;
import com.aqua.plus.commons.dtos.ResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 *          tipo (Reporte).
 */

@RestController
@RequestMapping("/api/v1/reporte")
@Tag(name = "Reporte - Controller", description = "Controller encargado de gestionar las operaciones de los reportes")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
public class ReporteController {

	private final ReporteServiceImpl reporteServiceImpl;
	
	@Operation(summary = "Listar reportes (activos) con filtro por nombre y paginación")
	@ApiResponses(value = {
	        @ApiResponse(responseCode = "200", description = "Consulta exitosa",
	                content = @Content(mediaType = "application/json",
	                        schema = @Schema(implementation = ResponseDTO.class))),
	        @ApiResponse(responseCode = "404", description = "No se encontraron datos",
	                content = @Content(mediaType = "application/json",
	                        schema = @Schema(implementation = ResponseDTO.class))),
	        @ApiResponse(responseCode = "500", description = "Error inesperado",
	                content = @Content(mediaType = "application/json",
	                        schema = @Schema(implementation = ResponseDTO.class)))
	})
	@GetMapping("/reportes")
	public ResponseEntity<ResponseDTO> getReportes(
	        @Parameter(description = "Filtro por nombre (like)", required = false)
	        @RequestParam(required = false) String nombre,
	        Pageable pageable
	) {
	    return reporteServiceImpl.findAll(nombre, pageable);
	}

}
