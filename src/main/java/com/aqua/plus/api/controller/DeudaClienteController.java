package com.aqua.plus.api.controller;

import java.time.LocalDate;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aqua.plus.api.service.impl.DeudaClienteServiceImpl;
import com.aqua.plus.commons.dtos.DeudaClienteDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/deuda-cliente")
@Tag(name = "DeudaCliente - Controller", description = "Controller encargado de gestionar las operaciones de las deudas del cliente")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
public class DeudaClienteController {

	private final DeudaClienteServiceImpl deudaClienteServiceImpl;

	@Operation(summary = "Guardar o actualizar Deuda del Cliente")
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
	public ResponseEntity<ResponseDTO> save(@RequestBody DeudaClienteDTO deudaClienteDTO) {
		return deudaClienteServiceImpl.save(deudaClienteDTO);
	}

	@Operation(summary = "Buscar Deuda del Cliente  por id")
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
		return deudaClienteServiceImpl.findById(id);
	}

	@Operation(summary = "Buscar la Deuda del Cliente más reciente por EmpresaClienteContador (eccId)", description = "Retorna la última deuda activa asociada al id de EmpresaClienteContador (reciente por fechaCreacion DESC).")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Consulta exitosa", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }) })
	@GetMapping("/cliente-deuda/{eccId}")
	public ResponseEntity<ResponseDTO> getLatestByEmpresaClienteContadorId(@PathVariable Integer eccId) {
		return deudaClienteServiceImpl.findByEmpresaClienteContadorId(eccId);
	}

	@Operation(summary = "Buscar deudas consolidadas por TipoDeuda para un EmpresaClienteContador (eccId)", description = "Retorna un listado consolidado de deudas activas agrupadas por TipoDeuda para el eccId indicado.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Consulta exitosa", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }) })
	@GetMapping("/consolidado/{eccId}")
	public ResponseEntity<ResponseDTO> getDeudaConsolidadaByEmpresaClienteContadorId(@PathVariable Integer eccId) {
		return deudaClienteServiceImpl.findConsolidadoByEmpresaClienteContadorId(eccId);
	}

	@Operation(summary = "Listar Deudas de Cliente por id de empresa (paginado + filtros opcionales)")
	@ApiResponse(responseCode = "200", description = "Consulta exitosa", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)))
	@ApiResponse(responseCode = "400", description = "Petición inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)))
	@ApiResponse(responseCode = "404", description = "Sin resultados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)))
	@ApiResponse(responseCode = "500", description = "Error interno", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)))
	@GetMapping("/empresa/{idEmpresa}")
	public ResponseEntity<ResponseDTO> listarDeudasPorEmpresa(@PathVariable Integer idEmpresa,
			@Parameter(description = "Buscar por nombre del cliente (LIKE, opcional)") @RequestParam(required = false) String clienteNombre,
			@Parameter(description = "Buscar por código de factura (LIKE, opcional)") @RequestParam(required = false) String facturaCodigo,
			@Parameter(description = "Buscar por descripción (LIKE, opcional)") @RequestParam(required = false) String descripcion,
			@Parameter(description = "Fecha exacta de la deuda (yyyy-MM-dd), opcional", example = "2025-09-17") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDeuda,
			@Parameter(description = "Filtrar por valor exacto de la deuda, opcional", example = "50000") @RequestParam(required = false) Double valor,
			@Parameter(description = "Filtrar por nombre del tipo de deuda (LIKE), opcional", example = "Factura Vencida") @RequestParam(required = false) String tipoDeudaNombre,
			@Parameter(description = "Filtrar por plazo pago, opcional", example = "2") @RequestParam(required = false) Integer plazoPago,
			@ParameterObject Pageable pageable) {
		return deudaClienteServiceImpl.findByIdEnterprise(idEmpresa, clienteNombre, facturaCodigo,
				descripcion, fechaDeuda, valor, tipoDeudaNombre, plazoPago, pageable);
	}

	@Operation(summary = "Listar todos las deudas del cliente")
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
		return deudaClienteServiceImpl.findAll();
	}

	@Operation(summary = "Eliminar deuda del cliente por id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Rol eliminado correctamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "Rol no encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Error interno del servidor", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@DeleteMapping("/{id}")
	public ResponseEntity<ResponseDTO> deleteById(@PathVariable Integer id) {
		return deudaClienteServiceImpl.deleteById(id);
	}

	@Operation(summary = "Actualizar deuda del cliente")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Se ha actualizado satisfactoriamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@PutMapping
	public ResponseEntity<ResponseDTO> update(@RequestBody DeudaClienteDTO deudaClienteDTO) {
		return deudaClienteServiceImpl.updateDeuda(deudaClienteDTO);
	}

}
