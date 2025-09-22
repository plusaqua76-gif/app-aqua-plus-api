package com.aqua.plus.api.service.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import org.postgresql.util.PGobject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.ITarifaConceptoService;
import com.aqua.plus.commons.dtos.ConceptoEstratoDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.TarifaConceptoDTO;
import com.aqua.plus.commons.entities.TarifaConceptoEntity;
import com.aqua.plus.commons.maps.TarifaConceptoMapper;
import com.aqua.plus.commons.repositories.ConceptoEstratoRepository;
import com.aqua.plus.commons.repositories.TarifaConceptoRepository;
import com.aqua.plus.commons.utils.Constantes;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TarifaConceptoServiceImpl implements ITarifaConceptoService {

	private final TarifaConceptoRepository tarifaConceptoRepository;
	private final TarifaConceptoMapper tarifaConceptoMapper;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final ObjectMapper objectMapper;
	private final ConceptoEstratoRepository conceptoEstratoRepository;

	/**
	 * Invoca el SP public."crearTarifa_Concepto"(jsonb) que retorna jsonb. Acepta
	 * un mapa (payload) que será serializado a JSON.
	 */
	@Transactional
	public ResponseEntity<ResponseDTO> crearTarifaConcepto(Map<String, Object> payload) {
		log.info("Upsert Tarifa+Concepto (single/batch wrapper) payload={}", payload);
		try {
			final String KEY_ITEMS = "items";
			final String KEY_CONCEPTO = "concepto";

			final String SQL_GET = """
					    SELECT public."get_tarifa_concepto_estrato"(
					        CAST(:idEmpresa AS int),
					        CAST(:idTipoTarifa AS int),
					        CAST(:idTipoConcepto AS int)
					    ) AS result
					""";
			final String SQL_UPD = "SELECT public.\"actualizar_tarifa_concepto\"(:jsonData) AS result";
			final String SQL_INS = "SELECT public.\"creartarifa_concepto\"(:jsonData) AS result";

			// ===== Detectar batch =====
			Object itemsObj = payload.get(KEY_ITEMS);
			boolean isBatch = (itemsObj instanceof List<?>);

			if (isBatch) {
				// Normalizar a List<Map<String,Object>> de forma segura
				List<Map<String, Object>> items = objectMapper.convertValue(itemsObj, new TypeReference<>() {
				});

				List<Map<String, Object>> resultados = new java.util.ArrayList<>();
				boolean allOk = true;

				for (int i = 0; i < items.size(); i++) {
					Map<String, Object> item = items.get(i);
					Map<String, Object> concepto = objectMapper.convertValue(item.get(KEY_CONCEPTO),
							new TypeReference<>() {
							});

					Integer idEmpresa = (item.get("idEmpresa") instanceof Number n) ? n.intValue() : null;
					Integer idTipoTarifa = (item.get("idTipoTarifa") instanceof Number n) ? n.intValue() : null;
					Integer idTipoConcepto = (concepto != null && (concepto.get("idTipoConcepto") instanceof Number n))
							? n.intValue()
							: null;

					if (idEmpresa == null || idTipoTarifa == null || idTipoConcepto == null) {
						allOk = false;
						resultados.add(Map.of("index", i, "status", HttpStatus.BAD_REQUEST.value(), "message",
								"Faltan idEmpresa, idTipoTarifa o concepto.idTipoConcepto"));
						continue;
					}

					// ---------- 1) GET existencia ----------
					MapSqlParameterSource paramsGet = new MapSqlParameterSource().addValue("idEmpresa", idEmpresa)
							.addValue("idTipoTarifa", idTipoTarifa).addValue("idTipoConcepto", idTipoConcepto);

					Map<String, Object> rowGet = namedParameterJdbcTemplate.queryForMap(SQL_GET, paramsGet);
					String jsonGet;
					Object roGet = rowGet.get("result");
					if (roGet instanceof PGobject pg && "jsonb".equalsIgnoreCase(pg.getType())) {
						jsonGet = pg.getValue();
					} else if (roGet instanceof String s) {
						jsonGet = s;
					} else {
						jsonGet = (roGet != null) ? String.valueOf(roGet) : null;
					}

					if (jsonGet == null || jsonGet.isBlank()) {
						allOk = false;
						resultados.add(java.util.Map.of("index", i, "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
								"message", "SP de consulta no retornó contenido"));
						continue;
					}

					Map<String, Object> mapGet = objectMapper.readValue(jsonGet,
							new TypeReference<Map<String, Object>>() {
							});

					int statusGet;
					try {
						statusGet = Integer.parseInt(String.valueOf(mapGet.getOrDefault("statusCode", 500)));
					} catch (Exception ex) {
						statusGet = 500;
					}

					if (statusGet == 200) {
						// ---------- EXISTE -> UPDATE ----------
						Map<String, Object> resp = objectMapper.convertValue(mapGet.get("response"),
								new TypeReference<>() {
								});
						Integer idTarifaConcepto = (resp != null && (resp.get("idTarifaConcepto") instanceof Number n))
								? n.intValue()
								: null;

						if (idTarifaConcepto == null) {
							allOk = false;
							resultados.add(Map.of("index", i, "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
									"message", "Respuesta inválida del SP de consulta (faltó idTarifaConcepto)"));
							continue;
						}

						Map<String, Object> jsonUpd = new HashMap<>();
						jsonUpd.put("idTarifaConcepto", idTarifaConcepto);
						if (concepto != null) {
							if (concepto.containsKey("valor"))
								jsonUpd.put("valor", concepto.get("valor"));
							if (concepto.containsKey("valoresEstrato"))
								jsonUpd.put("estratos", concepto.get("valoresEstrato"));
							if (concepto.containsKey("estratosEliminarIds"))
								jsonUpd.put("estratosEliminarIds", concepto.get("estratosEliminarIds"));
							if (concepto.containsKey("indCalcularMc"))
								jsonUpd.put("indCalcularMc", concepto.get("indCalcularMc"));
						}
						if (item.containsKey("activo"))
							jsonUpd.put("activo", item.get("activo"));
						jsonUpd.put("usuarioModificacion", item.getOrDefault("usuarioModificacion",
								item.getOrDefault("usuarioCreacion", "system")));

						String jsonUpdStr = objectMapper.writeValueAsString(jsonUpd);

						PGobject jsonbParamUpd = new PGobject();
						jsonbParamUpd.setType("jsonb");
						jsonbParamUpd.setValue(jsonUpdStr);

						MapSqlParameterSource paramsUpd = new MapSqlParameterSource().addValue("jsonData",
								jsonbParamUpd);
						Map<String, Object> rowUpd = namedParameterJdbcTemplate.queryForMap(SQL_UPD, paramsUpd);

						String jsonUpdResp;
						Object roUpd = rowUpd.get("result");
						if (roUpd instanceof PGobject pg2 && "jsonb".equalsIgnoreCase(pg2.getType())) {
							jsonUpdResp = pg2.getValue();
						} else if (roUpd instanceof String s2) {
							jsonUpdResp = s2;
						} else {
							jsonUpdResp = (roUpd != null) ? String.valueOf(roUpd) : null;
						}

						if (jsonUpdResp == null || jsonUpdResp.isBlank()) {
							allOk = false;
							resultados.add(Map.of("index", i, "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
									"message", "SP de actualización no retornó contenido"));
							continue;
						}

						Map<String, Object> updMap = objectMapper.readValue(jsonUpdResp,
								new TypeReference<Map<String, Object>>() {
								});
						int code = HttpStatus.OK.value();
						Object codeObj = updMap.getOrDefault("statusCode", updMap.get("code"));
						if (codeObj != null) {
							try {
								code = Integer.parseInt(codeObj.toString());
							} catch (Exception ignore) {
							}
						}
						resultados.add(Map.of("index", i, "status", code, "body", updMap));
						if (code < 200 || code >= 300)
							allOk = false;

					} else if (statusGet == 404) {
						// ---------- NO EXISTE -> INSERT ----------
						Map<String, Object> jsonIns = new HashMap<>();
						jsonIns.put("idEmpresa", idEmpresa);
						jsonIns.put("idTipoTarifa", idTipoTarifa);
						jsonIns.put("usuarioCreacion", item.getOrDefault("usuarioCreacion", "system"));

						Map<String, Object> conceptoIns = new HashMap<>();
						conceptoIns.put("idTipoConcepto", idTipoConcepto);
						if (concepto != null) {
							if (concepto.containsKey("valor")) {
								conceptoIns.put("valor", concepto.get("valor"));
							} else if (concepto.containsKey("valoresEstrato")) {
								conceptoIns.put("valoresEstrato", concepto.get("valoresEstrato"));
							}
							if (concepto.containsKey("indCalcularMc")) {
								conceptoIns.put("indCalcularMc", concepto.get("indCalcularMc"));
							}
						}
						jsonIns.put("concepto", conceptoIns);

						String jsonInsStr = objectMapper.writeValueAsString(jsonIns);

						// Parametro tipado JSONB
						PGobject jsonbParamIns = new PGobject();
						jsonbParamIns.setType("jsonb");
						jsonbParamIns.setValue(jsonInsStr);

						MapSqlParameterSource paramsIns = new MapSqlParameterSource().addValue("jsonData",
								jsonbParamIns);
						Map<String, Object> rowIns = namedParameterJdbcTemplate.queryForMap(SQL_INS, paramsIns);

						String jsonInsResp;
						Object roIns = rowIns.get("result");
						if (roIns instanceof PGobject pg3 && "jsonb".equalsIgnoreCase(pg3.getType())) {
							jsonInsResp = pg3.getValue();
						} else if (roIns instanceof String s3) {
							jsonInsResp = s3;
						} else {
							jsonInsResp = (roIns != null) ? String.valueOf(roIns) : null;
						}

						if (jsonInsResp == null || jsonInsResp.isBlank()) {
							allOk = false;
							resultados.add(Map.of("index", i, "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
									"message", "SP de creación no retornó contenido"));
							continue;
						}

						Map<String, Object> insMap = objectMapper.readValue(jsonInsResp,
								new TypeReference<Map<String, Object>>() {
								});
						int code = HttpStatus.CREATED.value();
						Object codeObj = insMap.getOrDefault("statusCode", insMap.get("code"));
						if (codeObj != null) {
							try {
								code = Integer.parseInt(codeObj.toString());
							} catch (Exception ignore) {
							}
						}
						resultados.add(Map.of("index", i, "status", code, "body", insMap));
						if (code < 200 || code >= 300)
							allOk = false;

					} else {
						String msg = String.valueOf(mapGet.getOrDefault("message", "Fallo consultando existencia"));
						allOk = false;
						resultados.add(Map.of("index", i, "status", statusGet, "message", msg));
					}
				}

				HttpStatus http = allOk ? HttpStatus.OK : HttpStatus.MULTI_STATUS;
				ResponseDTO dto = ResponseDTO.builder().success(allOk)
						.message(allOk ? "Batch procesado correctamente" : "Batch con errores en algunos items")
						.code(http.value()).response(Map.of("resultados", resultados)).build();
				return ResponseEntity.status(http).body(dto);
			}

			Map<String, Object> concepto = objectMapper.convertValue(payload.get(KEY_CONCEPTO), new TypeReference<>() {
			});

			Integer idEmpresa = (payload.get("idEmpresa") instanceof Number n1) ? n1.intValue() : null;
			Integer idTipoTarifa = (payload.get("idTipoTarifa") instanceof Number n2) ? n2.intValue() : null;
			Integer idTipoConcepto = (concepto != null && (concepto.get("idTipoConcepto") instanceof Number n3))
					? n3.intValue()
					: null;

			if (idEmpresa == null || idTipoTarifa == null || idTipoConcepto == null) {
				ResponseDTO dto = ResponseDTO.builder().success(false)
						.message("Faltan idEmpresa, idTipoTarifa o concepto.idTipoConcepto")
						.code(HttpStatus.BAD_REQUEST.value()).build();
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(dto);
			}

			MapSqlParameterSource paramsGet = new MapSqlParameterSource().addValue("idEmpresa", idEmpresa)
					.addValue("idTipoTarifa", idTipoTarifa).addValue("idTipoConcepto", idTipoConcepto);
			Map<String, Object> rowGet = namedParameterJdbcTemplate.queryForMap(SQL_GET, paramsGet);

			String jsonGet;
			Object roGet = rowGet.get("result");
			if (roGet instanceof PGobject pg && "jsonb".equalsIgnoreCase(pg.getType())) {
				jsonGet = pg.getValue();
			} else if (roGet instanceof String s) {
				jsonGet = s;
			} else {
				jsonGet = (roGet != null) ? String.valueOf(roGet) : null;
			}

			if (jsonGet == null || jsonGet.isBlank()) {
				ResponseDTO dto = ResponseDTO.builder().success(false).message("SP de consulta no retornó contenido")
						.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(dto);
			}

			Map<String, Object> mapGet = objectMapper.readValue(jsonGet,
					new TypeReference<java.util.Map<String, Object>>() {
					});
			int statusGet;
			try {
				statusGet = Integer.parseInt(String.valueOf(mapGet.getOrDefault("statusCode", 500)));
			} catch (Exception ex) {
				statusGet = 500;
			}

			if (statusGet == 200) {
				java.util.Map<String, Object> resp = objectMapper.convertValue(mapGet.get("response"),
						new TypeReference<>() {
						});
				Integer idTarifaConcepto = (resp != null && (resp.get("idTarifaConcepto") instanceof Number n))
						? n.intValue()
						: null;
				if (idTarifaConcepto == null) {
					ResponseDTO dto = ResponseDTO.builder().success(false)
							.message("Respuesta inválida del SP de consulta (faltó idTarifaConcepto)")
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(dto);
				}

				Map<String, Object> jsonUpd = new java.util.HashMap<>();
				jsonUpd.put("idTarifaConcepto", idTarifaConcepto);
				if (concepto != null) {
					if (concepto.containsKey("valor"))
						jsonUpd.put("valor", concepto.get("valor"));
					if (concepto.containsKey("valoresEstrato"))
						jsonUpd.put("estratos", concepto.get("valoresEstrato"));
					if (concepto.containsKey("estratosEliminarIds"))
						jsonUpd.put("estratosEliminarIds", concepto.get("estratosEliminarIds"));
					if (concepto.containsKey("indCalcularMc"))
						jsonUpd.put("indCalcularMc", concepto.get("indCalcularMc"));
				}
				if (payload.containsKey("activo"))
					jsonUpd.put("activo", payload.get("activo"));
				jsonUpd.put("usuarioModificacion",
						payload.getOrDefault("usuarioModificacion", payload.getOrDefault("usuarioCreacion", "system")));

				String jsonUpdStr = objectMapper.writeValueAsString(jsonUpd);

				PGobject jsonbParamUpd = new PGobject();
				jsonbParamUpd.setType("jsonb");
				jsonbParamUpd.setValue(jsonUpdStr);

				MapSqlParameterSource paramsUpd = new MapSqlParameterSource().addValue("jsonData", jsonbParamUpd);
				Map<String, Object> rowUpd = namedParameterJdbcTemplate.queryForMap(SQL_UPD, paramsUpd);

				String jsonUpdResp;
				Object roUpd = rowUpd.get("result");
				if (roUpd instanceof PGobject pg2 && "jsonb".equalsIgnoreCase(pg2.getType())) {
					jsonUpdResp = pg2.getValue();
				} else if (roUpd instanceof String s2) {
					jsonUpdResp = s2;
				} else {
					jsonUpdResp = (roUpd != null) ? String.valueOf(roUpd) : null;
				}

				if (jsonUpdResp == null || jsonUpdResp.isBlank()) {
					ResponseDTO dto = ResponseDTO.builder().success(false)
							.message("SP de actualización no retornó contenido")
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(dto);
				}

				Map<String, Object> updMap = objectMapper.readValue(jsonUpdResp,
						new TypeReference<Map<String, Object>>() {
						});
				int code = HttpStatus.OK.value();
				Object codeObj = updMap.getOrDefault("statusCode", updMap.get("code"));
				if (codeObj != null) {
					try {
						code = Integer.parseInt(codeObj.toString());
					} catch (Exception ignore) {
					}
				}
				HttpStatus status = HttpStatus.resolve(code);
				if (status == null)
					status = HttpStatus.OK;

				ResponseDTO dto = ResponseDTO.builder().success(status.is2xxSuccessful())
						.message(String.valueOf(updMap.getOrDefault("message", "Actualización realizada")))
						.code(status.value()).response(updMap).build();
				return ResponseEntity.status(status).body(dto);

			} else if (statusGet == 404) {
				Map<String, Object> jsonIns = new HashMap<>();
				jsonIns.put("idEmpresa", idEmpresa);
				jsonIns.put("idTipoTarifa", idTipoTarifa);
				jsonIns.put("usuarioCreacion", payload.getOrDefault("usuarioCreacion", "system"));

				Map<String, Object> conceptoIns = new HashMap<>();
				conceptoIns.put("idTipoConcepto", idTipoConcepto);
				if (concepto != null) {
					if (concepto.containsKey("valor")) {
						conceptoIns.put("valor", concepto.get("valor"));
					} else if (concepto.containsKey("valoresEstrato")) {
						conceptoIns.put("valoresEstrato", concepto.get("valoresEstrato"));
					}
					if (concepto.containsKey("indCalcularMc")) {
						conceptoIns.put("indCalcularMc", concepto.get("indCalcularMc"));
					}
				}
				jsonIns.put("concepto", conceptoIns);

				String jsonInsStr = objectMapper.writeValueAsString(jsonIns);

				PGobject jsonbParamIns = new PGobject();
				jsonbParamIns.setType("jsonb");
				jsonbParamIns.setValue(jsonInsStr);

				MapSqlParameterSource paramsIns = new MapSqlParameterSource().addValue("jsonData", jsonbParamIns);
				Map<String, Object> rowIns = namedParameterJdbcTemplate.queryForMap(SQL_INS, paramsIns);

				String jsonInsResp;
				Object roIns = rowIns.get("result");
				if (roIns instanceof PGobject pg3 && "jsonb".equalsIgnoreCase(pg3.getType())) {
					jsonInsResp = pg3.getValue();
				} else if (roIns instanceof String s3) {
					jsonInsResp = s3;
				} else {
					jsonInsResp = (roIns != null) ? String.valueOf(roIns) : null;
				}

				if (jsonInsResp == null || jsonInsResp.isBlank()) {
					ResponseDTO dto = ResponseDTO.builder().success(false)
							.message("SP de creación no retornó contenido")
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(dto);
				}

				Map<String, Object> insMap = objectMapper.readValue(jsonInsResp,
						new TypeReference<Map<String, Object>>() {
						});
				int code = HttpStatus.CREATED.value();
				Object codeObj = insMap.getOrDefault("statusCode", insMap.get("code"));
				if (codeObj != null) {
					try {
						code = Integer.parseInt(codeObj.toString());
					} catch (Exception ignore) {
					}
				}
				HttpStatus status = HttpStatus.resolve(code);
				if (status == null)
					status = HttpStatus.CREATED;

				ResponseDTO dto = ResponseDTO.builder().success(status.is2xxSuccessful())
						.message(String.valueOf(insMap.getOrDefault("message", "Creación realizada")))
						.code(status.value()).response(insMap).build();
				return ResponseEntity.status(status).body(dto);

			} else {
				String msg = String.valueOf(mapGet.getOrDefault("message", "Fallo consultando existencia"));
				ResponseDTO dto = ResponseDTO.builder().success(false).message(msg).code(statusGet).build();
				return ResponseEntity.status(statusGet).body(dto);
			}

		} catch (Exception e) {
			log.error("Error en crearTarifaConcepto (upsert single/batch)", e);
			ResponseDTO dto = ResponseDTO.builder().success(false)
					.message("Error inesperado al crear/actualizar tarifa concepto")
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(dto);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEmpresaId(Integer empresaId) {
		log.info("Buscar tarifas por id de empresa: {}", empresaId);
		try {
			List<TarifaConceptoEntity> conceptos = tarifaConceptoRepository.findByTipoTarifaEmpresaId(empresaId);

			if (conceptos.isEmpty()) {
				ResponseDTO notFound = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFound);
			}

			List<Integer> idsSinValor = conceptos.stream().filter(tc -> tc.getValor() == null)
					.map(TarifaConceptoEntity::getId).toList();

			final Map<Integer, List<ConceptoEstratoDTO>> estratosPorTc = idsSinValor.isEmpty() ? Collections.emptyMap()
					: conceptoEstratoRepository.findByTarifaConceptoIdInAndActivoTrue(idsSinValor).stream()
							.collect(java.util.stream.Collectors.groupingBy(ce -> ce.getTarifaConcepto().getId(),
									java.util.stream.Collectors.mapping(ce -> {
										ConceptoEstratoDTO dto = new ConceptoEstratoDTO();
										dto.setId(ce.getId());
										dto.setEstrato(ce.getEstrato());
										dto.setValor(ce.getValor());
										return dto;
									}, java.util.stream.Collectors.toList())));

			List<TarifaConceptoDTO> dtos = conceptos.stream().map(tc -> {
				TarifaConceptoDTO dto = tarifaConceptoMapper.entityToDto(tc);

				if (tc.getValor() == null) {
					dto.setPorEstrato(true);
					List<ConceptoEstratoDTO> lista = estratosPorTc.getOrDefault(tc.getId(), Collections.emptyList());
					if (!lista.isEmpty()) {
						lista.sort(java.util.Comparator.comparing(ConceptoEstratoDTO::getEstrato));
					}
					dto.setEstratos(lista);
				} else {
					dto.setPorEstrato(false);
					dto.setEstratos(Collections.emptyList());
				}
				return dto;
			}).toList();

			ResponseDTO ok = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtos).build();

			return ResponseEntity.ok(ok);

		} catch (Exception e) {
			log.error("Error al buscar tarifas por id de empresa: {}", empresaId, e);
			ResponseDTO err = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para ELIMINAR TarifaConcepto y sus ConceptoEstrato. id={}", id);
		try {
			if (id == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("El id es obligatorio").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			var tarifaOpt = tarifaConceptoRepository.findById(id);
			if (tarifaOpt.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.builder().success(false)
						.message(Constantes.RECORD_NOT_FOUND).code(HttpStatus.NOT_FOUND.value()).build());
			}

			int hijosEliminados = conceptoEstratoRepository.deleteByTarifaConcepto_Id(id);
			log.info("ConceptoEstrato eliminados por tarifaConcepto {}: {}", id, hijosEliminados);

			tarifaConceptoRepository.deleteById(id);
			log.info("TarifaConcepto {} eliminado", id);

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(Map.of("conceptosEstratoEliminados", hijosEliminados))
					.build());

		} catch (Exception e) {
			log.error("Error al eliminar la tarifa concepto con id: {}", id, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message(Constantes.DELETE_ERROR).code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> getTarifaConceptoEstrato(Integer idEmpresa, Integer idTipoTarifa,
			Integer idTipoConcepto) {
		log.info("Invocando SP get_tarifa_concepto_estrato con params: empresa={}, tipoTarifa={}, tipoConcepto={}",
				idEmpresa, idTipoTarifa, idTipoConcepto);

		try {
			String sql = """
					    SELECT public."get_tarifa_concepto_estrato"(
					        CAST(:idEmpresa AS int),
					        CAST(:idTipoTarifa AS int),
					        CAST(:idTipoConcepto AS int)
					    ) AS result
					""";

			MapSqlParameterSource params = new MapSqlParameterSource().addValue("idEmpresa", idEmpresa)
					.addValue("idTipoTarifa", idTipoTarifa).addValue("idTipoConcepto", idTipoConcepto);

			Map<String, Object> row = namedParameterJdbcTemplate.queryForMap(sql, params);

			Object resultObj = row.get("result");
			String jsonResult = null;
			if (resultObj instanceof PGobject pg && "jsonb".equalsIgnoreCase(pg.getType())) {
				jsonResult = pg.getValue();
			} else if (resultObj instanceof String s) {
				jsonResult = s;
			} else if (resultObj != null) {
				jsonResult = String.valueOf(resultObj);
			}

			if (jsonResult == null || jsonResult.isBlank()) {
				ResponseDTO dto = ResponseDTO.builder().success(false).message("El SP no retornó contenido.")
						.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(dto);
			}

			Map<String, Object> result = objectMapper.readValue(jsonResult,
					new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
					});

			int code = HttpStatus.OK.value();
			Object codeObj = result.getOrDefault("statusCode", result.get("code"));
			if (codeObj != null) {
				try {
					code = Integer.parseInt(codeObj.toString());
				} catch (NumberFormatException ex) {
					log.warn("El SP devolvió un código no numérico ('{}'). Usando {} por defecto.", codeObj, code);
				}
			}
			HttpStatus status = HttpStatus.resolve(code);
			if (status == null) {
				status = HttpStatus.OK;
			}

			String message = String.valueOf(result.getOrDefault("message", "Consulta realizada"));
			boolean success = status.is2xxSuccessful();

			ResponseDTO dto = ResponseDTO.builder().success(success).message(message).code(status.value())
					.response(result).build();

			return ResponseEntity.status(status).body(dto);

		} catch (Exception e) {
			log.error("Error inesperado al ejecutar get_tarifa_concepto_estrato", e);
			ResponseDTO dto = ResponseDTO.builder().success(false)
					.message("Error inesperado al consultar tarifa concepto estrato")
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(dto);
		}
	}

}
