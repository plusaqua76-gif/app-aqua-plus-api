package com.aqua.plus.api.controller;

import com.aqua.plus.api.service.wompi.CheckoutPagoService;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.CheckoutPagoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pagos")
@Tag(name = "Pago - Controller", description = "Web Checkout Wompi")
@CrossOrigin(origins = "*", methods = { RequestMethod.GET, RequestMethod.POST })
@RequiredArgsConstructor
public class PagoController {

    private final CheckoutPagoService checkoutPagoService;

    @Operation(summary = "Crear checkout Web Checkout Wompi a partir de una factura")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Checkout creado", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "Petición inválida", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Error interno", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
    @PostMapping("/checkout")
    public ResponseEntity<ResponseDTO> checkout(@Valid @RequestBody CheckoutPagoRequest request) {
        return checkoutPagoService.crearCheckout(request);
    }

    @Operation(summary = "Consultar estado de pago de una factura (solo BD, sin llamar a Wompi)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consulta exitosa", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Error interno", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
    @GetMapping("/{facturaId}/estado")
    public ResponseEntity<ResponseDTO> estado(@PathVariable Integer facturaId) {
        return checkoutPagoService.consultarEstado(facturaId);
    }
}
