package com.aqua.plus.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.aqua.plus.api.service.impl.AforoContadorServiceImpl;
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
 *          tipo (AforoContador).
 */

@RestController
@RequestMapping("/api/v1/aforo-contador")
@Tag(name = "AforoContador - Controller", description = "Controller encargado de gestionar las operaciones de aforos contadores")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
public class AforoContadorController {

	private final AforoContadorServiceImpl aforoContadorServiceImpl;

	@Operation(summary = "Eliminar aforo contador por id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Aforo contador eliminado correctamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "Aforo contador no encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Error interno del servidor", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@DeleteMapping("/{id}")
	public ResponseEntity<ResponseDTO> deleteById(@PathVariable Integer id) {
		return aforoContadorServiceImpl.deleteById(id);
	}
}
