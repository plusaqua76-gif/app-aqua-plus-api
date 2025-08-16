package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IRolService;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.RolDTO;
import com.aqua.plus.commons.entities.RolEntity;
import com.aqua.plus.commons.maps.RolMapper;
import com.aqua.plus.commons.repositories.RolRepository;
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
public class RolServiceImpl implements IRolService {

	private final RolRepository rolRepository;
	private final RolMapper rolMapper;
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(RolDTO rolDTO) {
	    log.info("Inicio guardar/actualizar rol");
	    try {
	        boolean isUpdate = rolDTO.getId() != null && rolRepository.existsById(rolDTO.getId());
	        RolEntity rolEntity;

	        if (isUpdate) {
	            rolEntity = rolRepository.findById(rolDTO.getId()).orElseThrow();
	            rolMapper.updateEntityFromDto(rolDTO, rolEntity);
	            rolEntity.setFechaModificacion(new Date());
	            rolEntity.setUsuarioModificacion(rolDTO.getUsuarioModificacion());
	        } else {
	            rolEntity = rolMapper.dtoToEntity(rolDTO);
	            rolEntity.setFechaCreacion(new Date());
	            rolEntity.setUsuarioCreacion(rolDTO.getUsuarioCreacion());
	            rolEntity.setActivo(true);
	        }

	        RolEntity savedEntity = rolRepository.save(rolEntity);
	        RolDTO savedDTO = rolMapper.entityToDto(savedEntity);

	        log.info("Fin guardar/actualizar rol");

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
	        log.error("Error al guardar/actualizar rol", e);
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
	public ResponseEntity<ResponseDTO> findRolById(Integer id) {
	    log.info("Inicio del método para obtener el rol por id: {}", id);

	    Optional<RolEntity> rolOptional = rolRepository.findById(id);

	    if (rolOptional.isPresent()) {
	        RolDTO rolDTO = rolMapper.entityToDto(rolOptional.get());
	        ResponseDTO responseDTO = ResponseDTO.builder()
	                .success(true)
	                .message(Constantes.CONSULTED_SUCCESSFULLY)
	                .code(HttpStatus.OK.value())
	                .response(rolDTO)
	                .build();
	        return ResponseEntity.ok(responseDTO);
	    } else {
	        String notFoundMsg = String.format(Constantes.ROL_NOT_FOUND, id);
	        log.warn(notFoundMsg);
	        ResponseDTO responseDTO = ResponseDTO.builder()
	                .success(false)
	                .code(HttpStatus.NOT_FOUND.value())
	                .message(notFoundMsg)
	                .response(null)
	                .build();
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
	    }
	}
	
	@Override
	@Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findAll() {
        log.info("Inicio método para obtener todos los roles");
        try {
            var rolEntities = rolRepository.findAll();
            var rolDTOList = rolMapper.listEntityToDtoList(rolEntities);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(rolDTOList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al obtener todos los roles", e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.CONSULTING_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .response(Constantes.NO_RECORD_FOUND)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> delete(Integer id) {
	    log.info("Inicio eliminar rol por id: {}", id);
	    try {
	        if (!rolRepository.existsById(id)) {
	            String notFoundMsg = String.format(Constantes.ROL_NOT_FOUND, id);
	            log.warn(notFoundMsg);
	            ResponseDTO responseDTO = ResponseDTO.builder()
	                    .success(false)
	                    .code(HttpStatus.NOT_FOUND.value())
	                    .message(notFoundMsg)
	                    .response(null)
	                    .build();
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
	        }

	        rolRepository.deleteById(id);
	        log.info("Rol eliminado correctamente para el Id: {}", id);

	        ResponseDTO responseDTO = ResponseDTO.builder()
	                .success(true)
	                .message(Constantes.DELETED_SUCCESSFULLY)
	                .code(HttpStatus.OK.value())
	                .response(null)
	                .build();

	        return ResponseEntity.ok(responseDTO);

	    } catch (Exception e) {
	        log.error("Error al eliminar rol", e);
	        ResponseDTO errorResponse = ResponseDTO.builder()
	                .success(false)
	                .message(Constantes.DELETE_ERROR)
	                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
	                .response(null)
	                .build();

	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	    }
	}
}
