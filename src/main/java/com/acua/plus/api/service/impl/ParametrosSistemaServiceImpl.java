package com.acua.plus.api.service.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acua.plus.api.service.IParametrosSistemaService;
import com.acua.plus.commons.dtos.ParametrosSistemaDTO;
import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.entities.ParametrosSistemaEntity;
import com.acua.plus.commons.maps.ParametrosSistemaMapper;
import com.acua.plus.commons.repositories.ParametrosSistemaRepository;
import com.acua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParametrosSistemaServiceImpl implements IParametrosSistemaService{
	
	private final ParametrosSistemaRepository parametrosSistemaRepository;
	private final ParametrosSistemaMapper parametrosSistemaMapper;
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(ParametrosSistemaDTO parametrosSistemaDTO) {
	    log.info("Guardar/Actualizar Parametros del Sistema");
	    try {
	        boolean isUpdate = parametrosSistemaDTO.getId() != null && parametrosSistemaRepository.existsById(parametrosSistemaDTO.getId());
	        ParametrosSistemaEntity entity;

	        if (isUpdate) {
	            entity = parametrosSistemaRepository.findById(parametrosSistemaDTO.getId()).orElseThrow();
	            parametrosSistemaMapper.updateEntityFromDto(parametrosSistemaDTO, entity);
	            entity.setFechaModificacion(new Date());
	            entity.setUsuarioModificacion(parametrosSistemaDTO.getUsuarioModificacion());
	        } else {
	            entity = parametrosSistemaMapper.dtoToEntity(parametrosSistemaDTO);
	            entity.setFechaCreacion(new Date());
	            entity.setUsuarioCreacion(parametrosSistemaDTO.getUsuarioCreacion());
	            entity.setActivo(true);
	        }

	        ParametrosSistemaEntity saved = parametrosSistemaRepository.save(entity);
	        ParametrosSistemaDTO savedDTO = parametrosSistemaMapper.entityToDto(saved);

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
	        log.error("Error guardando Parametros del Sistema", e);
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
	    log.info("Buscar Parametros del Sistema por id: {}", id);
	    try {
	        Optional<ParametrosSistemaEntity> parametrosSistema = parametrosSistemaRepository.findById(id);
	        if (parametrosSistema.isPresent()) {
	        	ParametrosSistemaDTO dto = parametrosSistemaMapper.entityToDto(parametrosSistema.get());
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
	        log.error("Error al buscar Parametros del Sistema por id: {}", id, e);
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
        log.info("Listar todos los Parametros del Sistema");
        try {
            var list = parametrosSistemaRepository.findAll();
            var dtoList = parametrosSistemaMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar los Parametros del Sistema", e);
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
        log.info("Inicio método para eliminar Parametros del Sistema por id: {}", id);
        try {
            if (!parametrosSistemaRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            parametrosSistemaRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar Parametros del Sistema con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}
