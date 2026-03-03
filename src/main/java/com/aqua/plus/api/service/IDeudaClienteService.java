package com.aqua.plus.api.service;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.DeudaClienteDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IDeudaClienteService {

	ResponseEntity<ResponseDTO> save(DeudaClienteDTO deudaClienteDTO);

	ResponseEntity<ResponseDTO> findById(Integer id);

	ResponseEntity<ResponseDTO> findByIdEnterprise(Integer idEmpresa, String clienteNombreLike,
			String facturaCodigoLike, String descripcionLike, LocalDate fechaDeuda, Double valor,
			String tipoDeudaNombre, Integer plazoPago, Pageable pageable);

	ResponseEntity<ResponseDTO> findAll();

	ResponseEntity<ResponseDTO> deleteById(Integer id);

	ResponseEntity<ResponseDTO> updateDeuda(DeudaClienteDTO deudaClienteDTO);

	ResponseEntity<ResponseDTO> findByEmpresaClienteContadorId(Integer eccId);

	ResponseEntity<ResponseDTO> findConsolidadoByEmpresaClienteContadorId(Integer eccId);
}
