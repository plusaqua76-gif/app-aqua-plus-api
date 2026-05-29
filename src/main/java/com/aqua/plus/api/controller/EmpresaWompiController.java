package com.aqua.plus.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.aqua.plus.api.service.impl.EmpresaWompiServiceImpl;
import com.aqua.plus.commons.dtos.EmpresaWompiDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/empresa-wompi")
@Tag(name = "Empresa Wompi - Controller", description = "Gestión de credenciales Wompi por empresa")
@CrossOrigin(origins = "*", methods = { RequestMethod.GET, RequestMethod.POST })
@RequiredArgsConstructor
public class EmpresaWompiController {

    private final EmpresaWompiServiceImpl empresaWompiServiceImpl;

    @Operation(summary = "Guardar o actualizar credenciales Wompi de una empresa")
    @PostMapping
    public ResponseEntity<ResponseDTO> guardar(@Valid @RequestBody EmpresaWompiDTO dto) {
        return empresaWompiServiceImpl.guardar(dto);
    }

    @Operation(summary = "Consultar credenciales Wompi de una empresa por id de empresa")
    @GetMapping("/{idEmpresa}")
    public ResponseEntity<ResponseDTO> findByEmpresaId(@PathVariable Integer idEmpresa) {
        return empresaWompiServiceImpl.findByEmpresaId(idEmpresa);
    }
}
