package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.ICuentaService;
import com.aqua.plus.api.service.impl.specification.CuentaSpecifications;
import com.aqua.plus.commons.dtos.CuentaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.CuentaEntity;
import com.aqua.plus.commons.maps.CuentaMapper;
import com.aqua.plus.commons.repositories.CuentaRepository;
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
public class CuentaServiceImpl implements ICuentaService {

	private final CuentaRepository cuentaRepository;
	private final CuentaMapper cuentaMapper;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(CuentaDTO cuentaDTO) {
		log.info("Guardar/Actualizar Cuenta ");
		try {
			boolean isUpdate = cuentaDTO.getId() != null && cuentaRepository.existsById(cuentaDTO.getId());
			CuentaEntity entity;

			if (isUpdate) {
				entity = cuentaRepository.findById(cuentaDTO.getId()).orElseThrow();
				cuentaMapper.updateEntityFromDto(cuentaDTO, entity);
				entity.setFechaModificacion(new Date());
				entity.setUsuarioModificacion(cuentaDTO.getUsuarioModificacion());
			} else {
				entity = cuentaMapper.dtoToEntity(cuentaDTO);
				entity.setFechaCreacion(new Date());
				entity.setUsuarioCreacion(cuentaDTO.getUsuarioCreacion());
				entity.setActivo(true);
			}

			CuentaEntity saved = cuentaRepository.save(entity);
			CuentaDTO savedDTO = cuentaMapper.entityToDto(saved);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(message).code(statusCode)
					.response(savedDTO).build();

			return ResponseEntity.status(statusCode).body(responseDTO);

		} catch (Exception e) {
			log.error("Error guardando la cuenta", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
		log.info("Buscar cuenta por id: {}", id);
		try {
			Optional<CuentaEntity> cuenta = cuentaRepository.findById(id);
			if (cuenta.isPresent()) {
				CuentaDTO dto = cuentaMapper.entityToDto(cuenta.get());
				ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
						.code(HttpStatus.OK.value()).response(dto).build();
				return ResponseEntity.ok(responseDTO);
			} else {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
		} catch (Exception e) {
			log.error("Error al buscar cuenta por id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todas las cuentas");
		try {
			var list = cuentaRepository.findAll();
			var dtoList = cuentaMapper.listEntityToDtoList(list);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar las cuentas", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEmpresa(Integer idEmpresa, String cuentaCodigo, String cuentaNombre,
			Double cuentaValor, String tipoNombre, String tipoNaturaleza, Pageable pageable) {
		log.info("Buscar cuentas por empresa={}, filtros: codigo={}, nombre={}, valor={}, tipoNombre={}, naturaleza={}",
				idEmpresa, cuentaCodigo, cuentaNombre, cuentaValor, tipoNombre, tipoNaturaleza);

		try {
			if (idEmpresa == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("Parámetro requerido: idEmpresa").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			Sort defaultSort = Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.desc("id"));
			Pageable pageToUse = (pageable == null) ? PageRequest.of(0, 20, defaultSort)
					: (pageable.getSort().isUnsorted()
							? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort)
							: pageable);

			Specification<CuentaEntity> spec = Specification.allOf(CuentaSpecifications.empresaIdEquals(idEmpresa),
					CuentaSpecifications.codigoLike(cuentaCodigo), CuentaSpecifications.nombreLike(cuentaNombre),
					CuentaSpecifications.valorEquals(cuentaValor), CuentaSpecifications.tipoNombreLike(tipoNombre),
					CuentaSpecifications.tipoNaturalezaLike(tipoNaturaleza));

			Page<CuentaEntity> page = cuentaRepository.findAll(spec, pageToUse);

			if (page.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron cuentas para la empresa indicada")
								.code(HttpStatus.NOT_FOUND.value()).response(List.of()).totalCount(0L)
								.pageSize(pageToUse.getPageSize()).currentPage(pageToUse.getPageNumber()).totalPages(0)
								.build());
			}

			List<CuentaDTO> content = page.getContent().stream().map(cuentaMapper::entityToDto).toList();

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(content).totalCount(page.getTotalElements())
					.pageSize(page.getSize()).currentPage(page.getNumber()).totalPages(page.getTotalPages()).build());

		} catch (Exception e) {
			log.error("Error al buscar cuentas por empresa {}", idEmpresa, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar cuenta por id: {}", id);
		try {
			if (!cuentaRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			cuentaRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar cuenta con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}
}
