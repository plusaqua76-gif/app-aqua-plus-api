package com.acua.plus.api.service.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acua.plus.api.service.IPersonaService;
import com.acua.plus.commons.dtos.PersonaDTO;
import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.entities.DireccionEntity;
import com.acua.plus.commons.entities.PersonaEntity;
import com.acua.plus.commons.entities.TipoDocumentoEntity;
import com.acua.plus.commons.maps.PersonaMapper;
import com.acua.plus.commons.repositories.DireccionRepository;
import com.acua.plus.commons.repositories.PersonaRepository;
import com.acua.plus.commons.repositories.TipoDocumentoRepository;
import com.acua.plus.commons.utils.Constantes;

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
public class PersonaServiceImpl implements IPersonaService {

	private final PersonaRepository personaRepository;
	private final DireccionRepository direccionRepository;
	private final TipoDocumentoRepository tipoDocumentoRepository;
    private final PersonaMapper personaMapper;

    @Override
    @Transactional
    public ResponseEntity<ResponseDTO> save(PersonaDTO personaDTO) {
        log.info("Guardar/Actualizar persona");

        try {
            boolean isUpdate = personaDTO.getId() != null && personaRepository.existsById(personaDTO.getId());

            ResponseEntity<ResponseDTO> validationResponse = validatePersonaData(personaDTO, isUpdate);
            if (validationResponse != null) {
                return validationResponse;
            }
            PersonaEntity entity = buildPersonaEntity(personaDTO, isUpdate);

            setRelacionDireccion(personaDTO, entity);
            setRelacionTipoDocumento(personaDTO, entity);

            PersonaEntity saved = personaRepository.save(entity);
            PersonaDTO savedDTO = personaMapper.entityToDto(saved);

            String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
            int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

            return ResponseEntity.status(statusCode).body(
                ResponseDTO.builder()
                    .success(true)
                    .message(message)
                    .code(statusCode)
                    .response(savedDTO)
                    .build()
            );

        } catch (Exception e) {
            log.error("Error guardando persona", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.SAVE_ERROR)
                    .code(HttpStatus.BAD_REQUEST.value())
                    .build()
            );
        }
    }

    private ResponseEntity<ResponseDTO> validatePersonaData(PersonaDTO personaDTO, boolean isUpdate) {
        if (!isUpdate) {
            if (personaDTO.getNumeroCedula() != null && personaRepository.existsByNumeroCedula(personaDTO.getNumeroCedula())) {
                return buildConflictResponse(Constantes.NUMBER_EXISTS);
            }
            if (personaDTO.getNombre() != null && personaRepository.existsByNombre(personaDTO.getNombre())) {
                return buildConflictResponse(Constantes.PERSON_EXISTS);
            }
        }
        return null;
    }

    private PersonaEntity buildPersonaEntity(PersonaDTO personaDTO, boolean isUpdate) {
        PersonaEntity entity;

        if (isUpdate) {
            entity = personaRepository.findById(personaDTO.getId()).orElseThrow();
            personaMapper.updateEntityFromDto(personaDTO, entity);
            entity.setFechaModificacion(new Date());
            entity.setUsuarioModificacion(personaDTO.getUsuarioModificacion());
        } else {
            entity = personaMapper.dtoToEntity(personaDTO);
            entity.setFechaCreacion(new Date());
            entity.setUsuarioCreacion(personaDTO.getUsuarioCreacion());
            entity.setActivo(true);
        }
        return entity;
    }

    private void setRelacionDireccion(PersonaDTO personaDTO, PersonaEntity entity) {
        if (personaDTO.getDireccion() != null && personaDTO.getDireccion().getId() != null) {
            DireccionEntity direccion = direccionRepository.findById(personaDTO.getDireccion().getId())
                .orElseThrow(() -> new RuntimeException(Constantes.DIREC_NOT_FOUND));
            entity.setDireccion(direccion);
        }
    }

    private void setRelacionTipoDocumento(PersonaDTO personaDTO, PersonaEntity entity) {
        if (personaDTO.getTipoDocumento() != null && personaDTO.getTipoDocumento().getId() != null) {
            TipoDocumentoEntity tipoDocumento = tipoDocumentoRepository.findById(personaDTO.getTipoDocumento().getId())
                .orElseThrow(() -> new RuntimeException(Constantes.TD_NOT_FOUND));
            entity.setTipoDocumento(tipoDocumento);
        }
    }

    private ResponseEntity<ResponseDTO> buildConflictResponse(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ResponseDTO.builder()
                .success(false)
                .message(message)
                .code(HttpStatus.CONFLICT.value())
                .build()
        );
    }

    @Override
    @Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
	    log.info("Buscar persona por id: {}", id);
	    try {
	        Optional<PersonaEntity> persona = personaRepository.findById(id);
	        if (persona.isPresent()) {
	            PersonaDTO dto = personaMapper.entityToDto(persona.get());
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
	        log.error("Error al buscar la persona por id: {}", id, e);
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
        log.info("Listar todas las personas");
        try {
            var list = personaRepository.findAll();
            var dtoList = personaMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar las personas", e);
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
        log.info("Inicio método para eliminar persona por id: {}", id);
        try {
            if (!personaRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            personaRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar la persona con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}
