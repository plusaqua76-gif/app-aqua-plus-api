package com.aqua.plus.api.controller;

import java.util.Date;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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

import com.aqua.plus.api.service.impl.CuentaServiceImpl;
import com.aqua.plus.commons.dtos.CuentaDTO;
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
 *          tipo (Cuenta).
 */

@RestController
@RequestMapping("/api/v1/cuenta")
@Tag(name = "Cuenta - Controller", description = "Controller encargado de gestionar las operaciones de las cuentas")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
public class CuentaController {

	private final CuentaServiceImpl cuentaServiceImpl;

	@Operation(summary = "Guardar o actualizar cuenta")
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
	public ResponseEntity<ResponseDTO> save(@RequestBody CuentaDTO cuentaDTO) {
		return cuentaServiceImpl.save(cuentaDTO);
	}

	@Operation(summary = "Buscar cuenta por id")
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
		return cuentaServiceImpl.findById(id);
	}

	@Operation(summary = "Listar todas las cuentas")
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
		return cuentaServiceImpl.findAll();
	}

	@Operation(summary = "Listar cuentas por empresa (con filtros opcionales y paginación)", description = "El parámetro idEmpresa es obligatorio. Filtros opcionales de Cuenta: codigo, nombre, valor. "
			+ "Filtros opcionales de Tipo de Cuenta: tipoNombre, tipoNaturaleza.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Consulta exitosa", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "404", description = "Sin resultados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error inesperado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@GetMapping("/empresa/{idEmpresa}")
	public ResponseEntity<ResponseDTO> getCuentasByEmpresa(
			@PathVariable("idEmpresa") @jakarta.validation.constraints.NotNull Integer idEmpresa,
			@RequestParam(value = "codigo", required = false) String cuentaCodigo,
			@RequestParam(value = "nombre", required = false) String cuentaNombre,
			@RequestParam(value = "valor", required = false) Double cuentaValor,
			@RequestParam(value = "tipoNombre", required = false) String tipoNombre,
			@RequestParam(value = "tipoNaturaleza", required = false) String tipoNaturaleza,
			@RequestParam(value = "tipoCategoria", required = false) String tipoCategoria,
			@RequestParam(value = "fecha", required = false) String fecha,

			@PageableDefault(size = 20, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
		return cuentaServiceImpl.findByEmpresa(idEmpresa, cuentaCodigo, cuentaNombre, cuentaValor, tipoNombre,
				tipoNaturaleza, tipoCategoria, fecha, pageable);
	}

	@Operation(summary = "Eliminar cuenta por id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Rol eliminado correctamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "Rol no encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Error interno del servidor", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@DeleteMapping("/{id}")
	public ResponseEntity<ResponseDTO> deleteById(@PathVariable Integer id) {
		return cuentaServiceImpl.deleteById(id);
	}
	
	@Operation(summary = "Consultar cuentas por empresa, rango de fechas y paginado")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Consulta exitosa", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) })
	})
	@GetMapping("/getHistorico")
	public ResponseEntity<ResponseDTO> getCuentas(
			@RequestParam(name = "idEmpresa") Integer idEmpresa,
			@RequestParam(name = "fechaInicio", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaInicio,
			@RequestParam(name = "fechaFin", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaFin,
			@RequestParam(name = "page", defaultValue = "0") Integer page,
			@RequestParam(name = "size", defaultValue = "10") Integer size)
	 {
		return cuentaServiceImpl.findCuentas(idEmpresa, fechaInicio, fechaFin, page, size);
	}
}
