package com.aqua.plus.api.controller;

import com.aqua.plus.api.helpers.PagoHelper;
import com.aqua.plus.api.service.impl.external.PagoServiceImpl;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.CrearTransaccionRequest;
import com.aqua.plus.commons.dtos.external.IniciarPagoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/pagos")
@Tag(name = "Pago - Controller", description = "Gestión de pagos Wompi")
@CrossOrigin(origins = "*", methods = { RequestMethod.GET, RequestMethod.POST })
@RequiredArgsConstructor
public class PagoController {

    private final PagoServiceImpl   pagoService;
    private final PagoHelper        pagoHelper;

    @Operation(summary = "Obtener instituciones financieras PSE")
    @GetMapping("/pse/bancos")
    public ResponseEntity<ResponseDTO> obtenerBancosPse(
            @RequestParam(value = "idEmpresa", required = false) Integer idEmpresa) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(ResponseDTO.builder()
            .success(true)
            .code(HttpStatus.OK.value())
            .response(pagoService.obtenerBancosPse(idEmpresa, usuarioActual))
            .build());
    }

    @Operation(summary = "Obtener información del merchant Wompi")
    @GetMapping("/merchant")
    public ResponseEntity<ResponseDTO> obtenerMerchant(
            @RequestParam(value = "idEmpresa", required = false) Integer idEmpresa) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(ResponseDTO.builder()
            .success(true)
            .code(HttpStatus.OK.value())
            .response(pagoService.obtenerMerchant(idEmpresa, usuarioActual))
            .build());
    }

    /**
     * PASO 1 — Frontend solicita iniciar el proceso de pago.
     * Retorna: referencia, acceptance_token, firma y clave pública.
     * La IP del cliente se registra para auditoría antifraude.
     */
    @Operation(summary = "Iniciar pago (PASO 1) — genera referencia y datos del widget Wompi")
    @PostMapping("/iniciar")
    public ResponseEntity<ResponseDTO> iniciar(
            @Valid @RequestBody IniciarPagoRequest req,
            HttpServletRequest httpRequest) {

        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        String ipAddress = pagoHelper.resolverIpCliente(httpRequest);
        return ResponseEntity.ok(ResponseDTO.builder()
            .success(true)
            .code(HttpStatus.OK.value())
            .response(pagoService.iniciarPago(req, usuarioActual, ipAddress))
            .build());
    }

    /**
     * PASO 2 — Frontend envía el medio de pago elegido con sus datos.
     * Retorna: id Wompi, estado PENDING, redirect_url segura (si aplica).
     * La IP del cliente se inyecta en el request para cifrado JWE PSE.
     * deviceId y sessionId se reciben en headers X-Device-Id / X-Session-Id.
     */
    @Operation(summary = "Crear transacción en Wompi (PASO 2)")
    @PostMapping("/transaccion")
    public ResponseEntity<ResponseDTO> crearTransaccion(
            @Valid @RequestBody CrearTransaccionRequest req,
            @RequestHeader(value = "X-Device-Id",  required = false) String deviceId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            HttpServletRequest httpRequest) {

        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        req.setIpCliente(pagoHelper.resolverIpCliente(httpRequest));
        req.setDeviceId(deviceId);
        req.setSessionId(sessionId);
        return ResponseEntity.ok(ResponseDTO.builder()
            .success(true)
            .code(HttpStatus.OK.value())
            .response(pagoService.crearTransaccion(req, usuarioActual))
            .build());
    }

    /**
     * Consultar estado actual de un pago por referencia (polling del frontend).
     * Valida que el pago pertenezca al usuario autenticado.
     * Si el pago sigue PENDING y ya tiene id Wompi, sincroniza el estado real.
     */
    @Operation(summary = "Consultar estado de un pago (polling frontend)")
    @GetMapping("/{referencia}")
    public ResponseEntity<ResponseDTO> consultar(@PathVariable String referencia) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(ResponseDTO.builder()
            .success(true)
            .code(HttpStatus.OK.value())
            .response(pagoService.consultarYSincronizar(referencia, usuarioActual))
            .build());
    }

    @Operation(summary = "Sincronizar estado de un pago con Wompi")
    @PostMapping("/sincronizar/{referencia}")
    public ResponseEntity<ResponseDTO> sincronizar(@PathVariable String referencia) {
        return ResponseEntity.ok(ResponseDTO.builder()
            .success(true)
            .code(HttpStatus.OK.value())
            .response(pagoService.sincronizarEstado(referencia))
            .build());
    }

    /**
     * REDIRECT SEGURO — Valida usuario, device_id y session_id antes de entregar
     * la URL real de Wompi (PSE / Bancolombia). La URL solo se entrega UNA VEZ
     * (one-time use) y solo al usuario y dispositivo que crearon la transacción.
     */
    @Operation(summary = "Obtener URL de redirección segura PSE/Bancolombia (one-time)")
    @PostMapping("/redirigir/{referencia}")
    public ResponseEntity<ResponseDTO> redirigir(
            @PathVariable String referencia,
            @RequestHeader(value = "X-Device-Id",  required = false) String deviceId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            HttpServletRequest httpRequest) {

        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        String ipCliente = pagoHelper.resolverIpCliente(httpRequest);
        String url = pagoService.obtenerUrlRedireccion(referencia, usuarioActual, deviceId, sessionId, ipCliente);
        return ResponseEntity.ok(ResponseDTO.builder()
            .success(true)
            .code(HttpStatus.OK.value())
            .response(Map.of("url", url))
            .build());
    }
}
