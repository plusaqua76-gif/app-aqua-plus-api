package com.aqua.plus.api.controller;

import org.springframework.data.domain.Pageable;
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

import com.aqua.plus.api.service.impl.SaldoClienteServiceImpl;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.SaldoClienteDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/saldo-cliente")
@Tag(name = "SaldoCliente - Controller", description = "Controller encargado de gestionar las operaciones del saldo del Cliente")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
public class SaldoClienteController {

	private final SaldoClienteServiceImpl saldoClienteServiceImpl;

	@Operation(summary = "Guardar o actualizar estado")
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
	public ResponseEntity<ResponseDTO> save(@RequestBody SaldoClienteDTO estadoDTO) {
		return saldoClienteServiceImpl.save(estadoDTO);
	}

	@Operation(summary = "Buscar estado por id")
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
		return saldoClienteServiceImpl.findById(id);
	}

	@Operation(summary = "Listar todos los estados")
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
		return saldoClienteServiceImpl.findAll();
	}

	@Operation(summary = "Listar saldos de clientes por id de Cliente(empresaClienteContador) con filtros")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Consulta realizada satisfactoriamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@GetMapping("/{idEmpresa}")
	public ResponseEntity<ResponseDTO> findAllByEmpresaId(@PathVariable Integer idEmpresaClienteContador,
			Pageable pageable, @RequestParam(required = false) String nombre,
			@RequestParam(required = false) String cedula, @RequestParam(required = false) String codigo,
			@RequestParam(required = false) Boolean estado, @RequestParam(required = false) Integer nuid,
			@RequestParam(required = false) Integer saldoTotal, @RequestParam(required = false) Integer saldoDisponible,
			@RequestParam(required = false) Integer cuotas) {

		return saldoClienteServiceImpl.findAllByEmpresaClienteContadorId(idEmpresaClienteContador, pageable, nombre,
				cedula, codigo, estado, nuid, saldoTotal, saldoDisponible, cuotas);
	}

	@Operation(summary = "Eliminar estado por id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Rol eliminado correctamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "Rol no encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Error interno del servidor", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@DeleteMapping("/{id}")
	public ResponseEntity<ResponseDTO> deleteById(@PathVariable Integer id) {
		return saldoClienteServiceImpl.deleteById(id);
	}
}
