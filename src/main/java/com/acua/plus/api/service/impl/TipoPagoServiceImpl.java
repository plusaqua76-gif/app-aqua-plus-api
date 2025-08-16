package com.acua.plus.api.service.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acua.plus.api.service.ITipoPagoService;
import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.dtos.TipoPagoDTO;
import com.acua.plus.commons.entities.TipoPagoEntity;
import com.acua.plus.commons.maps.TipoPagoMapper;
import com.acua.plus.commons.repositories.TipoPagoRepository;
import com.acua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TipoPagoServiceImpl implements ITipoPagoService{
	
	private final TipoPagoRepository tipoPagoRepository;
	private final TipoPagoMapper tipoPagoMapper;
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(TipoPagoDTO tipoPagoDTO) {
	    log.info("Guardar/Actualizar Tipo de Pago");
	    try {
	        boolean isUpdate = tipoPagoDTO.getId() != null && tipoPagoRepository.existsById(tipoPagoDTO.getId());
	        TipoPagoEntity entity;

	        if (isUpdate) {
	            entity = tipoPagoRepository.findById(tipoPagoDTO.getId()).orElseThrow();
	            tipoPagoMapper.updateEntityFromDto(tipoPagoDTO, entity);
	            entity.setFechaModificacion(new Date());
	            entity.setUsuarioModificacion(tipoPagoDTO.getUsuarioModificacion());
	        } else {
	            entity = tipoPagoMapper.dtoToEntity(tipoPagoDTO);
	            entity.setFechaCreacion(new Date());
	            entity.setUsuarioCreacion(tipoPagoDTO.getUsuarioCreacion());
	            entity.setActivo(true);
	        }

	        TipoPagoEntity saved = tipoPagoRepository.save(entity);
	        TipoPagoDTO savedDTO = tipoPagoMapper.entityToDto(saved);

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
	        log.error("Error guardando el tipo de pago", e);
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
	    log.info("Buscar tipo de pago por id: {}", id);
	    try {
	        Optional<TipoPagoEntity> tipoPago = tipoPagoRepository.findById(id);
	        if (tipoPago.isPresent()) {
	        	TipoPagoDTO dto = tipoPagoMapper.entityToDto(tipoPago.get());
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
	        log.error("Error al buscar tipo pago por id: {}", id, e);
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
        log.info("Listar todos los tipos de pago");
        try {
            var list = tipoPagoRepository.findAll();
            var dtoList = tipoPagoMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar los tipos de pago", e);
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
        log.info("Inicio método para eliminar tipo de pago por id: {}", id);
        try {
            if (!tipoPagoRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            tipoPagoRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar el tipo de pago con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}
