package com.aqua.plus.api.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.TarifaClienteDTO;

public interface ITarifaClienteService {

	ResponseEntity<ResponseDTO> save(List<TarifaClienteDTO> dtos);

	/*ResponseEntity<ResponseDTO> findByEmpresaId(Integer empresaId);*/

	ResponseEntity<ResponseDTO> deleteById(Integer id);
}
