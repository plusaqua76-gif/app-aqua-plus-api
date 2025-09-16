package com.aqua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IDocumentoService {

	ResponseEntity<ResponseDTO> listarPorEmpresaConBase64(Integer idEmpresa);
}
