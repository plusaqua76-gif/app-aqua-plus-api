package com.aqua.plus.api.service;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.AbonoDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IAbonoService {

	ResponseEntity<ResponseDTO> save(AbonoDTO abonoDTO);

	ResponseEntity<ResponseDTO> update(AbonoDTO abonoDTO);

	ResponseEntity<ResponseDTO> findById(Integer id);

	ResponseEntity<ResponseDTO> listarAbonosPorIdEmpresa(Integer idEmpresa, String clienteLike,
			String codigoFacturaLike, LocalDate fecha, Double valor, Pageable pageable);

	ResponseEntity<ResponseDTO> findAll();

	ResponseEntity<ResponseDTO> deleteById(Integer id);
}
