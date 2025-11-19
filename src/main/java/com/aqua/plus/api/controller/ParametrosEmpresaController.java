package com.aqua.plus.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aqua.plus.api.service.impl.ParametrosEmpresaServiceImpl;
import com.aqua.plus.commons.dtos.ParametrosEmpresaDTO;
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
 *          tipo (ParametrosEmpresa).
 */

@RestController
@RequestMapping("/api/v1/parametros-empresa")
@Tag(name = "ParametrosEmpresa - Controller", description = "Controller encargado de gestionar las operaciones de los Parametros Empresa")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
public class ParametrosEmpresaController {

	private final ParametrosEmpresaServiceImpl parametrosEmpresaServiceImpl;

	@Operation(summary = "Guardar o actualizar Parametros Empresa")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Se ha guardado satisfactoriamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "200", description = "Se ha actualizado satisfactoriamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@PostMapping
	public ResponseEntity<ResponseDTO> save(@RequestBody ParametrosEmpresaDTO parametrosEmpresaDTO) {
		return parametrosEmpresaServiceImpl.save(parametrosEmpresaDTO);
	}

	@Operation(summary = "Buscar Parametros Empresa por id de empresa")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Se ha guardado satisfactoriamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@GetMapping("/{id}")
	public ResponseEntity<ResponseDTO> getById(@PathVariable Integer id) {
		return parametrosEmpresaServiceImpl.findByIdEnterprise(id);
	}

	@Operation(summary = "Consultar parámetro de empresa por llave")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Consulta exitosa", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error interno", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@GetMapping("/empresa/{idEmpresa}/parametro")
	public ResponseEntity<ResponseDTO> findParametroByLlave(@PathVariable Integer idEmpresa,
			@RequestParam String llave) {
		return parametrosEmpresaServiceImpl.findByEmpresaAndLlave(idEmpresa, llave);
	}

}
