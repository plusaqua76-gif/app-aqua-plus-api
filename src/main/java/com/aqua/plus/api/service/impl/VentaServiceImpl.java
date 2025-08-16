package com.aqua.plus.api.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.postgresql.util.PGobject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IVentaService;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.VentaDTO;
import com.aqua.plus.commons.entities.VentaEntity;
import com.aqua.plus.commons.maps.VentaMapper;
import com.aqua.plus.commons.repositories.VentaRepository;
import com.aqua.plus.commons.utils.Constantes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
public class VentaServiceImpl implements IVentaService{

	private final VentaRepository ventaRepository;
	private final VentaMapper ventaMapper;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(VentaDTO ventaDTO) {
	    log.info("Guardar/Actualizar Venta ");
	    try {
	        boolean isUpdate = ventaDTO.getId() != null && ventaRepository.existsById(ventaDTO.getId());
	        VentaEntity entity;

	        if (isUpdate) {
	            entity = ventaRepository.findById(ventaDTO.getId()).orElseThrow();
	            ventaMapper.updateEntityFromDto(ventaDTO, entity);
	            entity.setFechaModificacion(new Date());
	            entity.setUsuarioModificacion(ventaDTO.getUsuarioModificacion());
	        } else {
	            entity = ventaMapper.dtoToEntity(ventaDTO);
	            entity.setFechaCreacion(new Date());
	            entity.setUsuarioCreacion(ventaDTO.getUsuarioCreacion());
	            entity.setActivo(true);
	        }

	        VentaEntity saved = ventaRepository.save(entity);
	        VentaDTO savedDTO = ventaMapper.entityToDto(saved);

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
	        log.error("Error guardando la venta", e);
	        ResponseDTO errorResponse = ResponseDTO.builder()
	                .success(false)
	                .message(Constantes.SAVE_ERROR)
	                .code(HttpStatus.BAD_REQUEST.value())
	                .build();

	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	    }
	}
	
	@Transactional
    public Map<String, Object> crearVenta(Map<String, Object> jsonParams) {
        try {
            String jsonString = objectMapper.writeValueAsString(jsonParams);

            String sql = "SELECT * FROM public.crear_venta(CAST(:jsonData AS jsonb))";
            
            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue("jsonData", jsonString);

            Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

          
            Object wrappedValue = rawResult.get("crear_venta");
            if (wrappedValue instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {
                String jsonValue = pgObject.getValue();
                
                return objectMapper.readValue(jsonValue, new TypeReference<Map<String, Object>>() {});
            }

            return Map.of(Constantes.ERROR_KEY, Constantes.RESULT_COULD_NOT_PROCESSED);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Collections.singletonMap(Constantes.ERROR_KEY, Constantes.PROCCESSING_ERROR + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.singletonMap(Constantes.ERROR_KEY, Constantes.UNEXPECTED_ERROR + e.getMessage());
        }
    }
	
	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
	    log.info("Buscar venta por id: {}", id);
	    try {
	        Optional<VentaEntity> producto = ventaRepository.findById(id);
	        if (producto.isPresent()) {
	        	VentaDTO dto = ventaMapper.entityToDto(producto.get());
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
	        log.error("Error al buscar venta por id: {}", id, e);
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
        log.info("Listar todas las ventas");
        try {
            var list = ventaRepository.findAll();
            var dtoList = ventaMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar las ventas", e);
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
        log.info("Inicio método para eliminar venta por id: {}", id);
        try {
            if (!ventaRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            ventaRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar cuenta con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}
