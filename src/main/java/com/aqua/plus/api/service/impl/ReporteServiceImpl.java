package com.aqua.plus.api.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IReporteService;
import com.aqua.plus.api.service.impl.specification.ReporteSpecifications;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ReporteEntity;
import com.aqua.plus.commons.maps.ReporteMapper;
import com.aqua.plus.commons.repositories.ReporteRepository;
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
public class ReporteServiceImpl implements IReporteService {

	private final ReporteRepository reporteRepository;
	private final ReporteMapper reporteMapper;

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll(String nombre, Pageable pageable) {
		log.info("Listar reportes (activo=true) con filtro nombre={}, paginación={}", nombre, pageable);

		try {
			var spec = ReporteSpecifications.allOfNonNull(ReporteSpecifications.activoTrue(),
					ReporteSpecifications.nombreLike(nombre));

			Pageable pageToUse = (pageable != null ? pageable : Pageable.unpaged());

			Page<ReporteEntity> page = reporteRepository.findAll((root, cq, cb) -> {
				cq.distinct(true);
				return (spec == null) ? cb.conjunction() : spec.toPredicate(root, cq, cb);
			}, pageToUse);

			var entities = page.getContent();
			var dtos = reporteMapper.listEntityToDtoList(entities);

			long totalCount = page.getTotalElements();
			int pageSize = page.getSize();
			int currentPage = page.getNumber();
			int totalPages = page.getTotalPages();

			if (entities.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false).message("No se encontraron reportes")
								.code(HttpStatus.NOT_FOUND.value()).response(List.of()).totalCount(totalCount)
								.pageSize(pageSize).currentPage(currentPage).totalPages(totalPages).build());
			}

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtos).totalCount(totalCount).pageSize(pageSize)
					.currentPage(currentPage).totalPages(totalPages).build());

		} catch (Exception e) {
			log.error("Error al listar reportes con filtro nombre={}", nombre, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

}
