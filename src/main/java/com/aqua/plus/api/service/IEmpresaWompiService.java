package com.aqua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.EmpresaWompiDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IEmpresaWompiService {

    ResponseEntity<ResponseDTO> guardar(EmpresaWompiDTO dto);
    ResponseEntity<ResponseDTO> findByEmpresaId(Integer idEmpresa);
}
