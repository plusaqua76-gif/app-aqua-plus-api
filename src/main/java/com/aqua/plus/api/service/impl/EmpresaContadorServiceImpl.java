package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IEmpresaContadorService;
import com.aqua.plus.commons.dtos.ContadorDTO;
import com.aqua.plus.commons.dtos.EmpresaContadorDTO;
import com.aqua.plus.commons.dtos.LecturaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ContadorEntity;
import com.aqua.plus.commons.entities.EmpresaContadorEntity;
import com.aqua.plus.commons.entities.EmpresaEntity;
import com.aqua.plus.commons.maps.EmpresaContadorMapper;
import com.aqua.plus.commons.repositories.ContadorRepository;
import com.aqua.plus.commons.repositories.EmpresaContadorRepository;
import com.aqua.plus.commons.repositories.EmpresaRepository;
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
public class EmpresaContadorServiceImpl implements IEmpresaContadorService {

	private final EmpresaContadorRepository empresaContadorRepository;
	private final EmpresaRepository empresaRepository;
	private final ContadorRepository contadorRepository;
	private final EmpresaContadorMapper empresaContadorMapper;
	private final LecturaServiceImpl lecturaServiceImpl;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final ObjectMapper objectMapper;

	@Transactional
	public ResponseEntity<ResponseDTO> saveEmpresaContadorAndLectura(EmpresaContadorDTO ecDTO, LecturaDTO lecturaDTO) {
		log.info("Orquestar guardado EmpresaContador + Lectura (empresaId={}, contadorId={})",
				(ecDTO.getEmpresa() != null ? ecDTO.getEmpresa().getId() : null),
				(ecDTO.getContador() != null ? ecDTO.getContador().getId() : null));

		try {
			Integer empresaId = (ecDTO.getEmpresa() != null) ? ecDTO.getEmpresa().getId() : null;
			Integer contadorId = (ecDTO.getContador() != null) ? ecDTO.getContador().getId() : null;

			if (empresaId == null || contadorId == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.code(HttpStatus.BAD_REQUEST.value()).message("Debe indicar empresa y contador").build());
			}

			var optEC = empresaContadorRepository.findByEmpresa_Id(empresaId);
			boolean createdEC = false;
			EmpresaContadorEntity ecEntity;

			if (optEC.isEmpty()) {
				EmpresaEntity empresa = empresaRepository.findById(empresaId)
						.orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
				ContadorEntity contador = contadorRepository.findById(contadorId)
						.orElseThrow(() -> new RuntimeException("Contador no encontrado"));

				ecEntity = new EmpresaContadorEntity();
				ecEntity.setEmpresa(empresa);
				ecEntity.setContador(contador);
				ecEntity.setActivo(true);
				ecEntity.setUsuarioCreacion(ecDTO.getUsuarioCreacion() != null ? ecDTO.getUsuarioCreacion()
						: lecturaDTO.getUsuarioCreacion());
				ecEntity.setFechaCreacion(new Date());

				ecEntity = empresaContadorRepository.save(ecEntity);
				createdEC = true;

			} else {
				ecEntity = optEC.get();

				Integer contadorPersistidoId = (ecEntity.getContador() != null) ? ecEntity.getContador().getId() : null;
				if (contadorPersistidoId != null && !contadorPersistidoId.equals(contadorId)) {
					return ResponseEntity.status(HttpStatus.CONFLICT)
							.body(ResponseDTO.builder().success(false).code(HttpStatus.CONFLICT.value()).message(
									"La empresa ya tiene un contador asociado distinto. No se permite modificar EmpresaContador.")
									.build());
				}
			}

			lecturaDTO.setContador(new ContadorDTO());
			lecturaDTO.getContador().setId(ecEntity.getContador().getId());

			ResponseEntity<ResponseDTO> lecturaResp = lecturaServiceImpl.save(lecturaDTO);

			if (!lecturaResp.getStatusCode().is2xxSuccessful()) {
				throw new RuntimeException("No se pudo guardar la lectura: "
						+ (lecturaResp.getBody() != null ? lecturaResp.getBody().getMessage() : "error"));
			}

			Object lecturaGuardada = lecturaResp.getBody() != null ? lecturaResp.getBody().getResponse() : null;
			Object ecDTOResponse = (empresaContadorMapper != null) ? empresaContadorMapper.entityToDto(ecEntity)
					: ecEntity;

			Map<String, Object> payload = new HashMap<>();
			payload.put("empresaContador", ecDTOResponse);
			payload.put("lectura", lecturaGuardada);
			payload.put("empresaContadorCreado", createdEC);

			int code = createdEC ? HttpStatus.CREATED.value() : HttpStatus.OK.value();
			String msg = createdEC ? "EmpresaContador creado y lectura guardada"
					: "Lectura guardada para EmpresaContador existente";

			return ResponseEntity.status(code)
					.body(ResponseDTO.builder().success(true).code(code).message(msg).response(payload).build());

		} catch (Exception e) {
			log.error("Error orquestando EmpresaContador + Lectura", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
					.code(HttpStatus.BAD_REQUEST.value()).message(Constantes.SAVE_ERROR).build());
		}
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEmpresaId(Integer idEmpresa) {
		log.info("Consultar EmpresaContador por idEmpresa={}", idEmpresa);
		try {
			var opt = empresaContadorRepository.findWithContadorByEmpresa_Id(idEmpresa);
			if (opt.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false).code(HttpStatus.NOT_FOUND.value())
								.message("No se encontró EmpresaContador para la empresa " + idEmpresa).build());
			}

			var entity = opt.get();

			var dto = empresaContadorMapper.entityToDto(entity);

			return ResponseEntity.ok(ResponseDTO.builder().success(true).code(HttpStatus.OK.value())
					.message("Consulta exitosa").response(dto).build());

		} catch (Exception e) {
			log.error("Error consultando EmpresaContador por empresa {}", idEmpresa, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).code(HttpStatus.INTERNAL_SERVER_ERROR.value())
							.message("Error consultando EmpresaContador").build());
		}
	}
	
	@Transactional(readOnly = true)
	public Map<String, Object> metricasLecturaSuperContador(Integer empresaId, Integer anio, Integer mes) {

	    String sql = """
	            SELECT public.fn_metricas_consumo_mes_empresa(:idEmpresa, :anio, :mes)::text AS payload
	            """;

	    MapSqlParameterSource params = new MapSqlParameterSource()
	            .addValue("idEmpresa", empresaId, java.sql.Types.INTEGER)
	            .addValue("anio", anio, java.sql.Types.INTEGER)
	            // p_mes en el SP puede ser NULL => todo el año
	            .addValue("mes", mes, java.sql.Types.INTEGER);

	    try {
	        String json = namedParameterJdbcTemplate.queryForObject(sql, params, String.class);

	        // El SP siempre retorna algo (jsonb_build_object), pero por seguridad:
	        if (json == null || json.isBlank()) {
	            // Mantenemos el mismo contrato del SP
	            return Map.of(
	                    "success", false,
	                    "message", "SIN_DATOS_DESDE_SP",
	                    "code", 500,
	                    "response", null
	            );
	        }

	        // Pasamos tal cual el JSON del SP a Map<String,Object>
	        return objectMapper.readValue(json,
	                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

	    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
	        log.error("Error parseando JSON de fn_metricas_consumo_mes_empresa. empresaId={}, anio={}, mes={}",
	                empresaId, anio, mes, e);

	        return Map.of(
	                "success", false,
	                "message", "Error parseando JSON de fn_metricas_consumo_mes_empresa",
	                "code", 500,
	                "response", Map.of(
	                        "detalle", e.getOriginalMessage() != null ? e.getOriginalMessage() : e.getMessage()
	                )
	        );

	    } catch (org.springframework.dao.DataAccessException e) {
	        log.error("Error de acceso a datos al ejecutar fn_metricas_consumo_mes_empresa. empresaId={}, anio={}, mes={}",
	                empresaId, anio, mes, e);

	        String detalle = e.getMostSpecificCause() != null
	                ? e.getMostSpecificCause().getMessage()
	                : e.getMessage();

	        return Map.of(
	                "success", false,
	                "message", "Error de base de datos al ejecutar fn_metricas_consumo_mes_empresa",
	                "code", 500,
	                "response", Map.of("detalle", detalle)
	        );

	    } catch (Exception e) {
	        log.error("Error inesperado ejecutando fn_metricas_consumo_mes_empresa. empresaId={}, anio={}, mes={}",
	                empresaId, anio, mes, e);

	        return Map.of(
	                "success", false,
	                "message", "Error inesperado ejecutando fn_metricas_consumo_mes_empresa",
	                "code", 500,
	                "response", Map.of("detalle", e.getMessage())
	        );
	    }
	}

}
