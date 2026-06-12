package com.aqua.plus.api.controller;

import com.aqua.plus.api.helpers.PagoHelper;
import com.aqua.plus.api.service.external.IPagoService;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.CrearTransaccionRequest;
import com.aqua.plus.commons.dtos.external.IniciarPagoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pagos")
@Tag(name = "Pago - Controller", description = "Gestión de pagos Wompi")
@CrossOrigin(origins = "*", methods = { RequestMethod.GET, RequestMethod.POST })
@RequiredArgsConstructor
public class PagoController {

    private final IPagoService pagoServiceImpl;
    private final PagoHelper      pagoHelper;

    @Operation(summary = "Obtener instituciones financieras PSE")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Se ha consultado satisfactoriamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
    @GetMapping("/pse/bancos")
    public ResponseEntity<ResponseDTO> obtenerBancosPse(
            @RequestParam(value = "idEmpresa", required = false) Integer idEmpresa) {
        return this.pagoServiceImpl.obtenerBancosPse(idEmpresa);
    }

    @Operation(summary = "Obtener información del merchant Wompi")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Se ha consultado satisfactoriamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
    @GetMapping("/merchant")
    public ResponseEntity<ResponseDTO> obtenerMerchant(
            @RequestParam(value = "idEmpresa", required = false) Integer idEmpresa) {
        return this.pagoServiceImpl.obtenerMerchant(idEmpresa);
    }

    /**
     * PASO 1 — Frontend solicita iniciar el proceso de pago.
     * Retorna: referencia, acceptance_token, firma y clave pública.
     * La IP del cliente se registra para auditoría antifraude.
     */
    @Operation(summary = "Iniciar pago (PASO 1) — genera referencia y datos del widget Wompi")
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
    @PostMapping("/iniciar")
    public ResponseEntity<ResponseDTO> iniciar(
            @Valid @RequestBody IniciarPagoRequest req,
            HttpServletRequest httpRequest) {
        return this.pagoServiceImpl.iniciarPago(req, pagoHelper.resolverIpCliente(httpRequest));
    }

    /**
     * PASO 2 — Frontend envía el medio de pago elegido con sus datos.
     * Retorna: id Wompi, estado PENDING, redirect_url segura (si aplica).
     * La IP del cliente se inyecta en el request para cifrado JWE PSE.
     * deviceId y sessionId se reciben en headers X-Device-Id / X-Session-Id.
     */
    @Operation(summary = "Crear transacción en Wompi (PASO 2)")
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
    @PostMapping("/transaccion")
    public ResponseEntity<ResponseDTO> crearTransaccion(
            @Valid @RequestBody CrearTransaccionRequest req,
            @RequestHeader(value = "X-Device-Id",  required = false) String deviceId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            HttpServletRequest httpRequest) {
        req.setIpCliente(pagoHelper.resolverIpCliente(httpRequest));
        req.setDeviceId(deviceId);
        req.setSessionId(sessionId);
        return this.pagoServiceImpl.crearTransaccion(req);
    }

    /**
     * Consultar estado actual de un pago por referencia (polling del frontend).
     * Valida que el pago pertenezca al usuario autenticado.
     * Si el pago sigue PENDING y ya tiene id Wompi, sincroniza el estado real.
     */
    @Operation(summary = "Consultar estado de un pago (polling frontend)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Se ha consultado satisfactoriamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
    @GetMapping("/{referencia}")
    public ResponseEntity<ResponseDTO> consultar(@PathVariable String referencia) {
        return this.pagoServiceImpl.consultarYSincronizar(referencia);
    }

    @Operation(summary = "Sincronizar estado de un pago con Wompi")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Se ha sincronizado satisfactoriamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
    @PostMapping("/sincronizar/{referencia}")
    public ResponseEntity<ResponseDTO> sincronizar(@PathVariable String referencia) {
        return this.pagoServiceImpl.sincronizarEstado(referencia);
    }

    /**
     * REDIRECT SEGURO — Valida usuario, device_id y session_id antes de entregar
     * la URL real de Wompi (PSE / Bancolombia). La URL solo se entrega UNA VEZ
     * (one-time use) y solo al usuario y dispositivo que crearon la transacción.
     */
    @Operation(summary = "Obtener URL de redirección segura PSE/Bancolombia (one-time)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Se ha consultado satisfactoriamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
    @PostMapping("/redirigir/{referencia}")
    public ResponseEntity<ResponseDTO> redirigir(
            @PathVariable String referencia,
            @RequestHeader(value = "X-Device-Id",  required = false) String deviceId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            HttpServletRequest httpRequest) {
        return this.pagoServiceImpl.obtenerUrlRedireccion(
            referencia, deviceId, sessionId, pagoHelper.resolverIpCliente(httpRequest));
    }
}
