package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IDireccionService;
import com.aqua.plus.commons.dtos.DireccionDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.CiudadEntity;
import com.aqua.plus.commons.entities.CorregimientoEntity;
import com.aqua.plus.commons.entities.DepartamentoEntity;
import com.aqua.plus.commons.entities.DireccionEntity;
import com.aqua.plus.commons.maps.DireccionMapper;
import com.aqua.plus.commons.repositories.CiudadRepository;
import com.aqua.plus.commons.repositories.CorregimientoRepository;
import com.aqua.plus.commons.repositories.DepartamentoRepository;
import com.aqua.plus.commons.repositories.DireccionRepository;
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
public class DireccionServiceImpl implements IDireccionService {

	private final DireccionRepository direccionRepository;
	private final CorregimientoRepository corregimientoRepository;
	private final DepartamentoRepository departamentoRepository;
	private final CiudadRepository ciudadRepository;
	private final DireccionMapper direccionMapper;
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(DireccionDTO direccionDTO) {
	    log.info("Guardar/Actualizar direccion");
	    try {
	        boolean isUpdate = direccionDTO.getId() != null && direccionRepository.existsById(direccionDTO.getId());
	        DireccionEntity entity;
	        log.info("exite id direccion:{} ",direccionDTO.getId());
	        if (isUpdate) {
	            entity = direccionRepository.findById(direccionDTO.getId()).orElseThrow();
	            direccionMapper.updateEntityFromDto(direccionDTO, entity);
	            entity.setFechaModificacion(new Date());
	            entity.setUsuarioModificacion(direccionDTO.getUsuarioModificacion());
	        } else {
	            entity = direccionMapper.dtoToEntity(direccionDTO);
	            entity.setFechaCreacion(new Date());
	            entity.setUsuarioCreacion(direccionDTO.getUsuarioCreacion());
	            entity.setActivo(true);
	        }
	        
	        if (direccionDTO.getDepartamento() != null && direccionDTO.getDepartamento().getId() != null) {
	            DepartamentoEntity departamento = departamentoRepository
	                .findById(direccionDTO.getDepartamento().getId())
	                .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));
	            entity.setDepartamento(departamento);
	        }
	        
	        if (direccionDTO.getCiudad() != null && direccionDTO.getCiudad().getId() != null) {
	            CiudadEntity ciudad = ciudadRepository
	                .findById(direccionDTO.getCiudad().getId())
	                .orElseThrow(() -> new RuntimeException("Ciudad no encontrado"));
	            entity.setCiudad(ciudad);
	        }

	        if (direccionDTO.getCorregimiento() != null && direccionDTO.getCorregimiento().getId() != null) {
	            CorregimientoEntity corregimiento = corregimientoRepository
	                .findById(direccionDTO.getCorregimiento().getId())
	                .orElseThrow(() -> new RuntimeException("Corregimiento no encontrado"));
	            entity.setCorregimiento(corregimiento);
	        }

	        DireccionEntity saved = direccionRepository.save(entity);
	        DireccionDTO savedDTO = direccionMapper.entityToDto(saved);

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
	        log.error("Error guardando dirección", e);
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
	    log.info("Buscar direccion por id: {}", id);
	    try {
	        Optional<DireccionEntity> direccion = direccionRepository.findById(id);
	        if (direccion.isPresent()) {
	        	DireccionDTO dto = direccionMapper.entityToDto(direccion.get());
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
	        log.error("Error al buscar direccion por id: {}", id, e);
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
        log.info("Listar todas las direcciones");
        try {
            var list = direccionRepository.findAll();
            var dtoList = direccionMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar las direcciones", e);
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
        log.info("Inicio método para eliminar direccion por id: {}", id);
        try {
            if (!direccionRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            direccionRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar la direccion con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}
