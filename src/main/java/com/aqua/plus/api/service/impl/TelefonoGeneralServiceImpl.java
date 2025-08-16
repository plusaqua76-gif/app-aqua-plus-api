package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.ITelefonoGeneralService;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.TelefonoGeneralDTO;
import com.aqua.plus.commons.entities.TelefonoGeneralEntity;
import com.aqua.plus.commons.maps.TelefonoGeneralMapper;
import com.aqua.plus.commons.repositories.TelefonoGeneralRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelefonoGeneralServiceImpl implements ITelefonoGeneralService{
	
	private final TelefonoGeneralRepository telefonoGeneralRepository;
	private final TelefonoGeneralMapper telefonoGeneralMapper;
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(TelefonoGeneralDTO telefonoGeneralDTO) {
	    log.info("Guardar/Actualizar Telefono general");
	    try {
	        boolean isUpdate = telefonoGeneralDTO.getId() != null && telefonoGeneralRepository.existsById(telefonoGeneralDTO.getId());
	        TelefonoGeneralEntity entity;

	        if (isUpdate) {
	            entity = telefonoGeneralRepository.findById(telefonoGeneralDTO.getId())
	                    .orElseThrow(() -> new RuntimeException("TelefonoGeneral not found with ID: " + telefonoGeneralDTO.getId()));

	            if (!entity.getNumero().equals(telefonoGeneralDTO.getNumero()) &&
	                telefonoGeneralRepository.existsByNumero(telefonoGeneralDTO.getNumero())) {
	                
	                log.warn("El número de teléfono {} ya existe en el sistema.", telefonoGeneralDTO.getNumero());
	                ResponseDTO errorResponse = ResponseDTO.builder()
	                        .success(false)
	                        .message(Constantes.DUPLICATE_PHONE_NUMBER_ERROR)
	                        .code(HttpStatus.CONFLICT.value())
	                        .build();
	                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
	            }

	            telefonoGeneralMapper.updateEntityFromDto(telefonoGeneralDTO, entity);
	            entity.setFechaModificacion(new Date());
	            entity.setUsuarioModificacion(telefonoGeneralDTO.getUsuarioModificacion());
	        } else {
	            if (telefonoGeneralRepository.existsByNumero(telefonoGeneralDTO.getNumero())) {
	                log.warn("El número de teléfono {} ya existe en el sistema.", telefonoGeneralDTO.getNumero());
	                ResponseDTO errorResponse = ResponseDTO.builder()
	                        .success(false)
	                        .message(Constantes.DUPLICATE_PHONE_NUMBER_ERROR)
	                        .code(HttpStatus.CONFLICT.value())
	                        .build();
	                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
	            }

	            entity = telefonoGeneralMapper.dtoToEntity(telefonoGeneralDTO);
	            entity.setFechaCreacion(new Date());
	            entity.setUsuarioCreacion(telefonoGeneralDTO.getUsuarioCreacion());
	            entity.setActivo(true);
	        }

	        TelefonoGeneralEntity saved = telefonoGeneralRepository.save(entity);
	        TelefonoGeneralDTO savedDTO = telefonoGeneralMapper.entityToDto(saved);

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
	        log.error("Error guardando el Telefono general", e);
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
	    log.info("Buscar Telefono general por id: {}", id);
	    try {
	        Optional<TelefonoGeneralEntity> telefonoGeneral = telefonoGeneralRepository.findById(id);
	        if (telefonoGeneral.isPresent()) {
	        	TelefonoGeneralDTO dto = telefonoGeneralMapper.entityToDto(telefonoGeneral.get());
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
	        log.error("Error al buscar Telefono general por id: {}", id, e);
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
        log.info("Listar todos Telefono generales");
        try {
            var list = telefonoGeneralRepository.findAll();
            var dtoList = telefonoGeneralMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar Telefono general", e);
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
        log.info("Inicio método para eliminar Telefono general por id: {}", id);
        try {
            if (!telefonoGeneralRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            telefonoGeneralRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar Telefono general con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}