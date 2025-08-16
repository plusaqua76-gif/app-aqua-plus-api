package com.acua.plus.api.controller;

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

import com.acua.plus.api.service.impl.TipoDeudaServiceImpl;
import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.dtos.TipoDeudaDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/TipoDeuda")
@Tag(name = "TipoDeuda - Controller", description = "Controller encargado de gestionar las operaciones de los tipos de pago")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
public class TipoDeudaController {

private final TipoDeudaServiceImpl tipoDeudaServiceImpl;
	
	@Operation(summary = "Guardar o actualizar tipo de Deuda")
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
    public ResponseEntity<ResponseDTO> save(@RequestBody TipoDeudaDTO tipoDeudaDTO) {
        return tipoDeudaServiceImpl.save(tipoDeudaDTO);
    }

    @Operation(summary = "Buscar tipo Deuda por id")
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
        return tipoDeudaServiceImpl.findById(id);
    }

    @Operation(summary = "Listar todos los tipos de Deuda")
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
        return tipoDeudaServiceImpl.findAll();
    }

    @Operation(summary = "Eliminar tipos de Deuda por id")
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
        return tipoDeudaServiceImpl.deleteById(id);
    }
}

