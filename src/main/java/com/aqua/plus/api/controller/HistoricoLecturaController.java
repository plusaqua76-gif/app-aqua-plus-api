package com.aqua.plus.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.aqua.plus.api.service.impl.HistoricoLecturaServiceImpl;
import com.aqua.plus.commons.dtos.ResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * @author nicope
 * @version 1.0
 * 
 *          Controlador que expone los servicios para trabajar con objeto(s) de
 *          tipo (HistoricoLectura).
 */

@RestController
@RequestMapping("/api/v1/historico-lectura")
@Tag(name = "HistoricoLectura - Controller", description = "Controller encargado de gestionar las operaciones de historicos lecturas")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
public class HistoricoLecturaController {

	private final HistoricoLecturaServiceImpl historicoLecturaServiceImpl;
	
	@Operation(summary = "Consultar histórico por id de lectura")
	@ApiResponses({
	    @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
	    @ApiResponse(responseCode = "400", description = "Petición inválida"),
	    @ApiResponse(responseCode = "404", description = "No se encontraron datos"),
	    @ApiResponse(responseCode = "500", description = "Error inesperado")
	})
	@GetMapping("/lecturas/{idLectura}")
	public ResponseEntity<ResponseDTO> getHistoricoByLectura(@PathVariable Integer idLectura) {
	    return historicoLecturaServiceImpl.findHistoricoByLecturaId(idLectura);
	}

}
