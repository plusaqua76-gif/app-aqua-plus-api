package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.ITipoDocumentoService;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.TipoDocumentoDTO;
import com.aqua.plus.commons.entities.TipoDocumentoEntity;
import com.aqua.plus.commons.maps.TipoDocumentoMapper;
import com.aqua.plus.commons.repositories.TipoDocumentoRepository;
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
public class TipoDocumentoServiceImpl implements ITipoDocumentoService {

	private final TipoDocumentoRepository tipoDocumentoRepository;
	private final TipoDocumentoMapper tipoDocumentoMapper;
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(TipoDocumentoDTO tipoDocumentoDTO) {
	    log.info("Guardar/Actualizar Tipo de Documento");
	    try {
	        boolean isUpdate = tipoDocumentoDTO.getId() != null && tipoDocumentoRepository.existsById(tipoDocumentoDTO.getId());
	        TipoDocumentoEntity entity;

	        if (isUpdate) {
	            entity = tipoDocumentoRepository.findById(tipoDocumentoDTO.getId()).orElseThrow();
	            tipoDocumentoMapper.updateEntityFromDto(tipoDocumentoDTO, entity);
	            entity.setFechaModificacion(new Date());
	            entity.setUsuarioModificacion(tipoDocumentoDTO.getUsuarioModificacion());
	        } else {
	            entity = tipoDocumentoMapper.dtoToEntity(tipoDocumentoDTO);
	            entity.setFechaCreacion(new Date());
	            entity.setUsuarioCreacion(tipoDocumentoDTO.getUsuarioCreacion());
	            entity.setActivo(true);
	        }

	        TipoDocumentoEntity saved = tipoDocumentoRepository.save(entity);
	        TipoDocumentoDTO savedDTO = tipoDocumentoMapper.entityToDto(saved);

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
	        log.error("Error guardando el tipo de documento", e);
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
	    log.info("Buscar tipo de documento por id: {}", id);
	    try {
	        Optional<TipoDocumentoEntity> tipoDocumento = tipoDocumentoRepository.findById(id);
	        if (tipoDocumento.isPresent()) {
	        	TipoDocumentoDTO dto = tipoDocumentoMapper.entityToDto(tipoDocumento.get());
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
	        log.error("Error al buscar tipo documento por id: {}", id, e);
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
        log.info("Listar todos los tipos de documentos");
        try {
            var list = tipoDocumentoRepository.findAll();
            var dtoList = tipoDocumentoMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar los tipos de documentos", e);
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
        log.info("Inicio método para eliminar tipo de documento por id: {}", id);
        try {
            if (!tipoDocumentoRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            tipoDocumentoRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar el tipo de documento con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}
