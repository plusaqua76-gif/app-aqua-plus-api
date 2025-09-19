package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IConceptoEstratoService;
import com.aqua.plus.commons.dtos.ConceptoEstratoDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ConceptoEstratoEntity;
import com.aqua.plus.commons.maps.ConceptoEstratoMapper;
import com.aqua.plus.commons.repositories.ConceptoEstratoRepository;
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
public class ConceptoEstratoServiceImpl implements IConceptoEstratoService {

	private final ConceptoEstratoRepository conceptoEstratoRepository;
	private final ConceptoEstratoMapper conceptoEstratoMapper;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(ConceptoEstratoDTO concepEstratoDTO) {
		log.info("Actualizar Concepto Estrato, id={}", concepEstratoDTO != null ? concepEstratoDTO.getId() : null);

		try {
			if (concepEstratoDTO == null || concepEstratoDTO.getId() == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("El id es obligatorio para actualizar").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			ConceptoEstratoEntity entity = conceptoEstratoRepository.findById(concepEstratoDTO.getId())
					.orElseThrow(() -> new NoSuchElementException("Concepto Estrato no encontrado"));

			conceptoEstratoMapper.updateEntityFromDto(concepEstratoDTO, entity);
			entity.setFechaModificacion(new Date());
			entity.setUsuarioModificacion(concepEstratoDTO.getUsuarioModificacion());

			ConceptoEstratoEntity updated = conceptoEstratoRepository.saveAndFlush(entity);
			ConceptoEstratoDTO updatedDTO = conceptoEstratoMapper.entityToDto(updated);

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.UPDATED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(updatedDTO).build());

		} catch (NoSuchElementException notFound) {
			log.warn("No existe Concepto Estrato id={}", concepEstratoDTO.getId());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.builder().success(false)
					.message("No existe el registro a actualizar").code(HttpStatus.NOT_FOUND.value()).build());
		} catch (Exception e) {
			log.error("Error actualizando Concepto Estrato id={}",
					concepEstratoDTO != null ? concepEstratoDTO.getId() : null, e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
					.message(Constantes.SAVE_ERROR).code(HttpStatus.BAD_REQUEST.value()).build());
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar Concepto Estrato por id: {}", id);
		try {
			if (!conceptoEstratoRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			conceptoEstratoRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar Concepto Estrato con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}
}
