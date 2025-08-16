package com.acua.plus.api.service.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acua.plus.api.service.IParametrosGeneralesService;
import com.acua.plus.commons.dtos.ParametrosGeneralesDTO;
import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.entities.ParametrosGeneralesEntity;
import com.acua.plus.commons.maps.ParametrosGeneralesMapper;
import com.acua.plus.commons.repositories.ParametrosGeneralesRepository;
import com.acua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParametrosGeneralesServiceImpl implements IParametrosGeneralesService{
	
	private final ParametrosGeneralesRepository parametrosGeneralesRepository;
	private final ParametrosGeneralesMapper parametrosGeneralesMapper;
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(ParametrosGeneralesDTO parametrosGeneralesDTO) {
	    log.info("Guardar/Actualizar Parametros Generales");
	    try {
	        boolean isUpdate = parametrosGeneralesDTO.getId() != null && parametrosGeneralesRepository.existsById(parametrosGeneralesDTO.getId());
	        ParametrosGeneralesEntity entity;

	        if (isUpdate) {
	            entity = parametrosGeneralesRepository.findById(parametrosGeneralesDTO.getId()).orElseThrow();
	            parametrosGeneralesMapper.updateEntityFromDto(parametrosGeneralesDTO, entity);
	            entity.setFechaModificacion(new Date());
	            entity.setUsuarioModificacion(parametrosGeneralesDTO.getUsuarioModificacion());
	        } else {
	            entity = parametrosGeneralesMapper.dtoToEntity(parametrosGeneralesDTO);
	            entity.setFechaCreacion(new Date());
	            entity.setUsuarioCreacion(parametrosGeneralesDTO.getUsuarioCreacion());
	            entity.setActivo(true);
	        }

	        ParametrosGeneralesEntity saved = parametrosGeneralesRepository.save(entity);
	        ParametrosGeneralesDTO savedDTO = parametrosGeneralesMapper.entityToDto(saved);

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
	        log.error("Error guardando Parametros Generales", e);
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
	    log.info("Buscar Parametros Generales por id: {}", id);
	    try {
	        Optional<ParametrosGeneralesEntity> parametrosGenerales = parametrosGeneralesRepository.findById(id);
	        if (parametrosGenerales.isPresent()) {
	        	ParametrosGeneralesDTO dto = parametrosGeneralesMapper.entityToDto(parametrosGenerales.get());
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
	        log.error("Error al buscar Parametros Generales por id: {}", id, e);
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
        log.info("Listar todos los Parametros Generales");
        try {
            var list = parametrosGeneralesRepository.findAll();
            var dtoList = parametrosGeneralesMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar los Parametros Generales", e);
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
        log.info("Inicio método para eliminar Parametros Generales por id: {}", id);
        try {
            if (!parametrosGeneralesRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            parametrosGeneralesRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar Parametros Generales con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}
