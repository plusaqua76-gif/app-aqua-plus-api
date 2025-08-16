package com.acua.plus.api.service.impl;

import java.util.Map;
import java.util.Optional;

import org.postgresql.util.PGobject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acua.plus.api.service.IEmpleadoEmpresaService;
import com.acua.plus.commons.dtos.EmpleadoEmpresaResponseDTO;
import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.entities.EmpleadoEmpresaEntity;
import com.acua.plus.commons.maps.EmpleadoEmpresaMapper;
import com.acua.plus.commons.repositories.EmpleadoEmpresaRepository;
import com.acua.plus.commons.utils.Constantes;
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
public class EmpleadoEmpresaServiceImpl implements IEmpleadoEmpresaService {

	private final EmpleadoEmpresaRepository empleadoEmpresaRepository;
	private final EmpleadoEmpresaMapper empleadoEmpresaMapper;
	private final ObjectMapper objectMapper;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Transactional
	public Map<String, Object> save(Map<String, Object> jsonParams) {
		try {
			String jsonString = objectMapper.writeValueAsString(jsonParams);

			String sql = "SELECT * FROM public.guardar_empleado_completo(CAST(:jsonData AS jsonb))";

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("jsonData", jsonString);

			Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

			Object wrappedValue = rawResult.get("guardar_empleado_completo");
			if (wrappedValue instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {
				String jsonValue = pgObject.getValue();
				return objectMapper.readValue(jsonValue, new TypeReference<Map<String, Object>>() {
				});
			}

			return Map.of("error", "El resultado no pudo ser procesado correctamente.");

		} catch (JsonProcessingException e) {
			log.error("Error de procesamiento JSON", e);
			return Map.of("error", "Error de procesamiento JSON: " + e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado en guardarEmpleado", e);
			return Map.of("error", "Error inesperado: " + e.getMessage());
		}
	}

	@Transactional
	public Map<String, Object> update(Map<String, Object> jsonParams) {
		try {
			String jsonString = objectMapper.writeValueAsString(jsonParams);

			String sql = "SELECT * FROM public.actualizar_empleado(CAST(:jsonData AS jsonb))";

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("jsonData", jsonString);

			Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

			Object wrappedValue = rawResult.get("actualizar_empleado");
			if (wrappedValue instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {
				String jsonValue = pgObject.getValue();
				return objectMapper.readValue(jsonValue, new TypeReference<Map<String, Object>>() {
				});
			}

			return Map.of("error", "El resultado no pudo ser procesado correctamente.");

		} catch (JsonProcessingException e) {
			log.error("Error de procesamiento JSON", e);
			return Map.of("error", "Error de procesamiento JSON: " + e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado en actualizarEmpleado", e);
			return Map.of("error", "Error inesperado: " + e.getMessage());
		}
	}

	@Transactional
	public Map<String, Object> actualizarEstadoPersona(Map<String, Object> jsonParams) {
		try {
			String jsonString = objectMapper.writeValueAsString(jsonParams);

			String sql = "SELECT * FROM public.actualizar_estado_por_persona(CAST(:jsonData AS jsonb))";

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("jsonData", jsonString);

			Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

			Object wrappedValue = rawResult.get("actualizar_estado_por_persona");
			if (wrappedValue instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {
				String jsonValue = pgObject.getValue();
				return objectMapper.readValue(jsonValue, new TypeReference<Map<String, Object>>() {
				});
			}

			return Map.of("error", "El resultado no pudo ser procesado correctamente.");

		} catch (JsonProcessingException e) {
			log.error("Error de procesamiento JSON", e);
			return Map.of("error", "Error de procesamiento JSON: " + e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado en actualizarEstadoPersona", e);
			return Map.of("error", "Error inesperado: " + e.getMessage());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
		log.info("Buscar empleado empresa por id: {}", id);
		try {
			Optional<EmpleadoEmpresaEntity> empleadoEmpresa = empleadoEmpresaRepository.findById(id);
			if (empleadoEmpresa.isPresent()) {
				EmpleadoEmpresaResponseDTO dto = empleadoEmpresaMapper.entityToResumenDto(empleadoEmpresa.get());
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
			log.error("Error al buscar empleado empresa por id: {}", id, e);
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
		log.info("Listar todos los empleado empresa");
		try {
			var list = empleadoEmpresaRepository.findAll();
			var dtoList = empleadoEmpresaMapper.listEntityToResumenDtoList(list);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar los empleados empresas", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar empelado empresa por id: {}", id);
		try {
			if (!empleadoEmpresaRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			empleadoEmpresaRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar el empleado empresa con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

}
