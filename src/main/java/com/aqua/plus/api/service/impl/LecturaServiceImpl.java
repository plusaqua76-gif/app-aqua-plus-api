package com.aqua.plus.api.service.impl;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.dao.DataAccessException;
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
import com.aqua.plus.commons.entities.FacturaEntity;
import com.aqua.plus.commons.entities.LecturaEntity;
import com.aqua.plus.commons.maps.LecturaMapper;
import com.aqua.plus.commons.repositories.ContadorRepository;
import com.aqua.plus.commons.repositories.FacturaRepository;
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
	private final FacturaRepository facturaRepository;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(LecturaDTO dto) {
		log.info("Guardar/Actualizar lectura (por id de lectura)");

		try {
			if (dto == null) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false).code(HttpStatus.BAD_REQUEST.value())
								.message("El DTO de lectura es obligatorio").response(null).build());
			}

			final Integer lecturaId = dto.getId();
			final boolean esUpdate = (lecturaId != null && lecturaRepository.existsById(lecturaId));

			if (esUpdate) {
				LecturaEntity target = lecturaRepository.findById(lecturaId)
						.orElseThrow(() -> new RuntimeException(Constantes.CON_NOT_FOUND));

				FacturaEntity factura = facturaRepository.findByLectura_Id(lecturaId).orElse(null);
				if (factura != null) {
					String estadoFactura = factura.getEstado().getNombre();
					if (estadoFactura.equalsIgnoreCase("VENCIDA") || estadoFactura.equalsIgnoreCase("PAGADA")
							|| estadoFactura.equalsIgnoreCase("PAGO PARCIAL")) {

						return ResponseEntity.status(HttpStatus.CONFLICT)
								.body(ResponseDTO.builder().success(false).code(HttpStatus.CONFLICT.value())
										.message("No se puede actualizar la lectura. La factura asociada tiene estado: "
												+ estadoFactura)
										.response(null).build());
					}
				}

				ContadorEntity contadorActual = target.getContador();

				Integer contadorIdDto = (dto.getContador() != null ? dto.getContador().getId() : null);
				ContadorEntity contadorAUsar;

				if (contadorIdDto != null) {
					contadorAUsar = contadorRepository.findById(contadorIdDto)
							.orElseThrow(() -> new RuntimeException(Constantes.CON_NOT_FOUND));
				} else {
					if (contadorActual == null || contadorActual.getId() == null) {
						return ResponseEntity.badRequest()
								.body(ResponseDTO.builder().success(false).code(HttpStatus.BAD_REQUEST.value())
										.message("La lectura existente no tiene contador asociado").response(null)
										.build());
					}
					contadorAUsar = contadorActual;
				}

				lecturaMapper.updateEntityFromDto(dto, target);

				target.setContador(contadorAUsar);

				Date fechaLectura = (dto.getFechaLectura() != null) ? dto.getFechaLectura() : target.getFechaLectura();

				if (fechaLectura == null) {
					fechaLectura = new Date();
				}
				target.setFechaLectura(fechaLectura);

				target.setFechaModificacion(new Date());
				target.setUsuarioModificacion(
						dto.getUsuarioModificacion() != null ? dto.getUsuarioModificacion() : dto.getUsuarioCreacion());

				LecturaEntity saved = lecturaRepository.save(target);
				return respond(true, saved);
			}

			final Integer contadorId = (dto.getContador() != null) ? dto.getContador().getId() : null;
			if (contadorId == null) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false).code(HttpStatus.BAD_REQUEST.value())
								.message("Debe indicar el contador").response(null).build());
			}

			ContadorEntity contador = contadorRepository.findById(contadorId)
					.orElseThrow(() -> new RuntimeException(Constantes.CON_NOT_FOUND));

			LecturaEntity target = lecturaMapper.dtoToEntity(dto);
			target.setContador(contador);

			Date fechaLectura = (dto.getFechaLectura() != null) ? dto.getFechaLectura() : new Date();
			target.setFechaLectura(fechaLectura);

			target.setFechaCreacion(new Date());
			target.setUsuarioCreacion(dto.getUsuarioCreacion());
			target.setActivo(true);

			LecturaEntity saved = lecturaRepository.save(target);
			return respond(false, saved);

		} catch (Exception e) {
			log.error("Error guardando lectura", e);

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
							.code(HttpStatus.BAD_REQUEST.value()).response(e.getMessage()).build());
		}
	}

	private ResponseEntity<ResponseDTO> respond(boolean isUpdate, LecturaEntity saved) {
		var dto = lecturaMapper.entityToDto(saved);
		int code = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();
		String msg = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
		return ResponseEntity.status(code)
				.body(ResponseDTO.builder().success(true).code(code).message(msg).response(dto).build());
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
			String fechaLectura, Boolean consumoAnormal, String observacion, String nombreCompleto, String descripcion,
			Pageable pageable) {

		log.info(
				"Listar lecturas por empresaId={} con filtros: serial={}, lectura={}, fecha={}, consumoAnormal={}, observacion={}, nombreCompleto={}, descripcion={}",
				empresaId, serial, lectura, fechaLectura, consumoAnormal, observacion, nombreCompleto, descripcion);

		try {
			LocalDate f = (fechaLectura == null || fechaLectura.isBlank()) ? null : LocalDate.parse(fechaLectura); // yyyy-MM-dd

			Specification<LecturaEntity> spec = allOfNonNull(LecturaSpecifications.perteneceAEmpresa(empresaId),
					LecturaSpecifications.serialLike(serial), LecturaSpecifications.lecturaEquals(lectura),
					(f != null ? LecturaSpecifications.fechaBetween(f, f) : null),
					LecturaSpecifications.consumoAnormalEquals(consumoAnormal),
					LecturaSpecifications.observacionLike(observacion),
					LecturaSpecifications.clienteNombreLike(nombreCompleto),
					LecturaSpecifications.comentarioLike(descripcion));

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

	@Transactional(readOnly = true)
	public ResponseEntity<Map<String, Object>> facturasPendientesLectura(Integer idEmpresa, String periodo,
			Integer page, Integer size) {

		final String sql = """
				    SELECT public.fn_facturas_pendientes_lectura(:idEmpresa, :periodo, :page, :size)::text AS payload
				""";

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("idEmpresa", idEmpresa)
				.addValue("periodo", periodo).addValue("page", page).addValue("size", size);

		try {
			String payload = namedParameterJdbcTemplate.queryForObject(sql, params, String.class);

			Map<String, Object> body = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
			});

			return ResponseEntity.ok(body);

		} catch (DataAccessException ex) {
			Throwable root = ex.getRootCause();
			if (root instanceof SQLException sqlEx && "22023".equals(sqlEx.getSQLState())) {
				log.warn("Validación de parámetros fallida en fn_facturas_pendientes_lectura: {}", sqlEx.getMessage());
				return ResponseEntity.badRequest()
						.body(Map.of("success", false, "code", 400, "message", sqlEx.getMessage()));
			}

			log.error("Error de acceso a datos en fn_facturas_pendientes_lectura", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "code", 500, "message",
							"Error consultando las facturas pendientes de lectura", "detalle",
							ex.getMostSpecificCause().getMessage()));

		} catch (JsonProcessingException ex) {
			log.error("JSON inválido devuelto por fn_facturas_pendientes_lectura", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "code", 500,
					"message", "JSON inválido desde la función", "detalle", ex.getMessage()));
		}
	}

}
