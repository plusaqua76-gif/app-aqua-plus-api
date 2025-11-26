package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.ITipoUsoService;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.TipoUsoDTO;
import com.aqua.plus.commons.entities.TipoUsoEntity;
import com.aqua.plus.commons.maps.TipoUsoMapper;
import com.aqua.plus.commons.repositories.TipoUsoRepository;
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
public class TipoUsoServiceImpl implements ITipoUsoService {

	private final TipoUsoRepository tipoUsoRepository;
	private final TipoUsoMapper tipoUsoMapper;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(TipoUsoDTO tipoUsoDTO) {
		log.info("Guardar/Actualizar Tipo de Uso");

		try {
			if (tipoUsoDTO == null) {
				throw new IllegalArgumentException("El objeto TipoUsoDTO es obligatorio");
			}

			boolean isUpdate = tipoUsoDTO.getId() != null && tipoUsoRepository.existsById(tipoUsoDTO.getId());
			TipoUsoEntity entity;

			if (isUpdate) {
				entity = tipoUsoRepository.findById(tipoUsoDTO.getId()).orElseThrow();

				tipoUsoMapper.updateEntityFromDto(tipoUsoDTO, entity);

				entity.setFechaModificacion(new Date());
				entity.setUsuarioModificacion(tipoUsoDTO.getUsuarioModificacion());

			} else {
				entity = tipoUsoMapper.dtoToEntity(tipoUsoDTO);

				entity.setFechaCreacion(new Date());
				entity.setUsuarioCreacion(tipoUsoDTO.getUsuarioCreacion());

				if (entity.getActivo() == null) {
					entity.setActivo(Boolean.TRUE);
				}
			}

			TipoUsoEntity saved = tipoUsoRepository.save(entity);
			TipoUsoDTO savedDTO = tipoUsoMapper.entityToDto(saved);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			HttpStatus status = isUpdate ? HttpStatus.OK : HttpStatus.CREATED;

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(message).code(status.value())
					.response(savedDTO).build();

			return ResponseEntity.status(status).body(responseDTO);

		} catch (DataIntegrityViolationException e) {
			log.error("Error de integridad de datos guardando Tipo de Uso", e);

			Map<String, Object> errorDetail = new HashMap<>();
			errorDetail.put("type", "DATA_INTEGRITY_VIOLATION");
			errorDetail.put("detail",
					e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage());

			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
					.code(HttpStatus.CONFLICT.value()).response(errorDetail).build();

			return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

		} catch (IllegalArgumentException e) {
			log.error("Error en datos de entrada al guardar Tipo de Uso: {}", e.getMessage());

			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(e.getMessage())
					.code(HttpStatus.BAD_REQUEST.value()).build();

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

		} catch (Exception e) {
			log.error("Error inesperado guardando Tipo de Uso", e);

			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEnterpriseId(Integer idEmpresa) {
		log.info("Buscar TipoUso por empresaId={}", idEmpresa);

		try {
			// Validación de parámetro
			if (idEmpresa == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
						.message("Parámetro requerido: idEmpresa").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			List<TipoUsoEntity> entities = tipoUsoRepository.findByEmpresa_Id(idEmpresa);

			if (entities == null || entities.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron tipos de uso para la empresa indicada")
								.code(HttpStatus.NOT_FOUND.value()).totalCount(0L).build());
			}

			List<TipoUsoDTO> dtos = entities.stream().map(tipoUsoMapper::entityToDto).toList();

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtos).totalCount((long) dtos.size()).build();

			return ResponseEntity.ok(responseDTO);

		} catch (IllegalArgumentException e) {
			log.error("Error de validación en findByEnterpriseId: {}", e.getMessage());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
					.message(e.getMessage()).code(HttpStatus.BAD_REQUEST.value()).build());

		} catch (DataAccessException e) {
			log.error("Error de acceso a datos al buscar tipo de uso por empresaId {}", idEmpresa, e);

			return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.builder().success(false)
					.message("Error consultando datos en la base de datos").code(HttpStatus.CONFLICT.value()).build());

		} catch (Exception e) {
			log.error("Error inesperado en findByEnterpriseId", e);

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar tipo de uso por id: {}", id);

		try {
			if (!tipoUsoRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();

				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}

			tipoUsoRepository.deleteById(id);

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();

			return ResponseEntity.ok(responseDTO);

		} catch (Exception e) {
			log.error("Error al eliminar el tipo de uso con id: {}", id, e);

			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

}
