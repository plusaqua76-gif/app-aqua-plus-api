package com.aqua.plus.api.service.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.postgresql.util.PSQLException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.aqua.plus.api.service.IReporteFiltroService;
import com.aqua.plus.commons.dtos.ReporteFiltroDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ReporteFiltroEntity;
import com.aqua.plus.commons.maps.ReporteFiltroMapper;
import com.aqua.plus.commons.repositories.ReporteFiltroRepository;
import com.aqua.plus.commons.utils.Constantes;
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
public class ReporteFiltroServiceImpl implements IReporteFiltroService {

	private final ReporteFiltroRepository reporteFiltroRepository;
	private final ReporteFiltroMapper reporteFiltroMapper;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByReporteId(Integer idReporte) {
		log.info("Listar filtros de reporte por idReporte={}, solo activos", idReporte);

		if (idReporte == null) {
			return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
					.message("El parámetro idReporte es requerido.").code(HttpStatus.BAD_REQUEST.value()).build());
		}

		try {
			List<ReporteFiltroEntity> asociaciones = reporteFiltroRepository.findByReporte_IdAndActivoTrue(idReporte);

			List<ReporteFiltroDTO> items = reporteFiltroMapper.listEntityToDtoList(asociaciones);

			if (items.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron filtros activos para el reporte indicado.")
								.code(HttpStatus.NOT_FOUND.value()).response(List.of()).build());
			}

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(items).build());

		} catch (Exception e) {
			log.error("Error al listar filtros por idReporte={}", idReporte, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message(Constantes.CONSULTING_ERROR).code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> ejecutarFuncionJsonLista(String schema, String functionName,
			Map<String, Object> filtrosJson) {

		try {
			validarIdentificador(schema);
			validarIdentificador(functionName);

			String jsonString = objectMapper.writeValueAsString(filtrosJson);
			String sql = "SELECT " + schema + "." + functionName + "(:jsonData::jsonb)::text AS result";

			MapSqlParameterSource params = new MapSqlParameterSource().addValue("jsonData", jsonString);
			String result = namedParameterJdbcTemplate.queryForObject(sql, params, String.class);
			if (result == null || result.isBlank()) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "La función no retornó contenido.");
			}

			Map<String, Object> out = objectMapper.readValue(result,
					new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
					});

			String tip = String.valueOf(out.getOrDefault("out_tip_res", ""));
			String sqlState = String.valueOf(out.getOrDefault("out_cod_error", ""));
			String desc = String.valueOf(out.getOrDefault("out_desc_error", "Error desconocido"));

			if (!"OK".equalsIgnoreCase(tip)) {
				throw new ResponseStatusException(mapSqlStateToHttp(sqlState), desc);
			}

			return java.util.List.of(out);

		} catch (ResponseStatusException e) {
			throw e;
		} catch (DataAccessException e) {
			throw new ResponseStatusException(extractHttpStatusFromDataAccess(e), rootMessage(e), e);
		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Error procesando JSON: " + e.getOriginalMessage(), e);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, rootMessage(e), e);
		}
	}

	private HttpStatus mapSqlStateToHttp(String sqlState) {
		if (sqlState == null || sqlState.isBlank())
			return HttpStatus.INTERNAL_SERVER_ERROR;

		if (sqlState.startsWith("22"))
			return HttpStatus.BAD_REQUEST;

		if (sqlState.startsWith("23"))
			return HttpStatus.CONFLICT;

		if (sqlState.startsWith("40"))
			return HttpStatus.CONFLICT;

		if (sqlState.startsWith("08"))
			return HttpStatus.SERVICE_UNAVAILABLE;

		if ("P0001".equals(sqlState))
			return HttpStatus.BAD_REQUEST;

		return HttpStatus.INTERNAL_SERVER_ERROR;
	}

	private HttpStatus extractHttpStatusFromDataAccess(DataAccessException e) {
		Throwable cause = e.getRootCause();
		if (cause instanceof PSQLException) {
			String state = ((PSQLException) cause).getSQLState();
			return mapSqlStateToHttp(state);
		}
		if (cause instanceof SQLException) {
			String state = ((SQLException) cause).getSQLState();
			return mapSqlStateToHttp(state);
		}
		return HttpStatus.INTERNAL_SERVER_ERROR;
	}

	private String rootMessage(Throwable t) {
		Throwable c = t;
		while (c.getCause() != null && c.getCause() != c) {
			c = c.getCause();
		}
		String msg = c.getMessage();
		return (msg != null && !msg.isBlank()) ? msg : c.toString();
	}

	private static void validarIdentificador(String id) {
		if (id == null || !id.matches("^[A-Za-z_][A-Za-z0-9_]*$")) {
			throw new IllegalArgumentException("Identificador inválido: " + id);
		}
	}

}
