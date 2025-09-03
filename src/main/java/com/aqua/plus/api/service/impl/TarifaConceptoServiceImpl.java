package com.aqua.plus.api.service.impl;

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
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TarifaConceptoServiceImpl implements ITarifaConceptoService {

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final ObjectMapper objectMapper;

	/**
	 * Invoca el SP public."crearTarifa_Concepto"(jsonb) que retorna jsonb. Acepta
	 * un mapa (payload) que será serializado a JSON.
	 */
	@Transactional
	public ResponseEntity<ResponseDTO> crearTarifaConcepto(Map<String, Object> payload) {
		log.info("Invocando SP crearTarifa_Concepto con payload: {}", payload);
		try {
			String jsonString = objectMapper.writeValueAsString(payload);

			String sql = """
					    SELECT public."crearTarifa_Concepto"(CAST(:jsonData AS jsonb)) AS result
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
}
