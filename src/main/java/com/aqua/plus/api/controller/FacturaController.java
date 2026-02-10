package com.aqua.plus.api.controller;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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

import com.aqua.plus.api.service.impl.FacturaServiceImpl;
import com.aqua.plus.commons.dtos.FacturaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/factura")
@Tag(name = "Factura - Controller", description = "Controller encargado de gestionar las operaciones de la Factura")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
public class FacturaController {

	private final FacturaServiceImpl facturaServiceImpl;

	@Operation(summary = "Guardar Factura")
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
	public ResponseEntity<ResponseDTO> save(@RequestBody FacturaDTO facturaDTO) {
		return facturaServiceImpl.save(facturaDTO);
	}

	@Operation(summary = "Guardar Factura(s) con lecturas anidadas")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operación completada exitosamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida...", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada...", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))) })
	@PostMapping("/registrar")
	public ResponseEntity<Map<String, Object>> registrarFacturas(@RequestBody JsonNode body) {
		return facturaServiceImpl.guardarFacturas(body);
	}

	@Operation(summary = "Buscar facturas por id de Empresa (con filtros y paginación)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Consulta exitosa", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "400", description = "Petición inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "404", description = "No se encontraron datos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error inesperado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@GetMapping("/empresa/{id}")
	public ResponseEntity<ResponseDTO> getByEmpresaId(@PathVariable("id") Integer idEmpresa,
			@RequestParam(required = false) String codigo,
			@RequestParam(required = false, name = "clienteNombreCompleto") String clienteNombreCompleto,
			@RequestParam(required = false) String fechaEmision, @RequestParam(required = false) String fechaFin,
			@RequestParam(required = false) String estadoNombre, @RequestParam(required = false) Boolean consumoAnormal,
			@RequestParam(required = false) Integer consumo, @RequestParam(required = false) Double precioMin,
			@RequestParam(required = false) Double precioMax, Pageable pageable) {
		return facturaServiceImpl.findByEnterpriseId(idEmpresa, codigo, clienteNombreCompleto, fechaEmision, fechaFin,
				estadoNombre, consumoAnormal, consumo, precioMin, precioMax, pageable);
	}

	@Operation(summary = "Listar todas las facturas de una persona (con filtros y paginación)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Consulta exitosa", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "400", description = "Petición inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "404", description = "No se encontraron datos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error inesperado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@GetMapping("/persona/{personaId}")
	public ResponseEntity<ResponseDTO> getFacturasByPersona(@PathVariable("personaId") Integer idPersona,
			@RequestParam(required = false) String codigo, @RequestParam(required = false) String fechaEmision,
			@RequestParam(required = false) String fechaFin, @RequestParam(required = false) String estadoNombre,
			@RequestParam(required = false) Boolean consumoAnormal, @RequestParam(required = false) Double precio,
			Pageable pageable) {
		return facturaServiceImpl.findFacturasByPersona(idPersona, codigo, fechaEmision, fechaFin, estadoNombre,
				consumoAnormal, precio, pageable);
	}

	@Operation(summary = "Buscar Factura por id")
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
		return facturaServiceImpl.findById(id);
	}

	@Operation(summary = "Listar todas las Factura")
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
		return facturaServiceImpl.findAll();
	}

	@Operation(summary = "Eliminar Factura por id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Rol eliminado correctamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "Rol no encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Error interno del servidor", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@DeleteMapping("/{id}")
	public ResponseEntity<ResponseDTO> deleteById(@PathVariable Integer id) {
		return facturaServiceImpl.deleteById(id);
	}

	@Operation(summary = "actualizar Factura")
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
	@PutMapping
	public ResponseEntity<ResponseDTO> update(@RequestBody FacturaDTO facturaDTO) {
		return facturaServiceImpl.update(facturaDTO);
	}

	@Operation(summary = "Generar Factura")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operación completada exitosamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)) }),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis o validación", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@PostMapping("/generar")
	public ResponseEntity<Map<String, Object>> generarFactura(@RequestBody Map<String, Object> jsonParams) {

		try {
			Map<String, Object> resultFromService = facturaServiceImpl.generarFactura(jsonParams);
			return ResponseEntity.ok(resultFromService);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Error en la operación del controlador: " + e.getMessage()));
		}
	}

	@Operation(summary = "Métricas de consumo por mes", description = "Invoca la función public.fn_metricas_consumo_mes para obtener m³ e importes del periodo.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
			@ApiResponse(responseCode = "400", description = "Solicitud inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
			@ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
			@ApiResponse(responseCode = "422", description = "Entidad no procesable", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
			@ApiResponse(responseCode = "500", description = "Error interno", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))) })
	@GetMapping("/consumo-clientes")
	public ResponseEntity<Map<String, Object>> metricasConsumoMes(@RequestParam Integer empresaId,
			@RequestParam Integer anio, Integer mes) {
		return facturaServiceImpl.metricasConsumoMes(empresaId, anio, mes);
	}

	@Operation(summary = "Métricas de consumo por mes", description = "Invoca la función public.fn_metricas_consumo_mes_empresa para obtener m³ Contador Padre Empresa e importes del periodo.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
			@ApiResponse(responseCode = "400", description = "Solicitud inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
			@ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
			@ApiResponse(responseCode = "422", description = "Entidad no procesable", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))),
			@ApiResponse(responseCode = "500", description = "Error interno", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class))) })
	@GetMapping("/consumo-empresas")
	public ResponseEntity<Map<String, Object>> metricasConsumoMesEmpresa(@RequestParam Integer empresaId,
			@RequestParam Integer anio, Integer mes) {
		return facturaServiceImpl.metricasConsumoMesEmpresa(empresaId, anio, mes);
	}

	@Operation(summary = "Métricas de factura por mes", description = "Invoca el SP public.fn_metricas_facturas_mes para obtener m³ totales e importes del mes.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operación completada exitosamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))) })
	@GetMapping("/factura-mes")
	public ResponseEntity<Map<String, Object>> metricasFacturasMes(
			@Parameter(description = "ID de la empresa", required = true, example = "14") @RequestParam Integer empresaId,
			@Parameter(description = "Año", required = true, example = "2025") @RequestParam Integer anio,
			@Parameter(description = "Mes (1..12)", required = true, example = "9") Integer mes) {
		Map<String, Object> resp = facturaServiceImpl.metricasFacturaMes(empresaId, anio, mes);
		return ResponseEntity.ok(resp);
	}

	@Operation(summary = "Actualizar facturas por código (batch o individual)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operación completada exitosamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
			@ApiResponse(responseCode = "400", description = "Petición inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error inesperado en el servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@PostMapping("/actualizar")
	public ResponseEntity<Map<String, Object>> actualizarFacturas(@RequestBody Object body) {
		try {
			Map<String, Object> result = facturaServiceImpl.actualizarFacturas(body);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Error en la operación del controlador: " + e.getMessage()));
		}
	}

	@Operation(summary = "Sugerir facturas por código")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Consulta realizada correctamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "204", description = "No se encontraron resultados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "400", description = "La petición contiene errores de sintaxis", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error inesperado en el servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@GetMapping("/sugerencias")
	public ResponseEntity<ResponseDTO> getSugerencias(@RequestParam String term) {
		return facturaServiceImpl.sugerirCodigos(term);
	}

	@Operation(summary = "Buscar Factura por id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Se ha guardado satisfactoriamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@GetMapping("/consultar/{id}")
	public ResponseEntity<ResponseDTO> getFacturaById(@PathVariable Integer id) {
		return facturaServiceImpl.obtenerFacturaDetalle(id);
	}
	
	
	@Operation(summary = "consultar metricas de agua factura")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Consulta realizada correctamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "204", description = "No se encontraron resultados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "400", description = "La petición contiene errores de sintaxis", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error inesperado en el servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@GetMapping("/agua-facturada")
	public ResponseEntity<?> consultarAguaFacturada(
	        @RequestParam Integer idEmpresa,
	        @RequestParam Integer anio,
	        @RequestParam(required = false) Integer mes
	) {
	    return ResponseEntity.ok(
	    		facturaServiceImpl.consultarAguaFacturadaMesEmpresa(idEmpresa, anio, mes)
	    );
	}


	@Operation(summary = "Obtener métricas de cartera por antigüedad", 
			   description = "Obtiene las métricas de cartera agrupadas por rangos de antigüedad (0-30, 31-60, 61-90, 90+ días)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Métricas obtenidas exitosamente", 
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "400", description = "Parámetros inválidos", 
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error interno del servidor", 
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@GetMapping("/cartera/antiguedad/{empresaId}")
	public ResponseEntity<ResponseDTO> obtenerMetricasCarteraPorAntiguedad(
			@Parameter(description = "ID de la empresa", required = true)
			@PathVariable Integer empresaId) {
		return facturaServiceImpl.obtenerMetricasCarteraPorAntiguedad(empresaId);
	}

	@Operation(summary = "Obtener métricas financieras consolidadas por período", 
			   description = "Obtiene indicadores financieros calculados de una empresa para un mes y año específicos")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Métricas obtenidas exitosamente", 
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "400", description = "Parámetros inválidos", 
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "404", description = "No se pudieron calcular las métricas", 
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error interno del servidor", 
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@GetMapping("/metricas-financieras/{empresaId}")
	public ResponseEntity<ResponseDTO> obtenerMetricasFinancieras(
			@Parameter(description = "ID de la empresa", required = true)
			@PathVariable Integer empresaId,
			@Parameter(description = "Mes del período (1-12)", required = true, example = "2")
			@RequestParam Integer mes,
			@Parameter(description = "Año del período", required = true, example = "2026")
			@RequestParam Integer anio) {
		return facturaServiceImpl.obtenerMetricasFinancieras(empresaId, mes, anio);
	}

}