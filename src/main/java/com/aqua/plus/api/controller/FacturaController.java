package com.aqua.plus.api.controller;

import java.util.Map;

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
import org.springframework.web.bind.annotation.RestController;

import com.aqua.plus.api.service.impl.FacturaServiceImpl;
import com.aqua.plus.commons.dtos.FacturaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
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
	                @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
	})
    @PostMapping
    public ResponseEntity<ResponseDTO> save(@RequestBody FacturaDTO facturaDTO) {
        return facturaServiceImpl.save(facturaDTO);
    }
	
	@Operation(summary = "Buscar Factura por id de Empresa")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Se ha guardado satisfactoriamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
    })
    @GetMapping("/empresa/{id}")
    public ResponseEntity<ResponseDTO> getByEmpresaId(@PathVariable Integer id) {
        return facturaServiceImpl.findByEnterpriseId(id);
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
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
    })
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
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
    })
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
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
    })
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
	                @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
	})
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
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
    })
    @PostMapping("/generar")
    public ResponseEntity<Map<String, Object>> generarFactura(
            @RequestBody Map<String, Object> jsonParams) {

        try {
            Map<String, Object> resultFromService = facturaServiceImpl.generarFactura(jsonParams);
            return ResponseEntity.ok(resultFromService);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Error en la operación del controlador: " + e.getMessage()));
        }
    }
}