package com.aqua.plus.api.service.impl;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.INominaService;
import com.aqua.plus.commons.dtos.NominaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.NominaEntity;
import com.aqua.plus.commons.maps.NominaMapper;
import com.aqua.plus.commons.repositories.NominaRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NominaServiceImpl implements INominaService {

	private final NominaRepository nominaRepository;
	private final NominaMapper nominaMapper;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(NominaDTO nominaDTO) {
		log.info("Guardar/Actualizar Nómina");
		try {
			boolean isUpdate = nominaDTO.getId() != null && nominaRepository.existsById(nominaDTO.getId());
			NominaEntity entity;

			if (isUpdate) {
				entity = nominaRepository.findById(nominaDTO.getId()).orElseThrow();
				nominaMapper.updateEntityFromDto(nominaDTO, entity);
			} else {
				entity = nominaMapper.dtoToEntity(nominaDTO);
				entity.setActivo(true);
			}

			NominaEntity saved = nominaRepository.save(entity);
			NominaDTO savedDTO = nominaMapper.entityToDto(saved);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			return ResponseEntity.status(statusCode).body(
					ResponseDTO.builder().success(true).message(message).code(statusCode).response(savedDTO).build());

		} catch (Exception e) {
			log.error("Error guardando nómina", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
					.message(Constantes.SAVE_ERROR).code(HttpStatus.BAD_REQUEST.value()).build());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEmpleadoId(Integer idEmpleado) {
		log.info("Buscar nómina por empleadoId={}", idEmpleado);
		try {
			if (idEmpleado == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("Parámetro requerido: idEmpleado").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			List<NominaEntity> entities = nominaRepository.findByEmpleado_Id(idEmpleado);

			if (entities == null || entities.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontró nómina para el empleado indicado")
								.code(HttpStatus.NOT_FOUND.value()).totalCount(0L).build());
			}

			List<NominaDTO> dtos = entities.stream().map(nominaMapper::entityToDto).toList();

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtos).totalCount((long) dtos.size()).build());

		} catch (Exception e) {
			log.error("Error buscando nómina por empleadoId={}", idEmpleado, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todas las nóminas");
		try {
			var list = nominaRepository.findAll();
			var dtoList = nominaMapper.listEntityToDtoList(list);

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).totalCount((long) dtoList.size()).build());

		} catch (Exception e) {
			log.error("Error al listar nóminas", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build());
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar nómina por id: {}", id);
		try {
			if (!nominaRepository.existsById(id)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.builder().success(false)
						.message(Constantes.RECORD_NOT_FOUND).code(HttpStatus.NOT_FOUND.value()).build());
			}

			nominaRepository.deleteById(id);

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build());

		} catch (Exception e) {
			log.error("Error al eliminar nómina con id: {}", id, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message(Constantes.DELETE_ERROR).code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

}
