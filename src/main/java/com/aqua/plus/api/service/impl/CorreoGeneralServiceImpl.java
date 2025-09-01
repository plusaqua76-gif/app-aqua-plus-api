package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.ICorreoGeneralService;
import com.aqua.plus.commons.dtos.CorreoGeneralDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.CorreoGeneralEntity;
import com.aqua.plus.commons.maps.CorreoGeneralMapper;
import com.aqua.plus.commons.repositories.CorreoGeneralRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorreoGeneralServiceImpl implements ICorreoGeneralService{
	
	private final CorreoGeneralRepository correoGeneralRepository;
	private final CorreoGeneralMapper correoGeneralMapper;
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(CorreoGeneralDTO correoGeneralDTO) {
	    log.info("Guardar/Actualizar Correo General");
	    try {
	        boolean isUpdate = correoGeneralDTO.getId() != null && correoGeneralRepository.existsById(correoGeneralDTO.getId());

	        Optional<CorreoGeneralEntity> existingCorreo = correoGeneralRepository.findByCorreoIgnoreCase(correoGeneralDTO.getCorreo());

	        if (existingCorreo.isPresent() && (!isUpdate || !existingCorreo.get().getId().equals(correoGeneralDTO.getId()))) {
	            ResponseDTO errorResponse = ResponseDTO.builder()
	                    .success(false)
	                    .message("El correo '" + correoGeneralDTO.getCorreo() + "' ya se encuentra registrado.")
	                    .code(HttpStatus.BAD_REQUEST.value())
	                    .build();
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	        }

	        CorreoGeneralEntity entity;
	        if (isUpdate) {
	            entity = correoGeneralRepository.findById(correoGeneralDTO.getId()).orElseThrow();
	            correoGeneralMapper.updateEntityFromDto(correoGeneralDTO, entity);
	            entity.setFechaModificacion(new Date());
	            entity.setUsuarioModificacion(correoGeneralDTO.getUsuarioModificacion());
	        } else {
	            entity = correoGeneralMapper.dtoToEntity(correoGeneralDTO);
	            entity.setFechaCreacion(new Date());
	            entity.setUsuarioCreacion(correoGeneralDTO.getUsuarioCreacion());
	            entity.setActivo(true);
	        }

	        CorreoGeneralEntity saved = correoGeneralRepository.save(entity);
	        CorreoGeneralDTO savedDTO = correoGeneralMapper.entityToDto(saved);

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
	        log.error("Error guardando el Correo General", e);
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
	    log.info("Buscar Correo General por id: {}", id);
	    try {
	        Optional<CorreoGeneralEntity> correoGeneral = correoGeneralRepository.findById(id);
	        if (correoGeneral.isPresent()) {
	        	CorreoGeneralDTO dto = correoGeneralMapper.entityToDto(correoGeneral.get());
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
	        log.error("Error al buscar Correo General por id: {}", id, e);
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
	public ResponseEntity<ResponseDTO> findByEnterpriseId(Integer id) {
	    log.info("Buscar Correo General por id de empresa: {}", id);
	    try {
	        Optional<CorreoGeneralEntity> correoGeneral = correoGeneralRepository.findTopByEmpresa_IdAndActivoTrueOrderByFechaCreacionDesc(id);
	        if (correoGeneral.isPresent()) {
	        	CorreoGeneralDTO dto = correoGeneralMapper.entityToDto(correoGeneral.get());
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
	        log.error("Error al buscar Correo General por id de empresa: {}", id, e);
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
        log.info("Listar todos los Correo Generales");
        try {
            var list = correoGeneralRepository.findAll();
            var dtoList = correoGeneralMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar los Correo General", e);
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
        log.info("Inicio método para eliminar Correo General por id: {}", id);
        try {
            if (!correoGeneralRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            correoGeneralRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar el Correo general con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}