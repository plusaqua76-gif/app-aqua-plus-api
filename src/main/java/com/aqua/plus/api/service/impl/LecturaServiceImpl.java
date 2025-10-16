package com.aqua.plus.api.service.impl;

import java.time.LocalDate;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
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
	public Map<String, Object> guardarLecturas(JsonNode body) {
		try {
			ArrayNode payloadArray;
			if (body == null) {
				return Map.of("error", "El cuerpo no puede ser null");
			} else if (body.isArray()) {
				payloadArray = (ArrayNode) body;
			} else if (body.has("lecturas") && body.get("lecturas").isArray()) {
				payloadArray = (ArrayNode) body.get("lecturas");
			} else if (body.isObject()) {
				payloadArray = objectMapper.createArrayNode().add(body);
			} else {
				return Map.of("error", "El cuerpo debe ser objeto, arreglo o {\"lecturas\": [...]}");
			}

			String jsonString = objectMapper.writeValueAsString(payloadArray);
			String sql = "SELECT public.registrar_lectura(CAST(:jsonData AS jsonb)) AS result";
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

			return objectMapper.readValue(jsonOut, new TypeReference<Map<String, Object>>() {
			});

		} catch (JsonProcessingException e) {
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
			String fecha, Boolean consumoAnormal, String observacion, Pageable pageable) {

		log.info(
				"Listar lecturas por empresaId={} con filtros: serial={}, lectura={}, fecha={}, consumoAnormal={}, observacion={}",
				empresaId, serial, lectura, fecha, consumoAnormal, observacion);

		try {
			LocalDate f = (fecha == null || fecha.isBlank()) ? null : LocalDate.parse(fecha); // yyyy-MM-dd

			Specification<LecturaEntity> spec = allOfNonNull(LecturaSpecifications.perteneceAEmpresa(empresaId),
					LecturaSpecifications.serialLike(serial), LecturaSpecifications.lecturaEquals(lectura),
					(f != null ? LecturaSpecifications.fechaBetween(f, f) : null),
					LecturaSpecifications.consumoAnormalEquals(consumoAnormal),
					LecturaSpecifications.observacionLike(observacion));

			Sort defaultSort = Sort.by(Sort.Order.desc("fechaLectura"), Sort.Order.desc("id"));

			Pageable pageToUse;
			if (pageable == null) {
				pageToUse = PageRequest.of(0, 20, defaultSort);
			} else if (pageable.getSort().isUnsorted()) {
				pageToUse = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort);
			} else {
				pageToUse = pageable;
			}

			Page<LecturaEntity> page = lecturaRepository.findAll(spec, pageToUse);
			var content = lecturaMapper.listEntityToDtoList(page.getContent());

			long totalCount = page.getTotalElements();
			int pageSize = page.getSize();
			int currentPage = page.getNumber();
			int totalPages = page.getTotalPages();

			if (page.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron lecturas para la empresa con id " + empresaId)
								.code(HttpStatus.NOT_FOUND.value()).response(content).totalCount(totalCount)
								.pageSize(pageSize).currentPage(currentPage).totalPages(totalPages).build());
			}

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(content).totalCount(totalCount).pageSize(pageSize)
					.currentPage(currentPage).totalPages(totalPages).build());

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

		String sql = """
				    SELECT public.fn_metricas_lecturas_mes(:idEmpresa, :anio, :mes, :idCiudad, :idCorreg)::text AS payload
				""";

		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("idEmpresa", empresaId, java.sql.Types.INTEGER).addValue("anio", anio, java.sql.Types.INTEGER)
				.addValue("mes", mes, java.sql.Types.INTEGER).addValue("idCiudad", idCiudad, java.sql.Types.INTEGER)
				.addValue("idCorreg", idCorregimiento, java.sql.Types.INTEGER);

		try {
			String json = namedParameterJdbcTemplate.queryForObject(sql, params, String.class);
			if (json == null) {
				return Map.of("error", "SIN_DATOS");
			}
			return objectMapper.readValue(json,
					new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
					});
		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			log.error("Error parseando JSON de fn_metricas_lecturas_mes", e);
			return Map.of("error", "PROCESAMIENTO_JSON", "detalle", e.getMessage());
		} catch (Exception e) {
			log.error("Error ejecutando fn_metricas_lecturas_mes", e);
			return Map.of("error", "ERROR_INESPERADO", "detalle", e.getMessage());
		}
	}

}
