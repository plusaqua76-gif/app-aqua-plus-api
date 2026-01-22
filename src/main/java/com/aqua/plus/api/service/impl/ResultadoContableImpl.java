package com.aqua.plus.api.service.impl;

import java.time.LocalDate;
import java.util.Map;

import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.aqua.plus.api.service.IResultadoContableMesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResultadoContableImpl implements IResultadoContableMesService {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Map<String, Object> obtenerResultadoContableMesMap(Integer idEmpresa, Integer anio, Integer mes,
            LocalDate fechaDesde, LocalDate fechaHasta) {
        try {
            String sql = "SELECT * FROM public.fn_resultado_economico_mes(:idEmpresa, :anio, :mes, :fechaDesde, :fechaHasta)";
            MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("idEmpresa", idEmpresa, java.sql.Types.INTEGER)
                .addValue("anio", anio, java.sql.Types.INTEGER)
                .addValue("mes", mes, java.sql.Types.INTEGER)
                .addValue("fechaDesde", fechaDesde, java.sql.Types.DATE)
                .addValue("fechaHasta", fechaHasta, java.sql.Types.DATE);
            
            Map<String, Object> rawResult = namedParameterJdbcTemplate
                .queryForMap(sql, parameters);          
            Object wrappedValue = rawResult.get("fn_resultado_economico_mes");
            
            if (wrappedValue instanceof PGobject pgObject && 
                "jsonb".equals(pgObject.getType())) {
                String jsonValue = pgObject.getValue();
                return objectMapper.readValue(jsonValue, 
                    new TypeReference<Map<String, Object>>() {});
            }
            
            return Map.of("error", "El resultado no pudo ser procesado correctamente.");
            
        } catch (JsonProcessingException e) {
            log.error("Error de procesamiento JSON", e);
            return Map.of("error", "Error de procesamiento JSON: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado en consumirStoredProcedure", e);
            return Map.of("error", "Error inesperado: " + e.getMessage());
        }
    }
}