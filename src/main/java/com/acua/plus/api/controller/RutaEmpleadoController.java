package com.acua.plus.api.controller;

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

import com.acua.plus.api.service.impl.RutaEmpleadoServiceImpl;
import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.dtos.RutaEmpleadoDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/RutaEmpleado")
@Tag(name = "RutaEmpleado - Controller", description = "Controller encargado de gestionar las operaciones de la Ruta Empleado")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
public class RutaEmpleadoController {
    
	private final RutaEmpleadoServiceImpl rutaEmpleadoServiceImpl;
	
	@Operation(summary = "Guardar  Ruta Empleado")
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
    public ResponseEntity<ResponseDTO> save(@RequestBody RutaEmpleadoDTO rutaEmpleadoDTO) {
        return rutaEmpleadoServiceImpl.save(rutaEmpleadoDTO);
    }

    @Operation(summary = "Buscar Ruta Empleado por id")
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
        return rutaEmpleadoServiceImpl.findById(id);
    }

    @Operation(summary = "Listar todos las Ruta Empleado")
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
        return rutaEmpleadoServiceImpl.findAll();
    }

    @Operation(summary = "Eliminar Ruta Empleado por id")
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
        return rutaEmpleadoServiceImpl.deleteById(id);
    }
    
    @Operation(summary = "actualizar Ruta Empleado")
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
    public ResponseEntity<ResponseDTO> update(@RequestBody RutaEmpleadoDTO rutaEmpleadoDTO) {
        return rutaEmpleadoServiceImpl.update(rutaEmpleadoDTO);
    }
    
    @Operation(summary = "Sincronizar datos del lector")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación completada exitosamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "403", description = "La persona no es un lector válido o no tiene permisos", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)) }),
    })
    @GetMapping("/sync/{idPersona}/{offset}/{limit}")
    public ResponseEntity<Map<String, Object>> sincronizarRuta(@PathVariable Integer idPersona, @PathVariable Integer offset, @PathVariable Integer limit) {
        try {
            Map<String, Object> resultFromService = rutaEmpleadoServiceImpl.syncLectorData(idPersona, offset, limit);

            String status = String.valueOf(resultFromService.getOrDefault("statusCode", "200"));
            HttpStatus httpStatus = switch (status) {
                case "403" -> HttpStatus.FORBIDDEN;
                case "500" -> HttpStatus.INTERNAL_SERVER_ERROR;
                default -> HttpStatus.OK;
            };

            return ResponseEntity.status(httpStatus).body(resultFromService);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Error en la operación del controlador",
                            "statusCode", "500",
                            "error", e.getMessage()
                    ));
        }
    }
}