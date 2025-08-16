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

import com.acua.plus.api.service.impl.EmpleadoEmpresaServiceImpl;
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
 *          tipo (Direccion).
 */

@RestController
@RequestMapping("/api/v1/EmpleadoEmpresa")
@Tag(name = "EmpleadoEmpresa - Controller", description = "Controller encargado de gestionar las operaciones de los empleado empresa")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
                RequestMethod.PUT })
@RequiredArgsConstructor
public class EmpleadoEmpresaController {

        private final EmpleadoEmpresaServiceImpl empleadoEmpresaServiceImpl;

        @Operation(summary = "Guardar empleado empresa")
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
        @PostMapping("/save")
        public ResponseEntity<Map<String, Object>> guardarEmpleado(@RequestBody Map<String, Object> jsonParams) {
                try {
                        Map<String, Object> resultado = empleadoEmpresaServiceImpl.save(jsonParams);

                        if (resultado.containsKey("error")) {
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resultado);
                        }

                        return ResponseEntity.ok(resultado);
                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("error", "Error interno del servidor"));
                }
        }

        @Operation(summary = "Actualizar estado activo/inactivo de persona y sus relaciones")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente", content = {
                                        @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
                        @ApiResponse(responseCode = "404", description = "La persona no fue encontrada", content = {
                                        @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
                        @ApiResponse(responseCode = "400", description = "Error de solicitud", content = {
                                        @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
                        @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = {
                                        @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
        })
        @PostMapping("/estado")
        public ResponseEntity<Map<String, Object>> actualizarEstadoEmpleado(
                        @RequestBody Map<String, Object> jsonParams) {
                try {
                        Map<String, Object> resultado = empleadoEmpresaServiceImpl.actualizarEstadoPersona(jsonParams);

                        if (resultado.containsKey("statusCode")) {
                                int status = (int) resultado.get("statusCode");

                                return switch (status) {
                                        case 404 -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(resultado);
                                        case 500 ->
                                                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resultado);
                                        default -> ResponseEntity.ok(resultado);
                                };
                        }

                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("error", "Respuesta inesperada del servidor"));

                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("error", "Error interno del servidor"));
                }
        }

        @Operation(summary = "Actualizar empleado empresa")
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

        @PutMapping("/update")
        public ResponseEntity<Map<String, Object>> actualizarEmpleado(
                        @RequestBody Map<String, Object> jsonParams) {
                try {
                        Map<String, Object> resultado = empleadoEmpresaServiceImpl.update(jsonParams);
                        if (resultado.containsKey("error")) {
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resultado);
                        }
                        return ResponseEntity.ok(resultado);
                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("error", "Error interno del servidor"));
                }
        }

        @Operation(summary = "Buscar empleado empresa por id")
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
                return empleadoEmpresaServiceImpl.findById(id);
        }

        @Operation(summary = "Listar todos los empleados empresa")
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
                return empleadoEmpresaServiceImpl.findAll();
        }

        @Operation(summary = "Eliminar empelado empresa por id")
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
                return empleadoEmpresaServiceImpl.deleteById(id);
        }
}
