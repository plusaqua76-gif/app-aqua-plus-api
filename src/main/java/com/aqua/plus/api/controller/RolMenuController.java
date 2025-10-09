package com.aqua.plus.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aqua.plus.api.service.impl.RolMenuServiceImpl;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.RolMenuDTO;
import com.aqua.plus.commons.dtos.RolMenuRequestDTO;

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
 *          tipo (RolMenu).
 */

@RestController
@RequestMapping("/api/v1/rol-menu")
@Tag(name = "RolMenu - Controller", description = "Controller encargado de gestionar las operaciones de los roles menus")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
public class RolMenuController {

	private final RolMenuServiceImpl rolMenuServiceImpl;

	@Operation(summary = "Guardar o actualizar rol menu")
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
	public ResponseEntity<ResponseDTO> save(@RequestBody RolMenuDTO rolMenuDTO) {
		return rolMenuServiceImpl.save(rolMenuDTO);
	}

	@Operation(summary = "Crear vínculos rol–menú (solo guardado)", description = "Crea múltiples vínculos rol–menú en una sola operación. "
			+ "Si existe cualquier duplicado, no guarda nada y responde 409.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Guardado exitosamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "400", description = "Petición inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "409", description = "Duplicados detectados; no se guardó", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error inesperado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@PostMapping("/rol-menu")
	public ResponseEntity<ResponseDTO> createMenusForRole(@RequestBody RolMenuRequestDTO req) {
		return rolMenuServiceImpl.createMenusForRole(req);
	}

	@Operation(summary = "Listar todos los roles menus")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Se ha guardado satisfactoriamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@GetMapping("/all")
	public ResponseEntity<ResponseDTO> getAll() {
		return rolMenuServiceImpl.findAll();
	}

	@Operation(summary = "Listar menú permitido por Rol y Empresa")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Consulta exitosa", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@GetMapping("/empresa")
	public ResponseEntity<ResponseDTO> getRolMenuByEmpresaAndRol(
			@Parameter(description = "ID de la empresa", required = true) @RequestParam(name = "empresaId", required = true) Integer empresaId,
			@Parameter(description = "ID del rol", required = true) @RequestParam(name = "rolId", required = true) Integer rolId) {
		return rolMenuServiceImpl.findByRolAndEmpresa(rolId, empresaId);
	}

	@Operation(summary = "Eliminar rol Menu por id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Rol eliminado correctamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "Rol no encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Error interno del servidor", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@DeleteMapping("/{id}")
	public ResponseEntity<ResponseDTO> deleteById(@PathVariable Integer id) {
		return rolMenuServiceImpl.deleteById(id);
	}

}
