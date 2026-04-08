package com.aqua.plus.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.aqua.plus.api.service.IFcmTokenService;
import com.aqua.plus.commons.dtos.FcmTokenRequest;
import com.aqua.plus.commons.dtos.ResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "FCM Token - Controller", description = "Controller encargado de gestionar los tokens FCM")
@CrossOrigin(origins = "*", methods = { RequestMethod.POST })
@RequiredArgsConstructor
public class FcmTokenController {

    private final IFcmTokenService fcmTokenService;

    @Operation(summary = "Registrar o actualizar token FCM")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Token guardado exitosamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "200", description = "Token actualizado exitosamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) })
    })
    @PostMapping("/save-token")
    public ResponseEntity<ResponseDTO> guardarToken(@RequestBody FcmTokenRequest request) {
        return fcmTokenService.guardarToken(
                request.getUsuarioId(),
                request.getToken(),
                request.getDispositivo(),
                request.getUsuarioCreacion());
    }
}
