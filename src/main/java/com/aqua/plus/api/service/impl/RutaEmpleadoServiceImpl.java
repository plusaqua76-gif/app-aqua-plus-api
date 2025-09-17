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

import com.aqua.plus.api.service.IRutaEmpleadoService;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.RutaEmpleadoDTO;
import com.aqua.plus.commons.entities.RutaEmpleadoEntity;
import com.aqua.plus.commons.maps.RutaEmpleadoMapper;
import com.aqua.plus.commons.repositories.RutaEmpleadoRepository;
import com.aqua.plus.commons.utils.Constantes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RutaEmpleadoServiceImpl implements IRutaEmpleadoService{
	
	private final RutaEmpleadoRepository rutaEmpleadoRepository;
	private final RutaEmpleadoMapper rutaEmpleadoMapper;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;
	
	@Override
	@Transactional
    public ResponseEntity<ResponseDTO> save(RutaEmpleadoDTO rutaEmpleadoDTO) {
        log.info("Creando Ruta Empleado");
        try {
        	RutaEmpleadoEntity entity = rutaEmpleadoMapper.dtoToEntity(rutaEmpleadoDTO);
            entity.setFechaCreacion(new Date());
            entity.setUsuarioCreacion(rutaEmpleadoDTO.getUsuarioCreacion());
            entity.setActivo(true);

            RutaEmpleadoEntity saved = rutaEmpleadoRepository.save(entity);
            RutaEmpleadoDTO savedDTO = rutaEmpleadoMapper.entityToDto(saved);

            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.SAVED_SUCCESSFULLY)
                    .code(HttpStatus.CREATED.value())
                    .response(savedDTO)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        } catch (Exception e) {
            log.error("Error creando la Ruta Empleado", e);
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
    public ResponseEntity<ResponseDTO> update(RutaEmpleadoDTO rutaEmpleadoDTO) {
        log.info("Actualizando Ruta Empleado");
        try {
            if (rutaEmpleadoDTO.getId() == null || !rutaEmpleadoRepository.existsById(rutaEmpleadoDTO.getId())) {
                throw new IllegalArgumentException(Constantes.RUT_NOT_FOUND);
            }

            RutaEmpleadoEntity entity = rutaEmpleadoRepository.findById(rutaEmpleadoDTO.getId()).orElseThrow();
            rutaEmpleadoMapper.updateEntityFromDto(rutaEmpleadoDTO, entity); 
            entity.setFechaModificacion(new Date());
            entity.setUsuarioModificacion(rutaEmpleadoDTO.getUsuarioModificacion());

            RutaEmpleadoEntity updated = rutaEmpleadoRepository.save(entity);
            RutaEmpleadoDTO updatedDTO = rutaEmpleadoMapper.entityToDto(updated);

            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.UPDATED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(updatedDTO)
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
        } catch (Exception e) {
            log.error("Error actualizando la Ruta Empleado", e);
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
	    log.info("Buscar Ruta Empleado por id: {}", id);
	    try {
	        Optional<RutaEmpleadoEntity> rutaEmpleado = rutaEmpleadoRepository.findById(id);
	        if (rutaEmpleado.isPresent()) {
	        	RutaEmpleadoDTO dto = rutaEmpleadoMapper.entityToDto(rutaEmpleado.get());
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
	        log.error("Error al buscar  Ruta Empleado por id: {}", id, e);
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
        log.info("Listar todos las Ruta Empleado");
        try {
            var list = rutaEmpleadoRepository.findAll();
            var dtoList = rutaEmpleadoMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar las Ruta Empleado", e);
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
        log.info("Inicio método para eliminar Ruta Empleado por id: {}", id);
        try {
            if (!rutaEmpleadoRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            rutaEmpleadoRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar Ruta Empleado con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
    
    /**
     * Sincroniza datos de rutas asignadas a un lector usando su ID.
     * Llama un SP en PostgreSQL que retorna estructura JSON con empresas, clientes y lecturas.
     * 
     * @author nicope
     * @version 1.0
     */
    @Transactional
    public Map<String, Object> syncLectorData(Integer idPersona, Integer offset, Integer limit) {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM public.sync_lector_data(:idPersona");

            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue("idPersona", idPersona);

            if (offset != null && limit != null) {
                sql.append(", :offset, :limit)");
                parameters.addValue("offset", offset);
                parameters.addValue("limit", limit);
            } else {
                sql.append(")");
            }

            Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql.toString(), parameters);

            Object wrappedValue = rawResult.get("sync_lector_data");

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
    
    /**
     * Sincroniza datos de rutas asignadas a un lector usando su ID.
     * Llama un SP en PostgreSQL que retorna estructura JSON con empresas, clientes y lecturas.
     * 
     * @author nicope
     * @version 1.0
     */
    @Transactional(readOnly = true)
    public Map<String, Object> syncConfigEnterprise(Integer idPersona, Integer offset, Integer limit) {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM public.sync_config_empresa(:idPersona");

            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue("idPersona", idPersona);

            if (offset != null && limit != null) {
                sql.append(", :offset, :limit)");
                parameters.addValue("offset", offset);
                parameters.addValue("limit", limit);
            } else {
                sql.append(")");
            }

            Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql.toString(), parameters);

            Object wrappedValue = rawResult.get("sync_config_empresa");

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

}
