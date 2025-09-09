package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.ILecturaService;
import com.aqua.plus.api.service.impl.specification.LecturaSpecifications;
import com.aqua.plus.commons.dtos.LecturaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ContadorEntity;
import com.aqua.plus.commons.entities.LecturaEntity;
import com.aqua.plus.commons.maps.LecturaMapper;
import com.aqua.plus.commons.repositories.ContadorRepository;
import com.aqua.plus.commons.repositories.LecturaRepository;
import com.aqua.plus.commons.utils.Constantes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

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
				ContadorEntity contador = contadorRepository.findById(lecturaDTO.getContador().getId())
						.orElseThrow(() -> new RuntimeException(Constantes.CON_NOT_FOUND));
				entity.setContador(contador);
			}

			LecturaEntity saved = lecturaRepository.save(entity);
			LecturaDTO savedDTO = lecturaMapper.entityToDto(saved);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(message).code(statusCode)
					.response(savedDTO).build();

			return ResponseEntity.status(statusCode).body(responseDTO);

		} catch (Exception e) {
			log.error("Error guardando lectura", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	/**
	 * Guarda una o varias lecturas. - Si el JSON viene como objeto plano (una
	 * lectura), lo envuelve en un array. - Si viene como { "lecturas": [ ... ] },
	 * toma ese array. - Llama al SP: public.sincronizar_lecturas_a_nube(jsonb)
	 */
	@Transactional
	public Map<String, Object> guardarLecturas(Map<String, Object> jsonParams) {
		try {
			JsonNode root = objectMapper.valueToTree(jsonParams);
			ArrayNode payloadArray;

			if (root.has("lecturas") && root.get("lecturas").isArray()) {
				payloadArray = (ArrayNode) root.get("lecturas");
			} else {
				if (!root.isObject()) {
					return Map.of("error", "El cuerpo debe ser un objeto o {\"lecturas\": [...] }");
				}
				payloadArray = objectMapper.createArrayNode().add(root);
			}

			String jsonString = objectMapper.writeValueAsString(payloadArray);

			String sql = "SELECT public.sincronizar_lecturas_a_nube(CAST(:jsonData AS jsonb)) AS result";
			MapSqlParameterSource params = new MapSqlParameterSource("jsonData", jsonString);

			Map<String, Object> raw = namedParameterJdbcTemplate.queryForMap(sql, params);

			Object wrapper = raw.get("result");
			String jsonOut;
			if (wrapper instanceof org.postgresql.util.PGobject pg && "jsonb".equalsIgnoreCase(pg.getType())) {
				jsonOut = pg.getValue();
			} else if (wrapper instanceof String s) {
				jsonOut = s;
			} else {
				return Map.of("error", "No se pudo interpretar la respuesta del procedimiento.");
			}

			return objectMapper.readValue(jsonOut,
					new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
					});

		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			return Map.of("error", "Error JSON: " + e.getMessage());
		} catch (Exception e) {
			return Map.of("error", "Error inesperado al guardar lecturas: " + e.getMessage());
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
				ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
						.code(HttpStatus.OK.value()).response(dto).build();
				return ResponseEntity.ok(responseDTO);
			} else {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
		} catch (Exception e) {
			log.error("Error al buscar lectura por id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@SafeVarargs
	private static <T> Specification<T> allOfNonNull(Specification<T>... specs) {
		Specification<T> result = (root, query, cb) -> cb.conjunction();
		for (Specification<T> s : Stream.of(specs).filter(Objects::nonNull).toList()) {
			result = result.and(s);
		}
		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findLecturasByEmpresaId(Integer empresaId, String serial, Integer lectura,
			String fechaDesde, String fechaHasta, Boolean consumoAnormal, String observacion, Pageable pageable) {

		log.info(
				"Listar lecturas por empresaId={} con filtros: serial={}, lectura={}, fechaDesde={}, fechaHasta={}, consumoAnormal={}, observacion={}",
				empresaId, serial, lectura, fechaDesde, fechaHasta, consumoAnormal, observacion);

		try {
			java.time.LocalDate dDesde = (fechaDesde == null || fechaDesde.isBlank()) ? null
					: java.time.LocalDate.parse(fechaDesde);
			java.time.LocalDate dHasta = (fechaHasta == null || fechaHasta.isBlank()) ? null
					: java.time.LocalDate.parse(fechaHasta);

			if (dDesde != null && dHasta != null && dDesde.isAfter(dHasta)) {
				java.time.LocalDate tmp = dDesde;
				dDesde = dHasta;
				dHasta = tmp;
			}

			Specification<LecturaEntity> spec = allOfNonNull(LecturaSpecifications.perteneceAEmpresa(empresaId),
					LecturaSpecifications.serialLike(serial), LecturaSpecifications.lecturaEquals(lectura),
					LecturaSpecifications.fechaBetween(dDesde, dHasta),
					LecturaSpecifications.consumoAnormalEquals(consumoAnormal),
					LecturaSpecifications.observacionLike(observacion));

			Pageable pageToUse = (pageable != null ? pageable : Pageable.unpaged());
			Page<LecturaEntity> page = lecturaRepository.findAll(spec, pageToUse);

			if (page.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron lecturas para la empresa con id " + empresaId)
								.code(HttpStatus.NOT_FOUND.value()).build());
			}

			var content = lecturaMapper.listEntityToDtoList(page.getContent());

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(content).build());

		} catch (java.time.format.DateTimeParseException ex) {
			return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
					.message("Formato de fecha inválido. Usa yyyy-MM-dd").code(HttpStatus.BAD_REQUEST.value()).build());
		} catch (Exception e) {
			log.error("Error al listar lecturas por empresa", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message(Constantes.CONSULTING_ERROR).code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todas las lecturas");
		try {
			var list = lecturaRepository.findAll();
			var dtoList = lecturaMapper.listEntityToDtoList(list);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar las lecturas", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar lectura por id: {}", id);
		try {
			if (!lecturaRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			lecturaRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar la lectura con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Transactional(readOnly = true)
	public Map<String, Object> metricasLecturasMes(Integer empresaId, Integer anio, Integer mes, Integer idCiudad,
			Integer idCorregimiento) {
		try {
			String sql = "SELECT public.fn_metricas_lecturas_mes(:idEmpresa, :anio, :mes, :idCiudad, :idCorreg) AS payload";

			MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idEmpresa", empresaId)
					.addValue("anio", anio).addValue("mes", mes).addValue("idCiudad", idCiudad)
					.addValue("idCorreg", idCorregimiento);

			Map<String, Object> raw = namedParameterJdbcTemplate.queryForMap(sql, parameters);
			Object payload = raw.get("payload");

			if (payload instanceof org.postgresql.util.PGobject pg
					&& ("jsonb".equals(pg.getType()) || "json".equals(pg.getType()))) {
				return objectMapper.readValue(pg.getValue(),
						new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
						});
			}
			if (payload instanceof String s) {
				return objectMapper.readValue(s,
						new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
						});
			}
			return Map.of(Constantes.ERROR_KEY, Constantes.RESULT_COULD_NOT_PROCESSED);

		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			e.printStackTrace();
			return java.util.Collections.singletonMap(Constantes.ERROR_KEY,
					Constantes.PROCCESSING_ERROR + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			return java.util.Collections.singletonMap(Constantes.ERROR_KEY,
					Constantes.UNEXPECTED_ERROR + e.getMessage());
		}
	}

}
