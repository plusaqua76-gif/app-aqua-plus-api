package com.aqua.plus.api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.postgresql.util.PGobject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IFacturaService;
import com.aqua.plus.api.service.impl.specification.FacturaSpecifications;
import com.aqua.plus.commons.dtos.CarteraAntiguedadDTO;
import com.aqua.plus.commons.dtos.FacturaDTO;
import com.aqua.plus.commons.dtos.MetricasFinancierasDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.FacturaEntity;
import com.aqua.plus.commons.maps.EmpresaClienteContadorMapper;
import com.aqua.plus.commons.maps.EstadoMapper;
import com.aqua.plus.commons.maps.FacturaMapper;
import com.aqua.plus.commons.maps.LecturaMapper;
import com.aqua.plus.commons.maps.TipoPagoMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;

import com.aqua.plus.commons.repositories.FacturaRepository;
import com.aqua.plus.commons.utils.Constantes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacturaServiceImpl implements IFacturaService {

	private final FacturaRepository facturaRepository;
	private final FacturaMapper facturaMapper;
	private final EmpresaClienteContadorMapper empresaMapper;
	private final LecturaMapper lecturaMapper;
	private final TipoPagoMapper tipoPagoMapper;
	private final EstadoMapper estadoMapper;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final ObjectMapper objectMapper;
	private final DocumentoServiceImpl documentoServiceImpl;

	/**
	 * Genera una factura a partir de los parámetros recibidos en formato JSON.
	 * Llama a un procedimiento almacenado en PostgreSQL y devuelve el resultado
	 * deserializado.
	 * 
	 * @author nicope
	 * @version 1.0
	 */
	@Transactional
	public Map<String, Object> generarFactura(Map<String, Object> jsonParams) {
		try {
			String jsonString = objectMapper.writeValueAsString(jsonParams);

			String sql = "SELECT * FROM public.generar_factura(CAST(:jsonData AS jsonb))";

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("jsonData", jsonString);

			Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

			Object wrappedValue = rawResult.get("generar_factura");
			if (wrappedValue instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {
				String jsonValue = pgObject.getValue();

				return objectMapper.readValue(jsonValue, new TypeReference<Map<String, Object>>() {
				});
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

	@Transactional(readOnly = true)
	public Map<String, Object> consultarAguaFacturadaMesEmpresa(Integer idEmpresa, Integer anio, Integer mes) {
		try {

			String sql = """
					    SELECT public.fn_agua_facturada_mes_empresa(
					        :idEmpresa,
					        :anio,
					        :mes
					    )
					""";

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("idEmpresa", idEmpresa);
			parameters.addValue("anio", anio);
			parameters.addValue("mes", mes);

			Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

			Object wrappedValue = rawResult.get("fn_agua_facturada_mes_empresa");

			if (wrappedValue instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {

				String jsonValue = pgObject.getValue();

				return objectMapper.readValue(jsonValue, new TypeReference<Map<String, Object>>() {
				});
			}

			return Map.of(Constantes.ERROR_KEY,
					"No fue posible procesar la respuesta del SP fn_agua_facturada_mes_empresa");

		} catch (JsonProcessingException e) {
			log.error("Error deserializando respuesta del SP fn_agua_facturada_mes_empresa", e);
			return Map.of(Constantes.ERROR_KEY, Constantes.PROCCESSING_ERROR + e.getMessage());

		} catch (Exception e) {
			log.error("Error ejecutando SP fn_agua_facturada_mes_empresa", e);
			return Map.of(Constantes.ERROR_KEY, Constantes.UNEXPECTED_ERROR + e.getMessage());
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(FacturaDTO facturaDTO) {
		log.info("Creando Factura");
		try {
			FacturaEntity entity = facturaMapper.dtoToEntity(facturaDTO);
			entity.setFechaCreacion(new Date());
			entity.setUsuarioCreacion(facturaDTO.getUsuarioCreacion());
			entity.setActivo(true);

			FacturaEntity saved = facturaRepository.save(entity);
			FacturaDTO savedDTO = facturaMapper.entityToDto(saved);

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.SAVED_SUCCESSFULLY)
					.code(HttpStatus.CREATED.value()).response(savedDTO).build();

			return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
		} catch (Exception e) {
			log.error("Error creando la factura", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Transactional
	public ResponseEntity<Map<String, Object>> guardarFacturas(JsonNode body) {
		try {
			final ArrayNode payloadArray;
			if (body == null) {
				Map<String, Object> res = Map.of("success", false, "message", "El cuerpo no puede ser null", "code",
						400, "response", null);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
			} else if (body.isArray()) {
				payloadArray = (ArrayNode) body;
			} else if (body.has("facturas") && body.get("facturas").isArray()) {
				payloadArray = (ArrayNode) body.get("facturas");
			} else if (body.isObject()) {
				payloadArray = objectMapper.createArrayNode().add(body);
			} else {
				Map<String, Object> res = Map.of("success", false, "message",
						"El cuerpo debe ser objeto, arreglo o {\"facturas\": [...]}", "code", 400, "response", null);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
			}

			String jsonString = objectMapper.writeValueAsString(payloadArray);
			String sql = "SELECT public.registrar_facturas(CAST(:jsonData AS jsonb)) AS result";
			MapSqlParameterSource params = new MapSqlParameterSource("jsonData", jsonString);

			Map<String, Object> raw = namedParameterJdbcTemplate.queryForMap(sql, params);

			Object wrapper = raw.get("result");
			String jsonOut;
			if (wrapper instanceof PGobject pg && "jsonb".equalsIgnoreCase(pg.getType())) {
				jsonOut = pg.getValue();
			} else if (wrapper instanceof String s) {
				jsonOut = s;
			} else {
				Map<String, Object> res = Map.of("success", false, "message",
						"No se pudo interpretar la respuesta del procedimiento.", "code", 500, "response", null);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
			}

			Map<String, Object> sp = objectMapper.readValue(jsonOut, new TypeReference<Map<String, Object>>() {
			});
			int code = ((Number) sp.getOrDefault("code", 500)).intValue();
			HttpStatus status = HttpStatus.resolve(code);
			if (status == null)
				status = HttpStatus.INTERNAL_SERVER_ERROR;

			Map<String, Object> bodyOut = Map.of("success", sp.getOrDefault("success", code >= 200 && code < 300),
					"message", String.valueOf(sp.getOrDefault("message", "")), "code", code, "response",
					sp.get("response"));
			return ResponseEntity.status(status).body(bodyOut);

		} catch (JsonProcessingException e) {
			Map<String, Object> res = Map.of("success", false, "message", "Error JSON: " + e.getMessage(), "code", 400,
					"response", null);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
		} catch (DataAccessException e) {
			String msg = (e.getMostSpecificCause() != null) ? e.getMostSpecificCause().getMessage() : e.getMessage();
			Map<String, Object> res = Map.of("success", false, "message", msg, "code", 400, "response", null);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
		} catch (Exception e) {
			Map<String, Object> res = Map.of("success", false, "message",
					"Error inesperado al guardar facturas: " + e.getMessage(), "code", 500, "response", null);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> update(FacturaDTO facturaDTO) {
		log.info("Actualizando factura con ID: {}", facturaDTO.getId());

		try {
			Optional<FacturaEntity> optionalFactura = facturaRepository.findById(facturaDTO.getId());

			if (optionalFactura.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.builder().success(false)
						.message(Constantes.FAC_NOT_FOUND).code(HttpStatus.NOT_FOUND.value()).build());
			}

			FacturaEntity entity = optionalFactura.get();
			facturaMapper.updateEntityFromDto(facturaDTO, entity);

			if (facturaDTO.getEmpresaClienteContador() != null)
				entity.setEmpresaClienteContador(empresaMapper.dtoToEntity(facturaDTO.getEmpresaClienteContador()));

			if (facturaDTO.getLectura() != null)
				entity.setLectura(lecturaMapper.dtoToEntity(facturaDTO.getLectura()));

			if (facturaDTO.getTipoPago() != null)
				entity.setTipoPago(tipoPagoMapper.dtoToEntity(facturaDTO.getTipoPago()));

			if (facturaDTO.getEstado() != null)
				entity.setEstado(estadoMapper.dtoToEntity(facturaDTO.getEstado()));

			entity.setUsuarioModificacion(facturaDTO.getUsuarioModificacion());
			entity.setFechaModificacion(new Date());

			FacturaEntity updated = facturaRepository.save(entity);
			FacturaDTO updatedDTO = facturaMapper.entityToDto(updated);

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.FAC_UPD_SUCCESS)
					.code(HttpStatus.OK.value()).response(updatedDTO).build());

		} catch (Exception e) {
			log.error("Error actualizando la factura", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
					.message(Constantes.UPD_NOT_FOUND).code(HttpStatus.BAD_REQUEST.value()).build());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
		log.info("Buscar factura por id: {}", id);
		try {
			Optional<FacturaEntity> factura = facturaRepository.findById(id);
			if (factura.isPresent()) {
				FacturaDTO dto = facturaMapper.entityToDto(factura.get());
				ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
						.code(HttpStatus.OK.value()).response(dto).build();
				return ResponseEntity.ok(responseDTO);
			} else {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
		} catch (Exception e) {
			log.error("Error al buscar  factura por id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEnterpriseId(Integer idEmpresa, String codigo,
			String clienteNombreCompleto, String fechaEmision, String fechaFin, String estadoNombre,
			Boolean consumoAnormal, Integer consumo, Double precioMin, Double precioMax, Pageable pageable) {

		log.info(
				"Buscar facturas por empresaId={}, filtros: codigo={}, cliente={}, fechaEmision={}, fechaFin={}, estado={}, anormal={}, consumo={}, precioMin={}, precioMax={}",
				idEmpresa, codigo, clienteNombreCompleto, fechaEmision, fechaFin, estadoNombre, consumoAnormal, consumo,
				precioMin, precioMax);

		try {
			LocalDate emision = parseSingleDateOrNull(fechaEmision);
			LocalDate venc = parseSingleDateOrNull(fechaFin);

			Specification<FacturaEntity> spec = buildFacturaSpec(idEmpresa, codigo, clienteNombreCompleto, emision,
					emision, venc, venc, estadoNombre, consumoAnormal, consumo, precioMin, precioMax)
					.and(FacturaSpecifications.activoTrue());

			Page<FacturaEntity> page = facturaRepository.findAll(spec, pageable);

			var items = facturaMapper.listEntityToResponse(page.getContent());

			long totalCount = page.getTotalElements();
			int totalPages = page.getTotalPages();
			int currentPage = page.getNumber();
			int pageSize = page.getSize();

			if (page.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron facturas para la empresa con id " + idEmpresa)
								.code(HttpStatus.NOT_FOUND.value()).response(items).totalCount(totalCount)
								.pageSize(pageSize).currentPage(currentPage).totalPages(totalPages).build());
			}

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(items).totalCount(totalCount).pageSize(pageSize)
					.currentPage(currentPage).totalPages(totalPages).build());

		} catch (java.time.format.DateTimeParseException ex) {
			return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
					.message("Formato de fecha inválido. Usa yyyy-MM-dd").code(HttpStatus.BAD_REQUEST.value()).build());
		} catch (Exception e) {
			log.error("Error al buscar facturas por id de empresa: {}", idEmpresa, e);

			Throwable root = e;
			while (root.getCause() != null && root.getCause() != root) {
				root = root.getCause();
			}

			String errorMessage = e.getMessage();
			String rootCauseMessage = root.getMessage();

			Map<String, Object> errorInfo = new LinkedHashMap<>();
			errorInfo.put("exception", e.getClass().getName());
			errorInfo.put("message", errorMessage);
			errorInfo.put("rootCause", rootCauseMessage);

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false)
							.message("Error consultando facturas: "
									+ (rootCauseMessage != null ? rootCauseMessage : "ver detalle en 'response'"))
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(errorInfo).build());
		}
	}

	/* ====================== Helpers ====================== */

	private LocalDate parseSingleDateOrNull(String dateStr) {
		if (dateStr == null || dateStr.isBlank())
			return null;
		return LocalDate.parse(dateStr);
	}

	@SafeVarargs
	private static <T> Specification<T> allOfNonNull(Specification<T>... specs) {
		Specification<T> result = (root, query, cb) -> cb.conjunction();
		for (Specification<T> s : java.util.Arrays.stream(specs).filter(java.util.Objects::nonNull).toList()) {
			result = result.and(s);
		}
		return result;
	}

	private Specification<FacturaEntity> buildFacturaSpec(Integer idEmpresa, String codigo,
			String clienteNombreCompleto, LocalDate emDesde, LocalDate emHasta, LocalDate finDesde, LocalDate finHasta,
			String estadoNombre, Boolean consumoAnormal, Integer consumo, Double precioMin, Double precioMax) {

		if (precioMin != null && precioMax != null && precioMin > precioMax) {
			double tmp = precioMin;
			precioMin = precioMax;
			precioMax = tmp;
		}

		return allOfNonNull(FacturaSpecifications.perteneceAEmpresa(idEmpresa),
				FacturaSpecifications.codigoLike(codigo),
				FacturaSpecifications.clienteNombreCompletoLike(clienteNombreCompleto),
				FacturaSpecifications.fechaEmisionBetween(emDesde, emHasta),
				FacturaSpecifications.fechaFinBetween(finDesde, finHasta),
				FacturaSpecifications.estadoNombreLike(estadoNombre),
				FacturaSpecifications.consumoAnormalEquals(consumoAnormal),
				FacturaSpecifications.consumoEquals(consumo),
				FacturaSpecifications.precioBetween(precioMin, precioMax));
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findFacturasByPersona(Integer idPersona, String codigo, String fechaEmision,
			String fechaFin, String estadoNombre, Boolean consumoAnormal, Double precio, Pageable pageable) {
		if (idPersona == null) {
			return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
					.message("El parámetro idPersona es requerido.").code(HttpStatus.BAD_REQUEST.value()).build());
		}

		log.info(
				"findFacturasByPersona -> personaId={}, filtros: codigo={}, fechaEmision={}, fechaFin={}, estado={}, anormal={}, precio={}",
				idPersona, codigo, fechaEmision, fechaFin, estadoNombre, consumoAnormal, precio);

		try {
			LocalDate emision = parseSingleDateOrNull(fechaEmision);
			LocalDate venc = parseSingleDateOrNull(fechaFin);

			Specification<FacturaEntity> spec = java.util.stream.Stream
					.of(FacturaSpecifications.activoTrue(), FacturaSpecifications.personaIdEquals(idPersona),
							FacturaSpecifications.codigoLike(codigo),
							FacturaSpecifications.estadoNombreLike(estadoNombre),
							FacturaSpecifications.fechaEmisionBetween(emision, emision),
							FacturaSpecifications.fechaFinBetween(venc, venc),
							FacturaSpecifications.consumoAnormalEquals(consumoAnormal),
							FacturaSpecifications.precioEquals(precio))
					.filter(java.util.Objects::nonNull).reduce((a, b) -> a.and(b))
					.orElse((root, q, cb) -> cb.conjunction());

			Pageable pageToUse = (pageable != null ? pageable : Pageable.unpaged());
			Page<FacturaEntity> page = facturaRepository.findAll(spec, pageToUse);

			var items = facturaMapper.listEntityToResponse(page.getContent());

			if (page.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron facturas para la persona " + idPersona)
								.code(HttpStatus.NOT_FOUND.value()).response(items).totalCount(page.getTotalElements())
								.pageSize(page.getSize()).currentPage(page.getNumber()).totalPages(page.getTotalPages())
								.build());
			}

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(items).totalCount(page.getTotalElements())
					.pageSize(page.getSize()).currentPage(page.getNumber()).totalPages(page.getTotalPages()).build());

		} catch (java.time.format.DateTimeParseException ex) {
			return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
					.message("Formato de fecha inválido. Usa yyyy-MM-dd").code(HttpStatus.BAD_REQUEST.value()).build());
		} catch (Exception e) {
			log.error("Error en findFacturasByPersona: personaId={}", idPersona, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todos las facturas");
		try {
			var list = facturaRepository.findAll();

			var dtoList = facturaMapper.listEntityToResumenDtoList(list);

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();

			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar las facturas", e);

			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar factura por id: {}", id);
		try {
			if (!facturaRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			facturaRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar factura con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Transactional(readOnly = true)
	public ResponseEntity<Map<String, Object>> metricasConsumoMes(Integer empresaId, Integer anio, Integer mes) {
		try {
			final String sql = """
					    SELECT public.fn_metricas_consumo_mes(:idEmpresa, :anio, :mes)::text AS payload
					""";

			MapSqlParameterSource params = new MapSqlParameterSource().addValue("idEmpresa", empresaId)
					.addValue("anio", anio).addValue("mes", mes);

			Map<String, Object> row = namedParameterJdbcTemplate.queryForMap(sql, params);
			String payload = (String) row.get("payload");

			Map<String, Object> body = objectMapper.readValue(payload,
					new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
					});

			Integer code = Integer.valueOf(String.valueOf(body.getOrDefault("code", 200)));
			boolean success = Boolean.TRUE.equals(body.getOrDefault("success", true));
			HttpStatus status = HttpStatus.resolve(code);
			if (status == null)
				status = success ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;

			return ResponseEntity.status(status).body(body);

		} catch (org.springframework.dao.EmptyResultDataAccessException ex) {
			return ResponseEntity.ok(Map.of("success", true, "code", 200, "message", "Sin datos", "data", Map.of()));
		} catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "code", 500,
					"message", "JSON inválido desde la función", "detalle", ex.getMessage()));
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
					Map.of("success", false, "code", 500, "message", "Error inesperado", "detalle", ex.getMessage()));
		}
	}

	@Transactional(readOnly = true)
	public ResponseEntity<Map<String, Object>> metricasConsumoMesEmpresa(Integer empresaId, Integer anio, Integer mes) {
		try {
			final String sql = """
					    SELECT public.fn_metricas_consumo_mes_empresa(:idEmpresa, :anio, :mes)::text AS payload
					""";

			MapSqlParameterSource params = new MapSqlParameterSource().addValue("idEmpresa", empresaId)
					.addValue("anio", anio).addValue("mes", mes);

			Map<String, Object> row = namedParameterJdbcTemplate.queryForMap(sql, params);
			String payload = (String) row.get("payload");

			Map<String, Object> body = objectMapper.readValue(payload,
					new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
					});

			Integer code = Integer.valueOf(String.valueOf(body.getOrDefault("code", 200)));
			boolean success = Boolean.TRUE.equals(body.getOrDefault("success", true));
			HttpStatus status = HttpStatus.resolve(code);
			if (status == null)
				status = success ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;

			return ResponseEntity.status(status).body(body);

		} catch (org.springframework.dao.EmptyResultDataAccessException ex) {
			return ResponseEntity.ok(Map.of("success", true, "code", 200, "message", "Sin datos", "data", Map.of()));
		} catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "code", 500,
					"message", "JSON inválido desde la función", "detalle", ex.getMessage()));
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
					Map.of("success", false, "code", 500, "message", "Error inesperado", "detalle", ex.getMessage()));
		}
	}

	@Transactional(readOnly = true)
	public Map<String, Object> metricasFacturaMes(Integer empresaId, Integer anio, Integer mes) {
		try {
			String sql = "SELECT public.fn_metricas_facturas_mes(:idEmpresa, :anio, :mes) AS payload";

			MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idEmpresa", empresaId)
					.addValue("anio", anio).addValue("mes", mes);

			Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

			Object wrappedValue = rawResult.get("payload");

			if (wrappedValue instanceof org.postgresql.util.PGobject pg
					&& ("jsonb".equals(pg.getType()) || "json".equals(pg.getType()))) {
				String jsonValue = pg.getValue();
				return objectMapper.readValue(jsonValue,
						new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
						});
			}
			if (wrappedValue instanceof String s) {
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

	/**
	 * Actualiza facturas por código (batch o una sola), delegando en el SP:
	 * public.actualizar_facturas(jsonb) El payload puede ser un objeto JSON o un
	 * array de objetos.
	 * 
	 * @author nicope
	 * @version 1.0
	 * 
	 */
	@Transactional
	public Map<String, Object> actualizarFacturas(Object jsonPayload) {
		try {
			String jsonString = objectMapper.writeValueAsString(jsonPayload);

			String sql = "SELECT public.actualizar_facturas(CAST(:jsonData AS jsonb)) AS actualizar_facturas";

			MapSqlParameterSource params = new MapSqlParameterSource("jsonData", jsonString);
			Map<String, Object> row = namedParameterJdbcTemplate.queryForMap(sql, params);

			Object resultObj = row.get("actualizar_facturas");
			if (resultObj instanceof org.postgresql.util.PGobject pg && "jsonb".equalsIgnoreCase(pg.getType())) {
				return objectMapper.readValue(pg.getValue(),
						new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
						});
			}
			if (resultObj instanceof String s) {
				return objectMapper.readValue(s,
						new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
						});
			}

			return Map.of(Constantes.ERROR_KEY, "No fue posible procesar la respuesta del SP");

		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			return Map.of(Constantes.ERROR_KEY, "Error serializando/deserializando JSON: " + e.getMessage());
		} catch (org.springframework.dao.DataAccessException e) {
			return Map.of("error",
					"Error de base de datos al ejecutar el SP: " + e.getMostSpecificCause().getMessage());
		} catch (Exception e) {
			return Map.of("error", "Error inesperado: " + e.getMessage());
		}
	}

	public ResponseEntity<ResponseDTO> sugerirCodigos(String term) {
		if (term == null || term.trim().length() < 3) {
			ResponseDTO dto = ResponseDTO.builder().success(false).message("Debe escribir al menos 3 caracteres")
					.code(HttpStatus.BAD_REQUEST.value()).build();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(dto);
		}

		List<Map<String, Object>> sugerencias = facturaRepository
				.findTop5ByCodigoStartingWithIgnoreCaseOrderByCodigoAsc(term.trim()).stream().map(f -> {
					Map<String, Object> map = new HashMap<>();
					map.put("empresaClienteContadorId", f.getEmpresaClienteContador().getId());
					map.put("codigo", f.getCodigo());
					return map;
				}).toList();

		if (sugerencias.isEmpty()) {
			ResponseDTO dto = ResponseDTO.builder().success(false).message("No se encontraron resultados")
					.code(HttpStatus.NO_CONTENT.value()).response(List.of()).build();
			return ResponseEntity.status(HttpStatus.NO_CONTENT).body(dto);
		}

		ResponseDTO dto = ResponseDTO.builder().success(true).message("Resultados encontrados")
				.code(HttpStatus.OK.value()).response(sugerencias).build();
		return ResponseEntity.ok(dto);
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> obtenerFacturaDetalle(Integer idFactura) {
		String sql = "SELECT public.obtener_factura_detalle(:id) AS result";
		MapSqlParameterSource params = new MapSqlParameterSource("id", idFactura);

		try {
			String json = namedParameterJdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> {
				Object obj = rs.getObject("result");
				if (obj instanceof PGobject pg && "jsonb".equalsIgnoreCase(pg.getType())) {
					return pg.getValue();
				}
				return (obj != null) ? obj.toString() : null;
			});

			if (json == null) {
				ResponseDTO dto = new ResponseDTO();
				dto.setSuccess(false);
				dto.setMessage("Respuesta vacía del SP.");
				dto.setCode(500);
				dto.setResponse(null);
				return new ResponseEntity<>(dto, HttpStatus.INTERNAL_SERVER_ERROR);
			}

			var root = objectMapper.readTree(json);
			if (!(root instanceof com.fasterxml.jackson.databind.node.ObjectNode rootObj)) {
				ResponseDTO dto = objectMapper.readValue(json, ResponseDTO.class);
				return new ResponseEntity<>(dto, httpStatusFromCode(dto));
			}

			var responseObj = asObject(rootObj.path("response"));
			if (responseObj == null) {
				ResponseDTO dto = objectMapper.readValue(json, ResponseDTO.class);
				return new ResponseEntity<>(dto, httpStatusFromCode(dto));
			}

			com.fasterxml.jackson.databind.node.ObjectNode parentOfEmpresa = null;
			com.fasterxml.jackson.databind.node.ObjectNode empresaObj = null;
			String empresaPathUsed = null;

			var dataObj = asObject(responseObj.path("data"));
			if (dataObj != null && dataObj.has("empresa") && dataObj.get("empresa").isObject()) {
				empresaObj = asObject(dataObj.get("empresa"));
				parentOfEmpresa = dataObj;
				empresaPathUsed = "response.data.empresa";
			}

			if (empresaObj == null && responseObj.has("empresa") && responseObj.get("empresa").isObject()) {
				empresaObj = asObject(responseObj.get("empresa"));
				parentOfEmpresa = responseObj;
				empresaPathUsed = "response.empresa";
			}

			if (empresaObj == null || parentOfEmpresa == null) {
				log.warn("No se encontró nodo 'empresa' ni en response.data.empresa ni en response.empresa");

				ResponseDTO dto = objectMapper.readValue(json, ResponseDTO.class);
				return new ResponseEntity<>(dto, httpStatusFromCode(dto));
			}

			Integer empresaId = parseIntSafe(empresaObj.get("id"));
			log.info("Ruta empresa detectada: {} | empresaId={}", empresaPathUsed, empresaId);

			try {
				var puntosPagoArr = buildDocsArrayNombreImagen(empresaId, Constantes.TYPE_PUNTO_PAGO);
				empresaObj.set("puntosPago", puntosPagoArr);
				log.info("puntosPago inyectado (empresaId={} count={})", empresaId, puntosPagoArr.size());
			} catch (Exception ex) {
				log.warn("Fallo al inyectar puntosPago (empresaId={}): {}", empresaId, ex.getMessage());
				empresaObj.set("puntosPago", objectMapper.createArrayNode());
			}

			try {
				var codigoQrObj = buildCodigoQrNombreImagen(empresaId);
				empresaObj.set("codigoQr", codigoQrObj);
				log.info("codigoQr inyectado (empresaId={} tieneImagen?={})", empresaId,
						codigoQrObj.hasNonNull("imagen"));
			} catch (Exception ex) {
				log.warn("Fallo al inyectar codigoQr (empresaId={}): {}", empresaId, ex.getMessage());
				empresaObj.set("codigoQr", objectMapper.createObjectNode());
			}

			parentOfEmpresa.set("empresa", empresaObj);

			ResponseDTO enriched = objectMapper.treeToValue(rootObj, ResponseDTO.class);
			return new ResponseEntity<>(enriched, httpStatusFromCode(enriched));

		} catch (EmptyResultDataAccessException e) {
			ResponseDTO dto = new ResponseDTO();
			dto.setSuccess(false);
			dto.setMessage("No existe una factura con ese id.");
			dto.setCode(404);
			dto.setResponse(null);
			return new ResponseEntity<>(dto, HttpStatus.NOT_FOUND);

		} catch (Exception e) {
			ResponseDTO dto = new ResponseDTO();
			dto.setSuccess(false);
			dto.setMessage("Error inesperado: " + e.getMessage());
			dto.setCode(500);
			dto.setResponse(null);
			return new ResponseEntity<>(dto, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/*
	 * -------------------- Helpers (idénticos/compatibles con syncConfigEnterprise)
	 * --------------------
	 */

	private ObjectNode asObject(JsonNode n) {
		return (n instanceof ObjectNode) ? (ObjectNode) n : null;
	}

	private HttpStatus httpStatusFromCode(ResponseDTO dto) {
		if (dto == null)
			return HttpStatus.OK;
		if (dto.getCode() != null) {
			return switch (dto.getCode()) {
			case 404 -> HttpStatus.NOT_FOUND;
			case 400 -> HttpStatus.BAD_REQUEST;
			case 500 -> HttpStatus.INTERNAL_SERVER_ERROR;
			default -> HttpStatus.OK;
			};
		}
		return Boolean.FALSE.equals(dto.getSuccess()) ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.OK;
	}

	private Integer parseIntSafe(com.fasterxml.jackson.databind.JsonNode n) {
		if (n == null || n.isNull())
			return null;
		if (n.isInt())
			return n.intValue();
		if (n.isTextual()) {
			try {
				return Integer.valueOf(n.textValue().trim());
			} catch (Exception ignore) {
			}
		}
		return null;
	}

	/**
	 * PUNTOS DE PAGO: TODOS los documentos de una categoría -> ARRAY [{ nombre,
	 * imagen }, ...].
	 */
	private ArrayNode buildDocsArrayNombreImagen(Integer empresaId, String categoriaCodigo) {
		ResponseEntity<ResponseDTO> resp = documentoServiceImpl.listarPorEmpresaYCategoriaCodigo(empresaId,
				categoriaCodigo);
		var out = objectMapper.createArrayNode();

		if (resp == null || !resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null
				|| !Boolean.TRUE.equals(resp.getBody().getSuccess())) {
			return out;
		}

		Object payload = resp.getBody().getResponse();
		if (!(payload instanceof java.util.List<?> lista) || lista.isEmpty())
			return out;

		for (Object item : lista) {
			var node = objectMapper.valueToTree(item);

			String b64 = null;
			if (node.hasNonNull("imagen"))
				b64 = node.get("imagen").asText();
			else if (node.hasNonNull("imagenBase64"))
				b64 = node.get("imagenBase64").asText();

			String nombre = null;
			if (node.hasNonNull("nombre"))
				nombre = node.get("nombre").asText();
			else if (node.hasNonNull("ruta"))
				nombre = extraerNombreDeRuta(node.get("ruta").asText());

			if (b64 != null && !b64.isBlank()) {
				if (nombre == null || nombre.isBlank())
					nombre = "documento";
				var obj = objectMapper.createObjectNode();
				obj.put("nombre", nombre);
				obj.put("imagen", b64);
				out.add(obj);
			}
		}
		return out;
	}

	/**
	 * CODIGO QR: objeto { nombre, imagen } con fallback: (empresa + "QR") -> ("QR"
	 * global).
	 */
	private ObjectNode buildCodigoQrNombreImagen(Integer empresaId) {
		var empty = objectMapper.createObjectNode();

		if (empresaId != null) {
			ResponseEntity<ResponseDTO> respEmp = documentoServiceImpl.listarPorEmpresaYCategoriaCodigo(empresaId,
					Constantes.TYPE_COD_QR);
			ObjectNode objEmp = toFirstNombreImagen(respEmp);
			if (objEmp != null)
				return objEmp;
		}

		ResponseEntity<ResponseDTO> respGlobal = documentoServiceImpl.listarPorEmpresaYCategoriaCodigo(null,
				Constantes.TYPE_COD_QR);
		ObjectNode objGlobal = toFirstNombreImagen(respGlobal);
		return (objGlobal != null) ? objGlobal : empty;
	}

	/**
	 * Toma el PRIMER elemento de la lista del servicio (si existe) y lo normaliza a
	 * {nombre, imagen}.
	 */
	private ObjectNode toFirstNombreImagen(ResponseEntity<ResponseDTO> resp) {
		if (resp == null || !resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null
				|| !Boolean.TRUE.equals(resp.getBody().getSuccess())) {
			return null;
		}
		Object payload = resp.getBody().getResponse();
		if (!(payload instanceof java.util.List<?> lista) || lista.isEmpty())
			return null;

		Object first = lista.get(0);
		var node = objectMapper.valueToTree(first);

		String b64 = null;
		if (node.hasNonNull("imagen"))
			b64 = node.get("imagen").asText();
		else if (node.hasNonNull("imagenBase64"))
			b64 = node.get("imagenBase64").asText();

		String nombre = null;
		if (node.hasNonNull("nombre"))
			nombre = node.get("nombre").asText();
		else if (node.hasNonNull("ruta"))
			nombre = extraerNombreDeRuta(node.get("ruta").asText());

		if (b64 == null || b64.isBlank())
			return null;
		if (nombre == null || nombre.isBlank())
			nombre = "documento";

		var obj = objectMapper.createObjectNode();
		obj.put("nombre", nombre);
		obj.put("imagen", b64);
		return obj;
	}

	private String extraerNombreDeRuta(String ruta) {
		if (ruta == null || ruta.isBlank())
			return "sin_nombre";
		int slash = Math.max(ruta.lastIndexOf('/'), ruta.lastIndexOf('\\'));
		return (slash >= 0 && slash < ruta.length() - 1) ? ruta.substring(slash + 1) : ruta;
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> obtenerMetricasCarteraPorAntiguedad(Integer empresaId) {
		log.info("Obteniendo métricas de cartera por antigüedad para empresa: {}", empresaId);
		
		try {
			if (empresaId == null) {
				log.warn("El ID de empresa es requerido");
				ResponseDTO errorResponse = ResponseDTO.builder()
						.success(false)
						.message("El ID de empresa es requerido")
						.code(HttpStatus.BAD_REQUEST.value())
						.build();
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
			}

			List<Object[]> resultados = facturaRepository.obtenerMetricasCarteraPorAntiguedad(empresaId);

			List<CarteraAntiguedadDTO> metricas = new ArrayList<>();
			for (Object[] resultado : resultados) {
				CarteraAntiguedadDTO dto = CarteraAntiguedadDTO.builder()
						.rangoAntiguedad((String) resultado[0])
						.cantidadDeudas(((Number) resultado[1]).longValue())
						.valorCartera((BigDecimal) resultado[2])
						.build();
				metricas.add(dto);
			}

			if (metricas.isEmpty()) {
				log.info("No se encontraron facturas pendientes para la empresa: {}", empresaId);
				ResponseDTO responseDTO = ResponseDTO.builder()
						.success(true)
						.message("No se encontraron facturas pendientes para esta empresa")
						.code(HttpStatus.OK.value())
						.response(metricas)
						.build();
				return ResponseEntity.ok(responseDTO);
			}

			BigDecimal totalGeneral = metricas.stream()
					.map(CarteraAntiguedadDTO::getValorCartera)
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			log.info("Métricas obtenidas exitosamente. Total de rangos: {}, Valor total: {}", 
					metricas.size(), totalGeneral);

			Map<String, Object> data = new HashMap<>();
			data.put("metricas", metricas);
			data.put("totalGeneral", totalGeneral);
			data.put("cantidadRangos", metricas.size());

			ResponseDTO responseDTO = ResponseDTO.builder()
					.success(true)
					.message("Métricas de cartera obtenidas exitosamente")
					.code(HttpStatus.OK.value())
					.response(data)
					.build();

			return ResponseEntity.ok(responseDTO);

		} catch (Exception e) {
			log.error("Error al obtener métricas de cartera por antigüedad para empresa: {}", empresaId, e);
			
			ResponseDTO errorResponse = ResponseDTO.builder()
					.success(false)
					.message("Error al obtener las métricas de cartera: " + e.getMessage())
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.build();
			
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> obtenerMetricasFinancieras(Integer empresaId, Integer mes, Integer anio) {
		log.info("Obteniendo métricas financieras para empresa: {}, mes: {}, año: {}", empresaId, mes, anio);
		
		try {
			if (empresaId == null) {
				log.warn("El ID de empresa es requerido");
				ResponseDTO errorResponse = ResponseDTO.builder()
						.success(false)
						.message("El ID de empresa es requerido")
						.code(HttpStatus.BAD_REQUEST.value())
						.build();
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
			}

			List<Object[]> resultados = facturaRepository.obtenerMetricasFinancieras(empresaId, mes, anio);

			if (resultados.isEmpty()) {
				log.warn("No se obtuvieron métricas para la empresa: {}, período: {}/{}", empresaId, mes, anio);
				ResponseDTO responseDTO = ResponseDTO.builder()
						.success(false)
						.message("No se pudieron calcular las métricas para esta empresa en el período especificado")
						.code(HttpStatus.NOT_FOUND.value())
						.build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}

			Object[] resultado = resultados.get(0);
			
			MetricasFinancierasDTO metricas = MetricasFinancierasDTO.builder()
					.coberturaGastosOperativos((BigDecimal) resultado[0])
					.recaudoPorcentaje((BigDecimal) resultado[1])
					.carteraVencidaPorcentaje((BigDecimal) resultado[2])
					.liquidez((BigDecimal) resultado[3])
					.totalRecaudo((BigDecimal) resultado[4])
					.totalFacturacion((BigDecimal) resultado[5])
					.totalCarteraVencida((BigDecimal) resultado[6])
					.activosCorrientes((BigDecimal) resultado[7])
					.pasivosCorrientes((BigDecimal) resultado[8])
					.totalGastos((BigDecimal) resultado[9])
					.build();

			log.info("Métricas financieras obtenidas exitosamente para empresa: {}, período: {}/{}", empresaId, mes, anio);
			log.debug("Recaudo: {}, Facturación: {}, Liquidez: {}", 
					metricas.getTotalRecaudo(), 
					metricas.getTotalFacturacion(), 
					metricas.getLiquidez());

			ResponseDTO responseDTO = ResponseDTO.builder()
					.success(true)
					.message("Métricas financieras obtenidas exitosamente para el período " + mes + "/" + anio)
					.code(HttpStatus.OK.value())
					.response(metricas)
					.build();

			return ResponseEntity.ok(responseDTO);

		} catch (Exception e) {
			log.error("Error al obtener métricas financieras para empresa: {}, período: {}/{}", empresaId, mes, anio, e);
			
			ResponseDTO errorResponse = ResponseDTO.builder()
					.success(false)
					.message("Error al obtener las métricas financieras: " + e.getMessage())
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.build();
			
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}

}