package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IHistoricoCuentaService;
import com.aqua.plus.commons.dtos.CuentaDTO;
import com.aqua.plus.commons.dtos.EmpresaDTO;
import com.aqua.plus.commons.dtos.HistoricoCuentaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.TipoCuentaContableDTO;
import com.aqua.plus.commons.entities.HistoricoCuentaEntity;
import com.aqua.plus.commons.repositories.HistoricoCuentaRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoricoCuentaServiceImpl implements IHistoricoCuentaService {

	private final HistoricoCuentaRepository historicoCuentaRepository;

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findHistoricoCuenta(Integer idEmpresa, Date fechaInicio, Date fechaFin,
			Integer page, Integer size) {
		log.info("Consultar histórico cuenta: idEmpresa={}, fechaInicio={}, fechaFin={}, page={}, size={}", idEmpresa,
				fechaInicio, fechaFin, page, size);

		try {
			if (idEmpresa == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("Parámetro requerido: idEmpresa").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			int pageNumber = (page == null || page < 0) ? 0 : page;
			int pageSize = (size == null || size <= 0) ? 10 : size;
			Pageable pageable = PageRequest.of(pageNumber, pageSize);

			Page<HistoricoCuentaEntity> resultPage;

			if (fechaInicio != null && fechaFin != null) {
				if (fechaInicio.after(fechaFin)) {
					return ResponseEntity.badRequest()
							.body(ResponseDTO.builder().success(false)
									.message("fechaInicio no puede ser mayor que fechaFin")
									.code(HttpStatus.BAD_REQUEST.value()).build());
				}

				resultPage = historicoCuentaRepository.findByEmpresa_IdAndFechaCreacionBetweenOrderByFechaCreacionDesc(
						idEmpresa, fechaInicio, fechaFin, pageable);

			} else {
				resultPage = historicoCuentaRepository.findByEmpresa_IdOrderByFechaCreacionDesc(idEmpresa, pageable);
			}

			if (resultPage == null || resultPage.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron registros de histórico para los filtros indicados")
								.code(HttpStatus.NOT_FOUND.value()).totalCount(0L).response(List.of()).build());
			}

			List<HistoricoCuentaDTO> dtos = resultPage.getContent().stream().map(h -> {

				CuentaDTO cuentaDTO = null;
				if (h.getCuenta() != null) {
					var c = h.getCuenta();
					cuentaDTO = CuentaDTO.builder().id(c.getId()).codigo(c.getCodigo()).nombre(c.getNombre()).build();
				}

				EmpresaDTO empresaDTO = null;
				if (h.getEmpresa() != null) {
					var e = h.getEmpresa();
					empresaDTO = EmpresaDTO.builder().id(e.getId()).nombre(e.getNombre()).nit(e.getNit())
							.codigo(e.getCodigo()).build();
				}

				TipoCuentaContableDTO tipoCuentaDTO = null;
				if (h.getTipoCuenta() != null) {
					var t = h.getTipoCuenta();
					tipoCuentaDTO = TipoCuentaContableDTO.builder().id(t.getId()).nombre(t.getNombre())
							.descripcion(t.getDescripcion()).build();
				}

				return HistoricoCuentaDTO.builder().id(h.getId()).cuenta(cuentaDTO).empresa(empresaDTO)
						.tipoCuenta(tipoCuentaDTO).codigo(h.getCodigo()).nombre(h.getNombre()).activo(h.getActivo())
						.usuarioCreacion(h.getUsuarioCreacion()).fechaCreacion(h.getFechaCreacion())
						.usuarioModificacion(h.getUsuarioModificacion()).fechaModificacion(h.getFechaModificacion())
						.build();

			}).toList();

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtos).totalCount(resultPage.getTotalElements()).build());

		} catch (Exception e) {
			log.error("Error consultando histórico cuenta", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}
}