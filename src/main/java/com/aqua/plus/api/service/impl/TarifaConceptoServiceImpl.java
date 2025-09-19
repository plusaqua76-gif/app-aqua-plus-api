package com.aqua.plus.api.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

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
		log.info("Invocando SP creartarifa_concepto con payload: {}", payload);
		try {
			String jsonString = objectMapper.writeValueAsString(payload);

			String sql = """
					    SELECT public."creartarifa_concepto"(CAST(:jsonData AS jsonb)) AS result
					""";
			MapSqlParameterSource params = new MapSqlParameterSource().addValue("jsonData", jsonString);
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
				String raw = codeObj.toString();
				try {
					code = Integer.parseInt(raw);
				} catch (NumberFormatException ex) {
					log.warn("El SP devolvió un código no numérico ('{}'). Usando {} por defecto.", raw, code);
				}
			}
			HttpStatus status = HttpStatus.resolve(code);
			if (status == null) {
				log.warn("Código HTTP inválido ({}) devuelto por el SP. Usando 200 OK.", code);
				status = HttpStatus.OK;
			}

			String message = String.valueOf(result.getOrDefault("message", "Operación realizada"));
			boolean success = status.is2xxSuccessful();

			ResponseDTO dto = ResponseDTO.builder().success(success).message(message).code(status.value())
					.response(result).build();

			return ResponseEntity.status(status).body(dto);

		} catch (JsonProcessingException e) {
			log.error("Error de procesamiento JSON", e);
			ResponseDTO dto = ResponseDTO.builder().success(false)
					.message("Error de procesamiento JSON: " + e.getOriginalMessage())
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(dto);

		} catch (Exception e) {
			log.error("Error inesperado al ejecutar crearTarifa_Concepto", e);
			ResponseDTO dto = ResponseDTO.builder().success(false)
					.message("Error inesperado al crear la tarifa/concepto")
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(dto);
		}
	}

	/**
	 * Invoca el SP public.actualizar_tarifa_concepto(jsonb) que retorna jsonb.
	 * Acepta un mapa (payload) que será serializado a JSON.
	 */
	@Transactional
	public ResponseEntity<ResponseDTO> actualizarTarifaConcepto(Map<String, Object> payload) {
		log.info("Invocando SP actualizar_tarifa_concepto con payload: {}", payload);
		try {
			String jsonString = objectMapper.writeValueAsString(payload);

			String sql = """
					    SELECT public.actualizar_tarifa_concepto(CAST(:jsonData AS jsonb)) AS result
					""";

			MapSqlParameterSource params = new MapSqlParameterSource().addValue("jsonData", jsonString);
			Map<String, Object> row = namedParameterJdbcTemplate.queryForMap(sql, params);

			Object resultObj = row.get("result");
			String jsonResult = null;

			if (resultObj instanceof org.postgresql.util.PGobject pg && "jsonb".equalsIgnoreCase(pg.getType())) {
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
				String raw = codeObj.toString();
				try {
					code = Integer.parseInt(raw);
				} catch (NumberFormatException ex) {
					log.warn("El SP devolvió un código no numérico ('{}'). Usando {} por defecto.", raw, code);
				}
			}
			HttpStatus status = HttpStatus.resolve(code);
			if (status == null) {
				log.warn("Código HTTP inválido ({}) devuelto por el SP. Usando 200 OK.", code);
				status = HttpStatus.OK;
			}

			String message = String.valueOf(result.getOrDefault("message", "Operación realizada"));
			boolean success = status.is2xxSuccessful();

			ResponseDTO dto = ResponseDTO.builder().success(success).message(message).code(status.value())
					.response(result).build();

			return ResponseEntity.status(status).body(dto);

		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			log.error("Error de procesamiento JSON", e);
			ResponseDTO dto = ResponseDTO.builder().success(false)
					.message("Error de procesamiento JSON: " + e.getOriginalMessage())
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(dto);

		} catch (org.springframework.dao.DataAccessException e) {
			log.error("Error de base de datos al ejecutar actualizar_tarifa_concepto", e);
			ResponseDTO dto = ResponseDTO.builder().success(false)
					.message("Error de base de datos al ejecutar el SP: " + e.getMostSpecificCause().getMessage())
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(dto);

		} catch (Exception e) {
			log.error("Error inesperado al ejecutar actualizar_tarifa_concepto", e);
			ResponseDTO dto = ResponseDTO.builder().success(false)
					.message("Error inesperado al actualizar la tarifa/concepto")
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
		log.info("Inicio método para eliminar tarifa concepto por id: {}", id);
		try {
			if (!tarifaConceptoRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			tarifaConceptoRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar la tarifa concepto con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
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
