package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IAforoService;
import com.aqua.plus.commons.dtos.AforoDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.AforoEntity;
import com.aqua.plus.commons.entities.EmpresaEntity;
import com.aqua.plus.commons.entities.ParametrosGeneralesEntity;
import com.aqua.plus.commons.entities.TipoUsoEntity;
import com.aqua.plus.commons.maps.AforoMapper;
import com.aqua.plus.commons.repositories.AforoRepository;
import com.aqua.plus.commons.repositories.EmpresaRepository;
import com.aqua.plus.commons.repositories.ParametrosGeneralesRepository;
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
public class AforoServiceImpl implements IAforoService {

	private final AforoMapper aforoMapper;
	private final AforoRepository aforoRepository;
	private final EmpresaRepository empresaRepository;
	private final TipoUsoRepository tipoUsoRepository;
	private final ParametrosGeneralesRepository parametrosGeneralesRepository;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(AforoDTO aforoDTO) {
		log.info("Guardar/Actualizar aforo");

		try {
			boolean isUpdate = aforoDTO.getId() != null && aforoRepository.existsById(aforoDTO.getId());

			AforoEntity entity;

			if (isUpdate) {
				entity = aforoRepository.findById(aforoDTO.getId())
						.orElseThrow(() -> new RuntimeException("Aforo no encontrado con id=" + aforoDTO.getId()));

				aforoMapper.updateEntityFromDto(aforoDTO, entity);
				entity.setFechaModificacion(new Date());
				entity.setUsuarioModificacion(aforoDTO.getUsuarioModificacion());

			} else {
				entity = aforoMapper.dtoToEntity(aforoDTO);

				entity.setActivo(true);

				entity.setFechaCreacion(new Date());
				entity.setUsuarioCreacion(aforoDTO.getUsuarioCreacion());
			}

			if (aforoDTO.getEmpresa() != null && aforoDTO.getEmpresa().getId() != null) {
				EmpresaEntity empresa = empresaRepository.findById(aforoDTO.getEmpresa().getId()).orElseThrow(
						() -> new RuntimeException("Empresa no encontrada con id=" + aforoDTO.getEmpresa().getId()));
				entity.setEmpresa(empresa);
			} else {
				throw new RuntimeException("Empresa es obligatoria");
			}

			if (aforoDTO.getTipoUso() != null && aforoDTO.getTipoUso().getId() != null) {
				TipoUsoEntity tipoUso = tipoUsoRepository.findById(aforoDTO.getTipoUso().getId()).orElseThrow(
						() -> new RuntimeException("TipoUso no encontrado con id=" + aforoDTO.getTipoUso().getId()));
				entity.setTipoUso(tipoUso);
			} else {
				throw new RuntimeException("TipoUso es obligatorio");
			}

			if (aforoDTO.getTipoAforo() != null && aforoDTO.getTipoAforo().getId() != null) {
				ParametrosGeneralesEntity tipoAforo = parametrosGeneralesRepository
						.findById(aforoDTO.getTipoAforo().getId())
						.orElseThrow(() -> new RuntimeException("TipoAforo (parametro general) no encontrado con id="
								+ aforoDTO.getTipoAforo().getId()));
				entity.setTipoAforo(tipoAforo);
			} else {
				if (!isUpdate) {
					entity.setTipoAforo(null);
				}
			}

			if (aforoDTO.getNumeroSuscriptores() == null) {
				throw new RuntimeException("numeroSuscriptores es obligatorio");
			}
			if (aforoDTO.getFrecBarrido() == null) {
				throw new RuntimeException("frecBarrido es obligatorio");
			}
			if (aforoDTO.getFrecRecoleccion() == null) {
				throw new RuntimeException("frecRecoleccion es obligatorio");
			}
			if (aforoDTO.getTarifaBase() == null) {
				throw new RuntimeException("tarifaBase es obligatorio");
			}

			AforoEntity saved = aforoRepository.save(entity);
			AforoDTO savedDTO = aforoMapper.entityToDto(saved);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(message).code(statusCode)
					.response(savedDTO).build();

			return ResponseEntity.status(statusCode).body(responseDTO);

		} catch (RuntimeException e) {
			log.error("Error de negocio guardando aforo: {}", e.getMessage(), e);

			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(e.getMessage())
					.code(HttpStatus.BAD_REQUEST.value()).build();

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

		} catch (Exception e) {
			log.error("Error inesperado guardando aforo", e);

			ResponseDTO errorResponse = ResponseDTO.builder().success(false)
					.message("Error interno del servidor: " + e.getClass().getSimpleName())
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEnterpriseId(Integer idEmpresa) {

		log.info("Buscar aforos por empresaId={}", idEmpresa);

		try {

			if (idEmpresa == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("Parámetro requerido: idEmpresa").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			List<AforoEntity> entities = aforoRepository.findByEmpresa_Id(idEmpresa);

			if (entities == null || entities.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron aforos para la empresa indicada")
								.code(HttpStatus.NOT_FOUND.value()).totalCount(0L).build());
			}

			List<AforoDTO> dtos = entities.stream().map(aforoMapper::entityToDto).toList();

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtos).totalCount((long) dtos.size()).build());

		} catch (Exception e) {

			log.error("Error al buscar aforos por empresaId={}", idEmpresa, e);

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message("Error al consultar aforos por empresa")
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar el aforo por id: {}", id);
		try {
			if (!aforoRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			aforoRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar el aforo con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

}
