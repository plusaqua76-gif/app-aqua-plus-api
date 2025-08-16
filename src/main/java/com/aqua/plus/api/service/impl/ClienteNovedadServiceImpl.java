package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IClienteNovedadService;
import com.aqua.plus.commons.dtos.ClienteNovedadDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ClienteNovedadEntity;
import com.aqua.plus.commons.maps.ClienteNovedadMapper;
import com.aqua.plus.commons.repositories.ClienteNovedadRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteNovedadServiceImpl implements IClienteNovedadService{
	
	private final ClienteNovedadRepository clienteNovedadRepository;
	private final ClienteNovedadMapper clienteNovedadMapper;
	
	@Override
	@Transactional
    public ResponseEntity<ResponseDTO> save(ClienteNovedadDTO clienteNovedadDTO) {
        log.info("Inicio metodo crear Cliente Novedad");
        try {
        	ClienteNovedadEntity entity = clienteNovedadMapper.dtoToEntity(clienteNovedadDTO);
            entity.setFechaCreacion(new Date());
            entity.setUsuarioCreacion(clienteNovedadDTO.getUsuarioCreacion());
            entity.setActivo(true);

            ClienteNovedadEntity saved = clienteNovedadRepository.save(entity);
            ClienteNovedadDTO savedDTO = clienteNovedadMapper.entityToDto(saved);

            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.SAVED_SUCCESSFULLY)
                    .code(HttpStatus.CREATED.value())
                    .response(savedDTO)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        } catch (Exception e) {
            log.error("Error creando el Cliente Novedad ", e);
            ResponseDTO errorResponse = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.SAVE_ERROR)
                    .code(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    
    @Override
    @Transactional
    public ResponseEntity<ResponseDTO> update(ClienteNovedadDTO clienteNovedadDTO) {
        log.info("inicio metodo Actualizando Cliente Novedad");
        try {
            if (clienteNovedadDTO.getId() == null || !clienteNovedadRepository.existsById(clienteNovedadDTO.getId())) {
                throw new IllegalArgumentException(Constantes.CLIENT_NOT_EXIST);
            }

            ClienteNovedadEntity entity = clienteNovedadRepository.findById(clienteNovedadDTO.getId()).orElseThrow();
            clienteNovedadMapper.updateEntityFromDto(clienteNovedadDTO, entity); 
            entity.setFechaModificacion(new Date());
            entity.setUsuarioModificacion(clienteNovedadDTO.getUsuarioModificacion());

            ClienteNovedadEntity updated = clienteNovedadRepository.save(entity);
            ClienteNovedadDTO updatedDTO = clienteNovedadMapper.entityToDto(updated);

            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.UPDATED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(updatedDTO)
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
        } catch (Exception e) {
            log.error("Error actualizando el Cliente Novedad", e);
            ResponseDTO errorResponse = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.UPDATE_ERROR)
                    .code(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
	    log.info("Buscar Cliente Novedad por id: {}", id);
	    try {
	        Optional<ClienteNovedadEntity> clienteNovedad = clienteNovedadRepository.findById(id);
	        if (clienteNovedad.isPresent()) {
	        	ClienteNovedadDTO dto = clienteNovedadMapper.entityToDto(clienteNovedad.get());
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
	        log.error("Error al buscar  el Cliente Novedad por id: {}", id, e);
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
        log.info("Listar todos los Cliente Novedad");
        try {
            var list = clienteNovedadRepository.findAll();
            var dtoList = clienteNovedadMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar los Cliente Novedad", e);
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
        log.info("Inicio método para eliminar Cliente Novedad por id: {}", id);
        try {
            if (!clienteNovedadRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            clienteNovedadRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar Cliente Novedad con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}
