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

import com.aqua.plus.api.service.ILecturaService;
import com.aqua.plus.commons.dtos.LecturaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ContadorEntity;
import com.aqua.plus.commons.entities.LecturaEntity;
import com.aqua.plus.commons.maps.LecturaMapper;
import com.aqua.plus.commons.repositories.ContadorRepository;
import com.aqua.plus.commons.repositories.LecturaRepository;
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
public class LecturaServiceImpl implements ILecturaService {

	private final LecturaRepository lecturaRepository;
	private final ContadorRepository contadorRepository;
	private final LecturaMapper lecturaMapper;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(LecturaDTO lecturaDTO) {
	    log.info("Guardar/Actualizar lectura");
	    try {
	        boolean isUpdate = lecturaDTO.getId() != null && lecturaRepository.existsById(lecturaDTO.getId());
	        LecturaEntity entity;
	        log.info("existe id lectura:{} ", lecturaDTO.getId());
	        if (isUpdate) {
	            entity = lecturaRepository.findById(lecturaDTO.getId()).orElseThrow();
	            lecturaMapper.updateEntityFromDto(lecturaDTO, entity);
	            entity.setFechaModificacion(new Date());
	            entity.setUsuarioModificacion(lecturaDTO.getUsuarioModificacion());
	        } else {
	            entity = lecturaMapper.dtoToEntity(lecturaDTO);
	            entity.setFechaLectura(new Date());
	            entity.setFechaCreacion(new Date());
	            entity.setUsuarioCreacion(lecturaDTO.getUsuarioCreacion());
	            entity.setActivo(true);
	        }
	        
	        if (lecturaDTO.getContador() != null && lecturaDTO.getContador().getId() != null) {
	            ContadorEntity contador = contadorRepository
	                .findById(lecturaDTO.getContador().getId())
	                .orElseThrow(() -> new RuntimeException(Constantes.CON_NOT_FOUND));
	            entity.setContador(contador);
	        }

	        LecturaEntity saved = lecturaRepository.save(entity);
	        LecturaDTO savedDTO = lecturaMapper.entityToDto(saved);

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
	        log.error("Error guardando lectura", e);
	        ResponseDTO errorResponse = ResponseDTO.builder()
	            .success(false)
	            .message(Constantes.SAVE_ERROR)
	            .code(HttpStatus.BAD_REQUEST.value())
	            .build();

	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	    }
	}
	
	/**
     * Guarda una lectura a partir de los parámetros recibidos en formato JSON.
     * Llama a un procedimiento almacenado en PostgreSQL y devuelve el resultado deserializado.
     * 
     * @author nicope
     * @version 1.0
     */
	@Transactional
    public Map<String, Object> guardarLectura(Map<String, Object> jsonParams) {
        try {
            String jsonString = objectMapper.writeValueAsString(jsonParams);

            String sql = "SELECT * FROM public.registrar_lectura(CAST(:jsonData AS jsonb))";
            
            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue("jsonData", jsonString);

            Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

            // Obtener el campo "value" del resultado
            Object wrappedValue = rawResult.get("registrar_lectura");
            if (wrappedValue instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {
                String jsonValue = pgObject.getValue();
                // Deserializar a Map
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
	    log.info("Buscar lectura por id: {}", id);
	    try {
	        Optional<LecturaEntity> lectura = lecturaRepository.findById(id);
	        if (lectura.isPresent()) {
	        	LecturaDTO dto = lecturaMapper.entityToDto(lectura.get());
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
	        log.error("Error al buscar lectura por id: {}", id, e);
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
        log.info("Listar todas las lecturas");
        try {
            var list = lecturaRepository.findAll();
            var dtoList = lecturaMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar las lecturas", e);
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
        log.info("Inicio método para eliminar lectura por id: {}", id);
        try {
            if (!lecturaRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            lecturaRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar la lectura con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}
