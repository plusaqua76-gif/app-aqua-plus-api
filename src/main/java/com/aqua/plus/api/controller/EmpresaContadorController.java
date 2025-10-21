package com.aqua.plus.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.aqua.plus.api.service.impl.EmpresaContadorServiceImpl;
import com.aqua.plus.commons.dtos.OqECLectRequestDTO;
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
 *          tipo (EmpresaContador).
 */

@RestController
@RequestMapping("/api/v1/empresa-contador")
@Tag(name = "EmpresaContador - Controller", description = "Controller encargado de gestionar las operaciones de la empresa contador")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
public class EmpresaContadorController {

	private final EmpresaContadorServiceImpl empresaContadorServiceImpl;

	@Operation(summary = "Guardar empresa-contador y Lectura")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Guardado exitoso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error interno", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))), })
	@PostMapping
	public ResponseEntity<ResponseDTO> saveEmpresaContadorAndLectura(@RequestBody OqECLectRequestDTO request) {
		return empresaContadorServiceImpl.saveEmpresaContadorAndLectura(request.getEmpresaContador(),
				request.getLectura());
	}

	@Operation(summary = "Consultar empresa-contador por id de empresa")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Consulta exitosa", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error interno", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))), })
	@GetMapping("/empresa/{idEmpresa}")
	public ResponseEntity<ResponseDTO> findByEmpresa(@PathVariable Integer idEmpresa) {
		return empresaContadorServiceImpl.findByEmpresaId(idEmpresa);
	}
}
