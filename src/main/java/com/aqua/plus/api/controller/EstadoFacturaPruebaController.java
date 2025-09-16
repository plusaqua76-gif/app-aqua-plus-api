package com.aqua.plus.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aqua.plus.api.jobs.FacturaVencimientoScheduler;
import com.aqua.plus.commons.dtos.ResponseDTO;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/estado-factura")
@Tag(name = "EstadoFactura - Controller", description = "Controller encargado de gestionar las operaciones de los estados de facturas")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
public class EstadoFacturaPruebaController {

	private final FacturaVencimientoScheduler facturaVencimientoService;

	@PostMapping("/jobs/facturas/vencidas/run")
	public ResponseEntity<ResponseDTO> runNow(@RequestParam(defaultValue = "system-cron") String usuario) {
		facturaVencimientoService.marcarPendientesComoVencidas(usuario);
		return ResponseEntity.ok(ResponseDTO.builder().success(true).message("Job ejecutado").code(200).build());
	}

}
