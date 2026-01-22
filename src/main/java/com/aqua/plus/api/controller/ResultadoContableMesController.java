package com.aqua.plus.api.controller;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.aqua.plus.api.service.impl.ResultadoContableImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/api/v1/resultado-contable-mes")
@Tag(name = "Resultado Contable por Mes - Controller", description = "Controller encargado de gestionar los resultados contables por mes")
@CrossOrigin(origins = "*", methods = { RequestMethod.POST, RequestMethod.GET })
@RequiredArgsConstructor
public class ResultadoContableMesController {
    

    private final ResultadoContableImpl resultadoContableService;

    @Operation(summary = "Obtener métricas contables por empresa y mes")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Métricas obtenidas exitosamente", 
            content = { @Content(mediaType = "application/json") }),
        @ApiResponse(responseCode = "400", description = "Parámetros inválidos", 
            content = { @Content(mediaType = "application/json") }),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor", 
            content = { @Content(mediaType = "application/json") })
    })
    @GetMapping("/{idEmpresa}")
    public ResponseEntity<Map<String, Object>> obtenerMetricas(
            @PathVariable Integer idEmpresa,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Integer cantidadPeriodos) {
        try {
            Map<String, Object> resultado = resultadoContableService
                .obtenerResultadoContableMesMap(idEmpresa, anio, mes, fechaDesde, fechaHasta);
            
            if (resultado.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(resultado);
            }
            
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno del servidor"));
        }
    }
}