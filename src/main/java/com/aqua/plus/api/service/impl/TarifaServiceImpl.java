package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.ITarifaService;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.TarifaDTO;
import com.aqua.plus.commons.entities.EmpresaEntity;
import com.aqua.plus.commons.entities.TarifaEntity;
import com.aqua.plus.commons.entities.TipoTarifaEntity;
import com.aqua.plus.commons.maps.TarifaMapper;
import com.aqua.plus.commons.repositories.EmpresaRepository;
import com.aqua.plus.commons.repositories.TarifaRepository;
import com.aqua.plus.commons.repositories.TipoTarifaRepository;
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
public class TarifaServiceImpl implements ITarifaService{

	private final TarifaRepository tarifaRepository;
	private final EmpresaRepository empresaRepository;
	private final TipoTarifaRepository tipoTarifaRepository;
	private final TarifaMapper tarifaMapper;
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(TarifaDTO tarifaDTO) {
	    log.info("Guardar/Actualizar tarifa");
	    try {
	        boolean isUpdate = tarifaDTO.getId() != null && tarifaRepository.existsById(tarifaDTO.getId());
	        if (!isUpdate
	            && tarifaDTO.getEmpresa() != null && tarifaDTO.getEmpresa().getId() != null
	            && tarifaDTO.getTipoTarifa() != null && tarifaDTO.getTipoTarifa().getId() != null
	            && tarifaRepository.existsByEmpresaIdAndTipoTarifaId(
	                tarifaDTO.getEmpresa().getId(), tarifaDTO.getTipoTarifa().getId())
	        ) {
	            ResponseDTO errorResponse = ResponseDTO.builder()
	                    .success(false)
	                    .message(Constantes.RATE_TYPE_ALREADY_EXISTS)
	                    .code(HttpStatus.CONFLICT.value())
	                    .build();
	            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
	        }

	        TarifaEntity entity;

	        if (isUpdate) {
	            entity = tarifaRepository.findById(tarifaDTO.getId()).orElseThrow();
	            tarifaMapper.updateEntityFromDto(tarifaDTO, entity);
	            entity.setFechaModificacion(new Date());
	            entity.setUsuarioModificacion(tarifaDTO.getUsuarioModificacion());
	        } else {
	            entity = tarifaMapper.dtoToEntity(tarifaDTO);
	            entity.setFechaCreacion(new Date());
	            entity.setUsuarioCreacion(tarifaDTO.getUsuarioCreacion());
	            entity.setActivo(true);
	        }

	        if (tarifaDTO.getEmpresa() != null && tarifaDTO.getEmpresa().getId() != null) {
	            EmpresaEntity empresa = empresaRepository.findById(tarifaDTO.getEmpresa().getId())
	                    .orElseThrow(() -> new RuntimeException(Constantes.EMP_NOT_FOUND));
	            entity.setEmpresa(empresa);
	        }
	        if (tarifaDTO.getTipoTarifa() != null && tarifaDTO.getTipoTarifa().getId() != null) {
	            TipoTarifaEntity tipoTarifa = tipoTarifaRepository.findById(tarifaDTO.getTipoTarifa().getId())
	                    .orElseThrow(() -> new RuntimeException(Constantes.TIP_NOT_FOUND));
	            entity.setTipoTarifa(tipoTarifa);
	        }

	        TarifaEntity saved = tarifaRepository.save(entity);
	        TarifaDTO savedDTO = tarifaMapper.entityToDto(saved);

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
	        log.error("Error guardando tarifa", e);
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
	    log.info("Buscar tarifa por id: {}", id);
	    try {
	        Optional<TarifaEntity> tarifa = tarifaRepository.findById(id);
	        if (tarifa.isPresent()) {
	            TarifaDTO dto = tarifaMapper.entityToDto(tarifa.get());
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
	        log.error("Error al buscar la tarifa por id: {}", id, e);
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
    public ResponseEntity<ResponseDTO> findByEmpresaId(Integer empresaId) {
        log.info("Buscar tarifas por id de empresa: {}", empresaId);
        try {
            List<TarifaEntity> tarifas = tarifaRepository.findByEmpresaId(empresaId);

            if (!tarifas.isEmpty()) {
                List<TarifaDTO> dtos = tarifas.stream()
                        .map(tarifaMapper::entityToDto)
                        .toList();

                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(true)
                        .message(Constantes.CONSULTED_SUCCESSFULLY)
                        .code(HttpStatus.OK.value())
                        .response(dtos)
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
            log.error("Error al buscar tarifas por id de empresa: {}", empresaId, e);
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
        log.info("Listar todas las tarifas");
        try {
            var list = tarifaRepository.findAll();
            var dtoList = tarifaMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar las tarifas", e);
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
        log.info("Inicio método para eliminar tarifa por id: {}", id);
        try {
            if (!tarifaRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            tarifaRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar la tarifa con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}
