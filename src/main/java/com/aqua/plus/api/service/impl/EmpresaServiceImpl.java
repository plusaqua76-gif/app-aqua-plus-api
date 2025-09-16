package com.aqua.plus.api.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.configs.security.utils.JwtUtil;
import com.aqua.plus.api.service.IEmpresaService;
import com.aqua.plus.api.utils.EncriptarDesencriptar;
import com.aqua.plus.commons.dtos.EmpresaDTO;
import com.aqua.plus.commons.dtos.EmpresaResponseDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.CiudadEntity;
import com.aqua.plus.commons.entities.DepartamentoEntity;
import com.aqua.plus.commons.entities.EmpresaEntity;
import com.aqua.plus.commons.maps.EmpresaMapper;
import com.aqua.plus.commons.repositories.CiudadRepository;
import com.aqua.plus.commons.repositories.CorreoGeneralRepository;
import com.aqua.plus.commons.repositories.DepartamentoRepository;
import com.aqua.plus.commons.repositories.EmpresaRepository;
import com.aqua.plus.commons.utils.Constantes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmpresaServiceImpl implements IEmpresaService {

	private final EmpresaRepository empresaRepository;
	private final EmpresaMapper empresaMapper;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final ObjectMapper objectMapper;
	private final EncriptarDesencriptar encriptarDesencriptar;
	private final NotificacionServiceImpl notificacionServiceImpl;
	private final DepartamentoRepository departamentoRepository;
	private final CiudadRepository ciudadRepository;
	private final DocumentoServiceImpl documentoService;
	private final CorreoGeneralRepository correoGeneralRepository;
	private final JwtUtil jwtUtil;

	private final DocumentoServiceImpl documentoServiceImpl;

	@Value("${mail.username}")
	private String correoAquaPlus;

	@Value("${link.recover}")
	private String linkRecover;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(EmpresaDTO empresaDTO) {
		log.info("Creando Empresa");
		try {
			EmpresaEntity entity = empresaMapper.dtoToEntity(empresaDTO);
			entity.setFechaCreacion(new Date());
			entity.setUsuarioCreacion(empresaDTO.getUsuarioCreacion());
			entity.setActivo(true);

			EmpresaEntity saved = empresaRepository.save(entity);
			EmpresaDTO savedDTO = empresaMapper.entityToDto(saved);

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.SAVED_SUCCESSFULLY)
					.code(HttpStatus.CREATED.value()).response(savedDTO).build();

			return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
		} catch (Exception e) {
			log.error("Error creando la Empresa", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Transactional
	public Map<String, Object> updateEmpresaDireccion(Map<String, Object> jsonParams) {
		try {
			String jsonString = objectMapper.writeValueAsString(jsonParams);
			String sql = "SELECT * FROM public.actualizar_empresa_direccion(CAST(:jsonData AS jsonb))";
			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("jsonData", jsonString);
			Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);
			Object wrappedValue = rawResult.get("actualizar_empresa_direccion");
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
			log.error("Error inesperado en actualizarEmpresaDireccion", e);
			return Map.of("error", "Error inesperado: " + e.getMessage());
		}
	}

	@Transactional
	public Map<String, Object> registrarEmpresa(Map<String, Object> jsonParams) {
		try {
			/* ---- Encriptar contraseña si viene en claro ---- */
			String plainPassword = (String) jsonParams.get("password");
			if (plainPassword != null) {
				String encodedPassword = encriptarDesencriptar.encriptar(plainPassword);
				jsonParams.put("password", encodedPassword);
			}

			/* ---- Invocar SP ---- */
			String jsonString = objectMapper.writeValueAsString(jsonParams);
			String sql = "SELECT public.crear_o_actualizar_empresa(CAST(:jsonData AS jsonb)) AS result";
			MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("jsonData", jsonString);

			Map<String, Object> row = namedParameterJdbcTemplate.queryForMap(sql, parameters);
			Object wrapped = row.get("result");

			if (wrapped instanceof org.postgresql.util.PGobject pg && "jsonb".equalsIgnoreCase(pg.getType())) {
				String jsonValue = pg.getValue();
				Map<String, Object> response = objectMapper.readValue(jsonValue,
						new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
						});

				Object codeObj = response.get("code");
				Integer code = (codeObj != null ? Integer.valueOf(String.valueOf(codeObj)) : null);
				boolean success = Boolean.TRUE.equals(response.get("success"));

				if (success && Integer.valueOf(201).equals(code)) {
					// ========== Notificación (tu lógica existente) ==========
					Integer idDepartamento = Integer.valueOf(String.valueOf(jsonParams.get("idDepartamento")));
					Integer idCiudad = Integer.valueOf(String.valueOf(jsonParams.get("idCiudad")));

					String nombreDepartamento = departamentoRepository.findById(idDepartamento)
							.map(DepartamentoEntity::getNombre).orElse("Desconocido");

					String nombreCiudad = ciudadRepository.findById(idCiudad).map(CiudadEntity::getNombre)
							.orElse("Desconocido");

					String nombreEmpresa = (String) jsonParams.get("nombreEmpresa");
					String nit = (String) jsonParams.get("nit");
					String correoEmpresa = (String) jsonParams.get("correo");
					String telefono = (String) jsonParams.get("telefono");

					if (correoAquaPlus != null && !correoAquaPlus.isBlank()) {
						Map<String, Object> data = new java.util.HashMap<>();
						data.put(Constantes.PARAMETRO_NAME_ENTERPRISE, nombreEmpresa);
						data.put(Constantes.PARAMETRO_NIT, nit);
						data.put(Constantes.PARAMETRO_EMAIL, correoEmpresa);
						data.put(Constantes.PARAMETRO_PHONE, telefono);
						data.put(Constantes.PARAMETRO_DEPARTAMENT, nombreDepartamento);
						data.put(Constantes.PARAMETRO_CITY, nombreCiudad);
						log.info("Info data notificacion {}", data);
						notificacionServiceImpl.enviarNotificacion(correoAquaPlus, Constantes.INFO_ACTIVATE, data);
					}

					// ========== Orquestación de imagen (OPCIONAL) ==========
					Object dataObj = response.get("data");
					Integer idEmpresaCreada = null;

					if (dataObj instanceof Map<?, ?> dataMap) {
						Object idEmp = dataMap.get("id_empresa");
						if (idEmp != null) {
							idEmpresaCreada = Integer.valueOf(String.valueOf(idEmp));
						}
					}

					String base64Imagen = (String) jsonParams.get("imagen");
					String extensionImg = (String) jsonParams.get("extensionImagen");
					String nombreArchivoImagen = (String) jsonParams.get("nombreArchivoImagen");
					String usuario = (String) jsonParams.getOrDefault("usuario", "system");
					String categoriaCodigo = (String) jsonParams.get("categoriaCodigoImagen");

					if (nombreArchivoImagen == null || nombreArchivoImagen.isBlank()) {
						nombreArchivoImagen = (nombreEmpresa != null && !nombreEmpresa.isBlank())
								? nombreEmpresa.replaceAll("[^A-Za-z0-9_-]", "_")
								: "logo_empresa";
					}

					if (idEmpresaCreada != null && base64Imagen != null && !base64Imagen.isBlank()) {
						try {
							var respDoc = documentoServiceImpl.saveDocumentoBase64(base64Imagen, idEmpresaCreada, null,
									nombreArchivoImagen, extensionImg, usuario, categoriaCodigo);

							if (respDoc.getStatusCode().is2xxSuccessful() && respDoc.getBody() != null
									&& Boolean.TRUE.equals(respDoc.getBody().getSuccess())) {
								response.put("documento", respDoc.getBody().getResponse());
							} else {
								String msg = (respDoc.getBody() != null ? respDoc.getBody().getMessage()
										: "Fallo subiendo imagen a Azure");
								response.put("warningDocumento", msg);
							}
						} catch (Exception exUp) {
							log.warn("No se pudo cargar la imagen en Azure: {}", exUp.getMessage());
							response.put("warningDocumento", "No se pudo cargar la imagen en Azure");
						}
					}
				}

				return response;
			}

			return Map.of(Constantes.ERROR_KEY, "No se pudo leer la respuesta JSON del SP.");

		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			log.error("Error JSON", e);
			return java.util.Collections.singletonMap(Constantes.ERROR_KEY,
					"Error de procesamiento JSON: " + e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado", e);
			return java.util.Collections.singletonMap(Constantes.ERROR_KEY, "Error inesperado: " + e.getMessage());
		}
	}

	@Transactional
	public Map<String, Object> updateEnterprise(Map<String, Object> jsonParams) {
		try {
			String jsonString = objectMapper.writeValueAsString(jsonParams);
			String sql = "SELECT public.actualizar_estado_empresa(CAST(:jsonData AS jsonb)) AS result";
			MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("jsonData", jsonString);
			Map<String, Object> row = namedParameterJdbcTemplate.queryForMap(sql, parameters);

			Object wrapped = row.get("result");
			String jsonValue;
			if (wrapped instanceof org.postgresql.util.PGobject pg && "jsonb".equalsIgnoreCase(pg.getType())) {
				jsonValue = pg.getValue();
			} else if (wrapped instanceof String s) {
				jsonValue = s;
			} else {
				return Map.of(Constantes.ERROR_KEY, "El resultado no pudo ser procesado correctamente.");
			}

			Map<String, Object> response = objectMapper.readValue(jsonValue,
					new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
					});

			Integer code = null;
			Object codeObj = (response.containsKey("code") ? response.get("code") : response.get("statusCode"));
			if (codeObj instanceof Number n)
				code = n.intValue();
			else if (codeObj != null) {
				try {
					code = Integer.valueOf(codeObj.toString());
				} catch (Exception ignored) {
				}
			}
			if (code == null || (code != 200 && code != 201))
				return response;

			Integer idEmpresa = null;
			Object idEmpObj = jsonParams.get("idEmpresa");
			if (idEmpObj instanceof Number n)
				idEmpresa = n.intValue();
			else if (idEmpObj != null) {
				try {
					idEmpresa = Integer.valueOf(idEmpObj.toString());
				} catch (Exception ignored) {
				}
			}
			if (idEmpresa == null) {
				response.put("notice", "idEmpresa ausente o inválido; no se intentó enviar correo.");
				return response;
			}

			String correoEmpresa = correoGeneralRepository.findCorreoPrincipalByEmpresaId(idEmpresa).orElse(null);
			if (correoEmpresa == null || correoEmpresa.isBlank()) {
				response.put("notice", "La empresa no tiene un correo activo registrado. No se envió la notificación.");
				return response;
			}

			String nombreEmpresa = (jsonParams.get("nombreEmpresa") == null) ? null
					: String.valueOf(jsonParams.get("nombreEmpresa"));
			String usuarioEmpresa = (jsonParams.get("usuario") == null) ? null
					: String.valueOf(jsonParams.get("usuario"));
			if (usuarioEmpresa == null || usuarioEmpresa.isBlank()) {
				response.put("notice",
						"'usuario' es requerido para generar el token de recuperación. No se envió notificación.");
				return response;
			}
			String tiempoLegible = notificacionServiceImpl
					.obtenerTiempoVigenciaLegible(Constantes.TIEMPO_VIGENCIA_EXTERNO);

			String token = jwtUtil.generateToken(usuarioEmpresa, Constantes.KEY_TOKEN_EXTERNO,
					Constantes.TIEMPO_VIGENCIA_EXTERNO);

			String encodedToken = java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);

			String baseRecover = (this.linkRecover == null) ? "" : this.linkRecover;

			baseRecover = baseRecover.replaceAll("(?i)([?&])Authorization=$", "$1");

			String recoverLink;
			if (baseRecover.endsWith("?") || baseRecover.endsWith("&")) {
				recoverLink = baseRecover + encodedToken;
			} else if (baseRecover.contains("?")) {
				recoverLink = baseRecover + "&" + encodedToken;
			} else {
				recoverLink = baseRecover + "?" + encodedToken;
			}

			Map<String, Object> data = new HashMap<>();
			data.put(Constantes.PARAMETRO_NAME_USER, nombreEmpresa);
			data.put(Constantes.PARAMETRO_USER, usuarioEmpresa);
			data.put(Constantes.PARAMETRO_LINK_RECOVER, recoverLink);
			data.put(Constantes.PARAMETRO_HOURS, tiempoLegible);

			String recoverLinkMasked = recoverLink.replaceAll("(?i)(Authorization=)[^&]+", "$1***");
			log.info("Info data notificacion (empresa {}): [nameUser={}, user={}, linkRecover={}, hours={}]", idEmpresa,
					nombreEmpresa, usuarioEmpresa, recoverLinkMasked, tiempoLegible);

			try {
				notificacionServiceImpl.enviarNotificacion(correoEmpresa, Constantes.CREATE_PASSWORD, data);
				response.put("emailSent", true);
				response.put("emailTo", correoEmpresa);
			} catch (Exception mailEx) {
				log.error("Fallo enviando notificación a {}", correoEmpresa, mailEx);
				response.put("emailSent", false);
				response.put("emailError", mailEx.getMessage());
			}

			return response;

		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			log.error("Error de procesamiento JSON", e);
			return Map.of(Constantes.ERROR_KEY, "Error de procesamiento JSON: " + e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado en updateEnterprise", e);
			return Map.of(Constantes.ERROR_KEY, "Error inesperado: " + e.getMessage());
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> update(EmpresaDTO empresaDTO) {
		log.info("Actualizando Empresa");
		try {
			if (empresaDTO.getId() == null || !empresaRepository.existsById(empresaDTO.getId())) {
				throw new IllegalArgumentException(Constantes.EMP_NOT_FOUND);
			}

			EmpresaEntity entity = empresaRepository.findById(empresaDTO.getId()).orElseThrow();
			empresaMapper.updateEntityFromDto(empresaDTO, entity);
			entity.setFechaModificacion(new Date());
			entity.setUsuarioModificacion(empresaDTO.getUsuarioModificacion());

			EmpresaEntity updated = empresaRepository.save(entity);
			EmpresaDTO updatedDTO = empresaMapper.entityToDto(updated);

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.UPDATED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(updatedDTO).build();

			return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
		} catch (Exception e) {
			log.error("Error actualizando la Empresa", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.UPDATE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> getAllEnterpriseResponseId() {
		log.info("Consultar todas las empresas y se muestra en el response solo los id de los registros:");
		try {
			List<EmpresaEntity> list = empresaRepository.findAll();

			List<Map<String, Object>> idList = list.stream().map(e -> Map.<String, Object>of("id", e.getId())).toList();

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(idList).build();

			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al consultar por id de empresa", e);

			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
		log.info("Buscar Empresa por id: {}", id);
		try {
			Optional<EmpresaEntity> empresa = empresaRepository.findById(id);
			if (empresa.isPresent()) {
				EmpresaResponseDTO dto = empresaMapper.entityToResumenDto(empresa.get());
				ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
						.code(HttpStatus.OK.value()).response(dto).build();
				return ResponseEntity.ok(responseDTO);
			} else {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false)
						.message("No se encontró la empresa con el ID especificado").code(HttpStatus.NOT_FOUND.value())
						.build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
		} catch (Exception e) {
			log.error("Error al buscar Empresa por id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByUsuarioId(Integer idUsuario) {
		log.info("Buscar Empresa por id de usuario: {}", idUsuario);
		try {
			Optional<EmpresaEntity> empresaOpt = empresaRepository.findByUsuario_Id(idUsuario);

			if (empresaOpt.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontró una empresa asociada al usuario")
								.code(HttpStatus.NOT_FOUND.value()).build());
			}

			EmpresaEntity empresa = empresaOpt.get();
			Integer idEmpresa = empresa.getId();

			Map<String, Object> responseMap = new LinkedHashMap<>();
			responseMap.put("idEmpresa", idEmpresa);
			responseMap.put("nombre", empresa.getNombre());

			// Correo principal (si existe)
			correoGeneralRepository.findCorreoPrincipalByEmpresaId(idEmpresa)
					.ifPresent(correo -> responseMap.put("correo", correo));

			// Documentos (con base64) orquestando DocumentoService
			List<?> documentos = Collections.emptyList();
			Long totalDocs = 0L;

			try {
				ResponseEntity<ResponseDTO> docsResp = documentoService.listarPorEmpresaConBase64(idEmpresa);
				if (docsResp != null && docsResp.getStatusCode().is2xxSuccessful() && docsResp.getBody() != null) {
					ResponseDTO body = docsResp.getBody();
					if (Boolean.TRUE.equals(body.getSuccess())) {
						Object resp = body.getResponse();
						if (resp instanceof List<?> lista) {
							documentos = lista;
							totalDocs = body.getTotalCount() != null ? body.getTotalCount() : (long) lista.size();
						} else if (resp != null) {
							responseMap.put("documentosRaw", resp);
						}
					} else {
						log.warn("DocumentoService devolvió success=false: {}", body.getMessage());
					}
				}
			} catch (Exception ex) {
				log.warn("Fallo al obtener documentos con base64 para empresa {}: {}", idEmpresa, ex.getMessage());
			}

			responseMap.put("documentos", documentos);
			responseMap.put("totalDocs", totalDocs);

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(responseMap).build());

		} catch (Exception e) {
			log.error("Error al buscar empresa por id de usuario: {}", idUsuario, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todas las empresas");
		try {
			var list = empresaRepository.findAll();
			var dtoList = empresaMapper.listEntityToResumenDtoList(list);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar las empresas", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar empresa por id: {}", id);
		try {
			if (!empresaRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			empresaRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar empresa con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}
}
