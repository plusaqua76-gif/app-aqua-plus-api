package com.acua.plus.api.service.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acua.plus.api.service.ITipoNovedadService;
import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.dtos.TipoNovedadDTO;
import com.acua.plus.commons.entities.TipoNovedadEntity;
import com.acua.plus.commons.maps.TipoNovedadMapper;
import com.acua.plus.commons.repositories.TipoNovedadRepository;
import com.acua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TipoNovedadServiceImpl implements ITipoNovedadService{
	
	private final TipoNovedadRepository tipoNovedadRepository;
	private final TipoNovedadMapper tipoNovedadMapper;
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(TipoNovedadDTO tipoNovedadDTO) {
	    log.info("Guardar/Actualizar Tipo de Novedad");
	    try {
	        boolean isUpdate = tipoNovedadDTO.getId() != null && tipoNovedadRepository.existsById(tipoNovedadDTO.getId());
	        TipoNovedadEntity entity;

	        if (isUpdate) {
	            entity = tipoNovedadRepository.findById(tipoNovedadDTO.getId()).orElseThrow();
	            tipoNovedadMapper.updateEntityFromDto(tipoNovedadDTO, entity);
	            entity.setFechaModificacion(new Date());
	            entity.setUsuarioModificacion(tipoNovedadDTO.getUsuarioModificacion());
	        } else {
	            entity = tipoNovedadMapper.dtoToEntity(tipoNovedadDTO);
	            entity.setFechaCreacion(new Date());
	            entity.setUsuarioCreacion(tipoNovedadDTO.getUsuarioCreacion());
	            entity.setActivo(true);
	        }

	        TipoNovedadEntity saved = tipoNovedadRepository.save(entity);
	        TipoNovedadDTO savedDTO = tipoNovedadMapper.entityToDto(saved);

	        String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
	        int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

	        ResponseDTO responseDTO = ResponseDTO.builder()
	                .success(true)
	                .message(message)
	                .code(statusCode)
	                .response(savedDTO)
	                .build();

	        return ResponseEntity.status(statusCode).body(responseDTO);

	    } catch (Exception e) {
	        log.error("Error guardando el tipo de Novedad", e);
	        ResponseDTO errorResponse = ResponseDTO.builder()
	                .success(false)
	                .message(Constantes.SAVE_ERROR)
	                .code(HttpStatus.BAD_REQUEST.value())
	                .build();

	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	    }
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
	    log.info("Buscar tipo de Novedad por id: {}", id);
	    try {
	        Optional<TipoNovedadEntity> tipoNovedad = tipoNovedadRepository.findById(id);
	        if (tipoNovedad.isPresent()) {
	        	TipoNovedadDTO dto = tipoNovedadMapper.entityToDto(tipoNovedad.get());
	            ResponseDTO responseDTO = ResponseDTO.builder()
	                    .success(true)
	                    .message(Constantes.CONSULTED_SUCCESSFULLY)
	                    .code(HttpStatus.OK.value())
	                    .response(dto)
	                    .build();
	            return ResponseEntity.ok(responseDTO);
	        } else {
	            ResponseDTO responseDTO = ResponseDTO.builder()
	                    .success(false)
	                    .message(Constantes.CONSULTING_ERROR)
	                    .code(HttpStatus.NOT_FOUND.value())
	                    .build();
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
	        }
	    } catch (Exception e) {
	        log.error("Error al buscar tipo Novedad por id: {}", id, e);
	        ResponseDTO responseDTO = ResponseDTO.builder()
	                .success(false)
	                .message(Constantes.ERROR_QUERY_RECORD_BY_ID)
	                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
	                .build();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
	    }
	}

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findAll() {
        log.info("Listar todos los tipos de Novedad");
        try {
            var list = tipoNovedadRepository.findAll();
            var dtoList = tipoNovedadMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar los tipos de Novedad", e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.CONSULTING_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .response(null)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDTO> deleteById(Integer id) {
        log.info("Inicio método para eliminar tipo de Novedad por id: {}", id);
        try {
            if (!tipoNovedadRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            tipoNovedadRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar el tipo de Novedad con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}