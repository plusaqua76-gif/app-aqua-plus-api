package com.aqua.plus.api.service.impl;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.ITipoConceptoService;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.TipoConceptoDTO;
import com.aqua.plus.commons.entities.TipoConceptoEntity;
import com.aqua.plus.commons.maps.TipoConceptoMapper;
import com.aqua.plus.commons.repositories.TipoConceptoRepository;
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
public class TipoConceptoServiceImpl implements ITipoConceptoService {

	private final TipoConceptoRepository tipoConceptoRepository;
	private final TipoConceptoMapper tipoConceptoMapper;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(TipoConceptoDTO tipoConceptoDTO) {
		log.info("Guardar/Actualizar Tipo Concepto");
		try {
			boolean isUpdate = tipoConceptoDTO.getId() != null
					&& tipoConceptoRepository.existsById(tipoConceptoDTO.getId());
			TipoConceptoEntity entity;

			if (isUpdate) {
				entity = tipoConceptoRepository.findById(tipoConceptoDTO.getId()).orElseThrow();
				tipoConceptoMapper.updateEntityFromDto(tipoConceptoDTO, entity);
				entity.setFechaModificacion(new Date());
				entity.setUsuarioModificacion(tipoConceptoDTO.getUsuarioModificacion());
			} else {
				entity = tipoConceptoMapper.dtoToEntity(tipoConceptoDTO);
				entity.setFechaCreacion(new Date());
				entity.setUsuarioCreacion(tipoConceptoDTO.getUsuarioCreacion());
				entity.setActivo(true);
			}

			TipoConceptoEntity saved = tipoConceptoRepository.save(entity);
			TipoConceptoDTO savedDTO = tipoConceptoMapper.entityToDto(saved);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(message).code(statusCode)
					.response(savedDTO).build();

			return ResponseEntity.status(statusCode).body(responseDTO);

		} catch (Exception e) {
			log.error("Error guardando el tipo concepto", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todos los tipos conceptos");
		try {
			var list = tipoConceptoRepository.findAll();
			var dtoList = tipoConceptoMapper.listEntityToDtoList(list);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar los tipos conceptos", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}
	
	@Override
    @Transactional
    public ResponseEntity<ResponseDTO> deleteById(Integer id) {
        log.info("Inicio método para eliminar tipo de tarifa por id: {}", id);
        try {
            if (!tipoConceptoRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            tipoConceptoRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar el tipo de tarifa con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}
