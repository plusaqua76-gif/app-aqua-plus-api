package com.aqua.plus.api.service.impl;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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

import com.aqua.plus.api.service.IEmpleadoEmpresaService;
import com.aqua.plus.api.service.impl.specification.EmpleadoEmpresaSpecification;
import com.aqua.plus.api.utils.EncriptarDesencriptar;
import com.aqua.plus.commons.dtos.EmpleadoEmpresaResponseDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.CorreoGeneralEntity;
import com.aqua.plus.commons.entities.EmpleadoEmpresaEntity;
import com.aqua.plus.commons.entities.TelefonoGeneralEntity;
import com.aqua.plus.commons.maps.EmpleadoEmpresaMapper;
import com.aqua.plus.commons.repositories.CorreoGeneralRepository;
import com.aqua.plus.commons.repositories.EmpleadoEmpresaRepository;
import com.aqua.plus.commons.repositories.TelefonoGeneralRepository;
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
public class EmpleadoEmpresaServiceImpl implements IEmpleadoEmpresaService {

	private final EmpleadoEmpresaRepository empleadoEmpresaRepository;
	private final EmpleadoEmpresaMapper empleadoEmpresaMapper;
	private final ObjectMapper objectMapper;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final TelefonoGeneralRepository telefonoGeneralRepository;
	private final CorreoGeneralRepository correoGeneralRepository;
	private final NotificacionServiceImpl notificacionServiceImpl;
	private final EncriptarDesencriptar encriptarDesencriptar;

	@Transactional
	public Map<String, Object> save(Map<String, Object> jsonParams) {
		try {
			String plainPassword = (String) jsonParams.get("password");
			if (plainPassword != null && !plainPassword.isBlank()) {
				String encodedPassword = encriptarDesencriptar.encriptar(plainPassword);
				jsonParams.put("password", encodedPassword);
			}

			String jsonString = objectMapper.writeValueAsString(jsonParams);
			String sql = "SELECT public.guardar_empleado_completo(CAST(:jsonData AS jsonb)) AS result";

			MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("jsonData", jsonString);

			Map<String, Object> row = namedParameterJdbcTemplate.queryForMap(sql, parameters);
			Object wrapped = row.get("result");

			if (wrapped instanceof org.postgresql.util.PGobject pg && "jsonb".equalsIgnoreCase(pg.getType())) {
				String jsonValue = pg.getValue();

				Map<String, Object> response = objectMapper.readValue(jsonValue,
						new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
						});

				Object statusObj = response.get("statusCode");
				Integer statusCode = (statusObj != null ? Integer.valueOf(String.valueOf(statusObj)) : null);

				String correoEmpleado = (String) jsonParams.get("correo");
				String nombreEmpleado = (String) jsonParams.get("primerNombre");
				String apellidoEmpleado = (String) jsonParams.get("primerApellido");
				String usuarioLogin = (String) jsonParams.get("nombreUsuario");

				if (statusCode != null && statusCode == 200) {
					if (correoEmpleado != null && !correoEmpleado.isBlank()) {
						try {
							Map<String, Object> data = new java.util.HashMap<>();
							data.put("nombre", nombreEmpleado != null ? nombreEmpleado : "Usuario");
							data.put("apellido", apellidoEmpleado);
							data.put("usuario", usuarioLogin);
							notificacionServiceImpl.enviarNotificacion(correoEmpleado, Constantes.INFO_ACTIVATE, data);
						} catch (Exception mailEx) {
							response.put("warningCorreo",
									"Empleado creado pero no se pudo enviar el correo: " + mailEx.getMessage());
							log.warn("No se pudo enviar correo de creación de empleado a {}: {}", correoEmpleado,
									mailEx.getMessage());
						}
					} else {
						response.put("warningCorreo",
								"Empleado creado pero no se envió correo porque no se recibió email.");
					}
				}

				return response;
			}

			return Map.of("error", "No se pudo leer la respuesta JSON del SP.");

		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			log.error("Error de procesamiento JSON en save empleado", e);
			return Map.of("error", "Error de procesamiento JSON: " + e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado en guardarEmpleado", e);
			return Map.of("error", "Error inesperado: " + e.getMessage());
		}
	}

	@Transactional
	public Map<String, Object> update(Map<String, Object> jsonParams) {
		try {
			String jsonString = objectMapper.writeValueAsString(jsonParams);

			String sql = "SELECT * FROM public.actualizar_empleado(CAST(:jsonData AS jsonb))";

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("jsonData", jsonString);

			Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

			Object wrappedValue = rawResult.get("actualizar_empleado");
			if (wrappedValue instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {
				String jsonValue = pgObject.getValue();
				return objectMapper.readValue(jsonValue, new TypeReference<Map<String, Object>>() {
				});
			}

			return Map.of("error", "El resultado no pudo ser procesado correctamente.");

		} catch (JsonProcessingException e) {
			log.error("Error de procesamiento JSON", e);
			return Map.of("error", "Error de procesamiento JSON: " + e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado en actualizarEmpleado", e);
			return Map.of("error", "Error inesperado: " + e.getMessage());
		}
	}

	@Transactional
	public Map<String, Object> actualizarEstadoPersona(Map<String, Object> jsonParams) {
		try {
			String jsonString = objectMapper.writeValueAsString(jsonParams);

			String sql = "SELECT * FROM public.actualizar_estado_por_persona(CAST(:jsonData AS jsonb))";

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("jsonData", jsonString);

			Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

			Object wrappedValue = rawResult.get("actualizar_estado_por_persona");
			if (wrappedValue instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {
				String jsonValue = pgObject.getValue();
				return objectMapper.readValue(jsonValue, new TypeReference<Map<String, Object>>() {
				});
			}

			return Map.of("error", "El resultado no pudo ser procesado correctamente.");

		} catch (JsonProcessingException e) {
			log.error("Error de procesamiento JSON", e);
			return Map.of("error", "Error de procesamiento JSON: " + e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado en actualizarEstadoPersona", e);
			return Map.of("error", "Error inesperado: " + e.getMessage());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
		log.info("Buscar empleado empresa por id: {}", id);
		try {
			Optional<EmpleadoEmpresaEntity> empleadoEmpresa = empleadoEmpresaRepository.findById(id);
			if (empleadoEmpresa.isPresent()) {
				EmpleadoEmpresaResponseDTO dto = empleadoEmpresaMapper.entityToResumenDto(empleadoEmpresa.get());
				ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
						.code(HttpStatus.OK.value()).response(dto).build();
				return ResponseEntity.ok(responseDTO);
			} else {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
		} catch (Exception e) {
			log.error("Error al buscar empleado empresa por id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEmpresaId(Integer empresaId, Pageable pageable, String nombreCompleto,
			String cedula, String codigo, String telefono, String correo, String estado) {
		log.info(
				"Buscando empleados empresa={}, filtros: nombre={}, cedula={}, codigo={}, tel={}, correo={}, estado={}",
				empresaId, nombreCompleto, cedula, codigo, telefono, correo, estado);

		try {
			Boolean estadoBool = null;
			if (estado != null) {
				if ("ACTIVO".equalsIgnoreCase(estado))
					estadoBool = Boolean.TRUE;
				else if ("INACTIVO".equalsIgnoreCase(estado))
					estadoBool = Boolean.FALSE;
			}

			Specification<EmpleadoEmpresaEntity> spec = EmpleadoEmpresaSpecification.allOfNonNull(
					EmpleadoEmpresaSpecification.belongsToEmpresa(empresaId),
					EmpleadoEmpresaSpecification.personaNombreCompletoLike(nombreCompleto),
					EmpleadoEmpresaSpecification.personaCedulaEquals(cedula),
					EmpleadoEmpresaSpecification.personaCodigoLike(codigo),
					EmpleadoEmpresaSpecification.telefonoLike(telefono),
					EmpleadoEmpresaSpecification.correoLike(correo),
					EmpleadoEmpresaSpecification.personaEstadoEquals(estadoBool));

			Pageable pageToUse = (pageable != null ? pageable : Pageable.unpaged());
			Page<EmpleadoEmpresaEntity> page = empleadoEmpresaRepository.findAll(spec, pageToUse);

			var dtoList = empleadoEmpresaMapper.listEntityToResumenDtoList(page.getContent());

			var personaIds = dtoList.stream().map(EmpleadoEmpresaResponseDTO::getPersonaId).filter(Objects::nonNull)
					.distinct().toList();

			if (!personaIds.isEmpty()) {
				var correos = correoGeneralRepository.findLatestByPersonaIds(personaIds);
				var correoByPersona = correos.stream().filter(cg -> cg.getPersona() != null).collect(
						Collectors.toMap(cg -> cg.getPersona().getId(), CorreoGeneralEntity::getCorreo, (a, b) -> a));

				var telefonos = telefonoGeneralRepository.findLatestByPersonaIds(personaIds);
				var telefonoByPersona = telefonos.stream().filter(tg -> tg.getPersona() != null).collect(
						Collectors.toMap(tg -> tg.getPersona().getId(), TelefonoGeneralEntity::getNumero, (a, b) -> a));

				dtoList.forEach(dto -> {
					Integer pid = dto.getPersonaId();
					if (pid != null) {
						dto.setCorreo(correoByPersona.get(pid));
						dto.setTelefono(telefonoByPersona.get(pid));
					}
				});
			}

			long totalCount = page.getTotalElements();
			int totalPages = page.getTotalPages();
			int currentPage = page.getNumber();
			int pageSize = page.getSize();

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).totalCount(totalCount).pageSize(pageSize)
					.currentPage(currentPage).totalPages(totalPages).build());

		} catch (Exception e) {
			log.error("Error al consultar empleados por empresaId: {}", empresaId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todos los empleado empresa");
		try {
			var list = empleadoEmpresaRepository.findAll();
			var dtoList = empleadoEmpresaMapper.listEntityToResumenDtoList(list);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar los empleados empresas", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar empelado empresa por id: {}", id);
		try {
			if (!empleadoEmpresaRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			empleadoEmpresaRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar el empleado empresa con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

}
