package com.acua.plus.api.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.acua.plus.api.service.impl.LecturaServiceImpl;
import com.acua.plus.commons.dtos.LecturaDTO;
import com.acua.plus.commons.dtos.ResponseDTO;

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
 *          tipo (Lectura).
 */

@RestController
@RequestMapping("/api/v1/Lectura")
@Tag(name = "Lectura - Controller", description = "Controller encargado de gestionar las operaciones de las lecturas")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
public class LecturaController {

	private final LecturaServiceImpl lecturaServiceImpl;
	
	@Operation(summary = "Guardar o actualizar lectura")
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
    public ResponseEntity<ResponseDTO> save(@RequestBody LecturaDTO lecturaDTO) {
        return lecturaServiceImpl.save(lecturaDTO);
    }
	
	@Operation(summary = "Guardar Lectura SP")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación completada exitosamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis o validación", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
    })
    @PostMapping("/saveLecturaSp")
    public ResponseEntity<Map<String, Object>> generarFactura(
            @RequestBody Map<String, Object> jsonParams) {

        try {
            Map<String, Object> resultFromService = lecturaServiceImpl.guardarLectura(jsonParams);
            return ResponseEntity.ok(resultFromService);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Error en la operación del controlador: " + e.getMessage()));
        }
    }

    @Operation(summary = "Buscar lectura por id")
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
        return lecturaServiceImpl.findById(id);
    }

    @Operation(summary = "Listar todas las lecturas")
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
        return lecturaServiceImpl.findAll();
    }

    @Operation(summary = "Eliminar lectura por id")
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
        return lecturaServiceImpl.deleteById(id);
    }
}
