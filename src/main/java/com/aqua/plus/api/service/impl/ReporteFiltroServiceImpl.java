package com.aqua.plus.api.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IReporteFiltroService;
import com.aqua.plus.commons.dtos.ReporteFiltroDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ReporteFiltroEntity;
import com.aqua.plus.commons.maps.ReporteFiltroMapper;
import com.aqua.plus.commons.repositories.ReporteFiltroRepository;
import com.aqua.plus.commons.utils.Constantes;
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
    public List<Map<String, Object>> ejecutarFuncionJsonLista(
            String schema, String functionName, Map<String, Object> filtrosJson) {

        try {
            validarIdentificador(schema);
            validarIdentificador(functionName);

            String jsonString = objectMapper.writeValueAsString(filtrosJson);

            String sql = "SELECT (" + schema + "." + functionName + "(:jsonData::jsonb))::text AS result";

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("jsonData", jsonString);

            List<String> rows = namedParameterJdbcTemplate.query(
                    sql, params, (rs, i) -> rs.getString("result"));

            List<Map<String, Object>> parsed = new java.util.ArrayList<>();
            for (String row : rows) {
                if (row != null && !row.isBlank()) {
                    parsed.add(objectMapper.readValue(row, new TypeReference<Map<String, Object>>() {}));
                }
            }
            return parsed;
        } catch (JsonProcessingException e) {
            return List.of(Map.of("error", "Error procesando JSON: " + e.getMessage()));
        } catch (Exception e) {
            return List.of(Map.of("error", "Error inesperado: " + e.getMessage()));
        }
    }

    private static void validarIdentificador(String id) {
        if (id == null || !id.matches("^[A-Za-z_][A-Za-z0-9_]*$")) {
            throw new IllegalArgumentException("Identificador inválido: " + id);
        }
    }

}
