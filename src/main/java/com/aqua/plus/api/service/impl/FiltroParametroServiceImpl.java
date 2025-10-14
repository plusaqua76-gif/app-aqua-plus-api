package com.aqua.plus.api.service.impl;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IFiltroParametroService;
import com.aqua.plus.commons.dtos.FiltroParametroDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.FiltroParametroEntity;
import com.aqua.plus.commons.maps.FiltroParametroMapper;
import com.aqua.plus.commons.repositories.FiltroParametroRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author nicope
 * @version 1.0
 * 
 *          Clase que implementa la interfaz de la lógica de negocio.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class FiltroParametroServiceImpl implements IFiltroParametroService {

	private final FiltroParametroRepository filtroParametroRepository;
	private final FiltroParametroMapper filtroParametroMapper;

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByReporteId(Integer idReporte) {
		log.info("Listar filtros de reporte por idReporte={}, solo activos", idReporte);

		if (idReporte == null) {
			return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
					.message("El parámetro idReporte es requerido.").code(HttpStatus.BAD_REQUEST.value()).build());
		}

		try {
			List<FiltroParametroEntity> asociaciones = filtroParametroRepository
					.findByReporteFiltro_IdAndActivoTrue(idReporte);

			List<FiltroParametroDTO> items = filtroParametroMapper.listEntityToDtoList(asociaciones);

			if (items.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron filtros activos para el reporte indicado.")
								.code(HttpStatus.NOT_FOUND.value()).response(List.of()).build());
			}

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(items).build());

		} catch (Exception e) {
			log.error("Error al listar filtros por idReporte={}", idReporte, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message(Constantes.CONSULTING_ERROR).code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}
}
