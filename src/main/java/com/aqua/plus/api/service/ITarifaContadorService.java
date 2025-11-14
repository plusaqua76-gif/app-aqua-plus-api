package com.aqua.plus.api.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.TarifaContadorDTO;

public interface ITarifaContadorService {

	ResponseEntity<ResponseDTO> save(List<TarifaContadorDTO> dtos);

	/*ResponseEntity<ResponseDTO> findByEmpresaId(Integer empresaId);*/

	ResponseEntity<ResponseDTO> deleteById(Integer id);
}
