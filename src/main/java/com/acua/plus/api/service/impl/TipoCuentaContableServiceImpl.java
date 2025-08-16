package com.acua.plus.api.service.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acua.plus.api.service.ITipoCuentaContableService;
import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.dtos.TipoCuentaContableDTO;
import com.acua.plus.commons.entities.TipoCuentaContableEntity;
import com.acua.plus.commons.maps.TipoCuentaContableMapper;
import com.acua.plus.commons.repositories.TipoCuentaContableRepository;
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
public class TipoCuentaContableServiceImpl implements ITipoCuentaContableService {

	private final TipoCuentaContableRepository tipoCuentaRepository;
	private final TipoCuentaContableMapper tipoCuentaMapper;
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(TipoCuentaContableDTO tipoCuentaDTO) {
	    log.info("Guardar/Actualizar Tipo Cuenta Contable");
	    try {
	        boolean isUpdate = tipoCuentaDTO.getId() != null && tipoCuentaRepository.existsById(tipoCuentaDTO.getId());
	        TipoCuentaContableEntity entity;

	        if (isUpdate) {
	            entity = tipoCuentaRepository.findById(tipoCuentaDTO.getId()).orElseThrow();
	            tipoCuentaMapper.updateEntityFromDto(tipoCuentaDTO, entity);
	            entity.setFechaModificacion(new Date());
	            entity.setUsuarioModificacion(tipoCuentaDTO.getUsuarioModificacion());
	        } else {
	            entity = tipoCuentaMapper.dtoToEntity(tipoCuentaDTO);
	            entity.setFechaCreacion(new Date());
	            entity.setUsuarioCreacion(tipoCuentaDTO.getUsuarioCreacion());
	            entity.setActivo(true);
	        }

	        TipoCuentaContableEntity saved = tipoCuentaRepository.save(entity);
	        TipoCuentaContableDTO savedDTO = tipoCuentaMapper.entityToDto(saved);

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
	        log.error("Error guardando el tipo de contador", e);
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
	    log.info("Buscar tipo cuenta contador por id: {}", id);
	    try {
	        Optional<TipoCuentaContableEntity> tipoCuenta = tipoCuentaRepository.findById(id);
	        if (tipoCuenta.isPresent()) {
	        	TipoCuentaContableDTO dto = tipoCuentaMapper.entityToDto(tipoCuenta.get());
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
	        log.error("Error al buscar tipo cuenta contador por id: {}", id, e);
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
        log.info("Listar todos los tipos de cuentas contables");
        try {
            var list = tipoCuentaRepository.findAll();
            var dtoList = tipoCuentaMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar los tipos de cuentas contables", e);
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
        log.info("Inicio método para eliminar tipo de cuenta contable por id: {}", id);
        try {
            if (!tipoCuentaRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            tipoCuentaRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar el tipo cuenta contable con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}
