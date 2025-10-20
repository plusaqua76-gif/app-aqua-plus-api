package com.aqua.plus.api.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.aqua.plus.api.service.impl.FiltroParametroServiceImpl;
import com.aqua.plus.commons.dtos.ResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author nicope
 * @version 1.0
 * 
 *          Controlador que expone los servicios para trabajar con objeto(s) de
 *          tipo (FiltroParametro).
 */

@RestController
@RequestMapping("/api/v1/filtro-parametro")
@Tag(name = "FiltroParametro - Controller", description = "Controller encargado de gestionar las operaciones de los Filtros Parametros")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
@Slf4j
public class FiltroParametroController {

	private final FiltroParametroServiceImpl filtroParametroServiceImpl;

	@Operation(summary = "Listar filtros activos por reporte")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Consulta exitosa", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "400", description = "Petición inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "404", description = "No se encontraron datos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error inesperado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@GetMapping("/reporte/{id}")
	public ResponseEntity<ResponseDTO> getFiltrosByReporte(@PathVariable("id") Integer idReporte) {
		return filtroParametroServiceImpl.findByReporteId(idReporte);
	}

	@Operation(summary = "Consultar Listas", description = "Invoca reportes.run_list_param_select(codigo, params) y retorna un arreglo JSON envuelto en ResponseDTO.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Consulta exitosa", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "400", description = "Petición inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "404", description = "No se encontraron datos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error inesperado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@PostMapping(value = "/run-list/{codigo}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseDTO> runList(@PathVariable String codigo,
			@RequestBody(required = false) Map<String, Object> params) {

		if (codigo == null || codigo.isBlank()) {
			return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
					.message("Parámetro requerido: codigo").code(HttpStatus.BAD_REQUEST.value()).build());
		}

		Map<String, Object> safeParams = (params == null) ? Collections.emptyMap() : params;

		List<Map<String, Object>> data = filtroParametroServiceImpl.runListParamSelect(codigo, safeParams);

		ResponseDTO body = ResponseDTO.builder().success(true).message("OK").code(HttpStatus.OK.value()).response(data)
				.build();

		return ResponseEntity.ok(body);
	}

}
