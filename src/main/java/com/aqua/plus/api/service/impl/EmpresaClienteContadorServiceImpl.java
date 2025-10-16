package com.aqua.plus.api.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.configs.security.utils.JwtUtil;
import com.aqua.plus.api.service.IEmpresaClienteContadorService;
import com.aqua.plus.api.service.impl.specification.ContadorSpecification;
import com.aqua.plus.api.service.impl.specification.PersonaSpecification;
import com.aqua.plus.commons.dtos.EmpresaClienteContadorDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ContadorEntity;
import com.aqua.plus.commons.entities.CorreoGeneralEntity;
import com.aqua.plus.commons.entities.EmpresaClienteContadorEntity;
import com.aqua.plus.commons.entities.RutaEmpleadoEntity;
import com.aqua.plus.commons.entities.TelefonoGeneralEntity;
import com.aqua.plus.commons.maps.EmpresaClienteContadorMapper;
import com.aqua.plus.commons.repositories.ContadorRepository;
import com.aqua.plus.commons.repositories.CorreoGeneralRepository;
import com.aqua.plus.commons.repositories.EmpresaClienteContadorRepository;
import com.aqua.plus.commons.repositories.RutaEmpleadoRepository;
import com.aqua.plus.commons.repositories.TelefonoGeneralRepository;
import com.aqua.plus.commons.utils.Constantes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmpresaClienteContadorServiceImpl implements IEmpresaClienteContadorService {

	@Value("${link.recover}")
	private String linkRecover;

	private final EmpresaClienteContadorRepository empresaClienteContadorRepository;
	private final EmpresaClienteContadorMapper empresaClienteContadorMapper;
	private final ObjectMapper objectMapper;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final NotificacionServiceImpl notificacionServiceImpl;
	private final ContadorRepository contadorRepository;
	private final TelefonoGeneralRepository telefonoGeneralRepository;
	private final CorreoGeneralRepository correoGeneralRepository;
	private RutaEmpleadoRepository rutaEmpleadoRepository;
	private final JwtUtil jwtUtil;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(EmpresaClienteContadorDTO empresaClienteContadorDTO) {
		log.info("Creando Empresa Cliente Contador");
		try {
			boolean existe = empresaClienteContadorRepository.existsByEmpresaIdAndClienteIdAndContadorId(
					empresaClienteContadorDTO.getEmpresa().getId(), empresaClienteContadorDTO.getCliente().getId(),
					empresaClienteContadorDTO.getContador().getId());

			if (existe) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.builder().success(false)
						.message(Constantes.EMCL_EXISTS).code(HttpStatus.CONFLICT.value()).build());
			}
			EmpresaClienteContadorEntity entity = empresaClienteContadorMapper.dtoToEntity(empresaClienteContadorDTO);
			entity.setFechaCreacion(new Date());
			entity.setUsuarioCreacion(empresaClienteContadorDTO.getUsuarioCreacion());
			entity.setActivo(true);

			EmpresaClienteContadorEntity saved = empresaClienteContadorRepository.save(entity);
			EmpresaClienteContadorDTO savedDTO = empresaClienteContadorMapper.entityToDto(saved);

			return ResponseEntity.status(HttpStatus.CREATED)
					.body(ResponseDTO.builder().success(true).message(Constantes.SAVED_SUCCESSFULLY)
							.code(HttpStatus.CREATED.value()).response(savedDTO).build());

		} catch (Exception e) {
			log.error("Error creando la Empresa Cliente Contador", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
					.message(Constantes.SAVE_ERROR).code(HttpStatus.BAD_REQUEST.value()).build());
		}
	}

	@Transactional
	public Map<String, Object> saveClient(Map<String, Object> jsonParams) {
		try {
			String jsonString = objectMapper.writeValueAsString(jsonParams);

			String sql = "SELECT * FROM public.guardar_cliente_completo(CAST(:jsonData AS jsonb))";
			MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("jsonData", jsonString);

			Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);
			Object wrappedValue = rawResult.get("guardar_cliente_completo");

			if (wrappedValue instanceof org.postgresql.util.PGobject pgObject && "jsonb".equals(pgObject.getType())) {
				String jsonValue = pgObject.getValue();
				Map<String, Object> response = objectMapper.readValue(jsonValue,
						new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
						});

				Object statusCode = response.get("statusCode");
				if ("200".equals(String.valueOf(statusCode))) {

					String primerNombre = (String) jsonParams.get("primerNombre");
					String segundoNombre = (String) jsonParams.get("segundoNombre");
					String primerApellido = (String) jsonParams.get("primerApellido");
					String segundoApellido = (String) jsonParams.get("segundoApellido");
					String correo = (String) jsonParams.get("correo");
					String usuario = (String) jsonParams.get("usuario");

					String nombre = String
							.join(" ", java.util.Optional.ofNullable(primerNombre).orElse(""),
									java.util.Optional.ofNullable(segundoNombre).orElse(""),
									java.util.Optional.ofNullable(primerApellido).orElse(""),
									java.util.Optional.ofNullable(segundoApellido).orElse(""))
							.replaceAll("\\s+", " ").trim();

					if (correo != null && !correo.isBlank() && usuario != null && !usuario.isBlank()) {
						String tiempoLegible = notificacionServiceImpl
								.obtenerTiempoVigenciaLegible(Constantes.TIEMPO_VIGENCIA_EXTERNO);

						String token = jwtUtil.generateToken(usuario, Constantes.KEY_TOKEN_EXTERNO,
								Constantes.TIEMPO_VIGENCIA_EXTERNO);

						String encodedToken = java.net.URLEncoder.encode(token,
								java.nio.charset.StandardCharsets.UTF_8);

						String baseRecover = (this.linkRecover == null) ? "" : this.linkRecover;

						String recoverLink;
						if (baseRecover.endsWith("?") || baseRecover.endsWith("&")) {
							recoverLink = baseRecover + encodedToken;
						} else if (baseRecover.contains("?")) {
							recoverLink = baseRecover + "&" + encodedToken;
						} else {
							recoverLink = baseRecover + "?" + encodedToken;
						}

						String recoverLinkMasked = recoverLink.replaceAll("([?&])[^#]*", "$1***");
						log.info(
								"Info data notificacion (saveClient): [nameUser={}, user={}, linkRecover={}, hours={}]",
								nombre, usuario, recoverLinkMasked, tiempoLegible);

						Map<String, Object> data = new HashMap<>();
						data.put(Constantes.PARAMETRO_NAME_USER, nombre);
						data.put(Constantes.PARAMETRO_USER, usuario);
						data.put(Constantes.PARAMETRO_LINK_RECOVER, recoverLink);
						data.put(Constantes.PARAMETRO_HOURS, tiempoLegible);

						try {
							String codigoPlantilla = Constantes.CREATE_PASSWORD;
							notificacionServiceImpl.enviarNotificacion(correo, codigoPlantilla, data);
							response.put("emailSent", true);
							response.put("emailTo", correo);
						} catch (Exception mailEx) {
							log.error("Fallo enviando notificación a {}", correo, mailEx);
							response.put("emailSent", false);
							response.put("emailError", mailEx.getMessage());
						}
					} else if (correo == null || correo.isBlank()) {
						response.put("notice", "El cliente no tiene un correo válido; no se envió notificación.");
					} else {
						response.put("notice",
								"'usuario' es requerido para generar el token de recuperación; no se envió notificación.");
					}
				}

				return response;
			}

			return Map.of("error", "El resultado no pudo ser procesado correctamente.");

		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			log.error("Error de procesamiento JSON", e);
			return Map.of("error", "Error de procesamiento JSON: " + e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado en saveClient", e);
			return Map.of("error", "Error inesperado: " + e.getMessage());
		}
	}

	@Transactional
	public Map<String, Object> updateClient(Map<String, Object> jsonParams) {
		try {

			String jsonString = objectMapper.writeValueAsString(jsonParams);

			String sql = "SELECT * FROM public.actualizar_cliente_basico(CAST(:jsonData AS jsonb))";

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("jsonData", jsonString);

			Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

			Object wrappedValue = rawResult.get("actualizar_cliente_basico");
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
			log.error("Error inesperado en updateClient", e);
			return Map.of("error", "Error inesperado: " + e.getMessage());
		}
	}

	@Transactional
	public Map<String, Object> deleteClient(Integer idPersona) {
		try {
			String sql = "SELECT * FROM public.eliminar_cliente_completo(:idPersona)";

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("idPersona", idPersona);

			Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

			Object wrappedValue = rawResult.get("eliminar_cliente_completo");
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
			log.error("Error inesperado en deleteClient", e);
			return Map.of("error", "Error inesperado: " + e.getMessage());
		}
	}

	@Transactional
	public Map<String, Object> actualizarEstado(Map<String, Object> jsonParams) {
		try {
			String jsonString = objectMapper.writeValueAsString(jsonParams);

			String sql = "SELECT * FROM public.actualizar_estado(CAST(:jsonData AS jsonb))";

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("jsonData", jsonString);

			Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

			Object wrappedValue = rawResult.get("actualizar_estado");
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
	@Transactional
	public ResponseEntity<ResponseDTO> update(EmpresaClienteContadorDTO empresaClienteContadorDTO) {
		log.info("Actualizando Empresa Cliente Contador");
		try {
			if (empresaClienteContadorDTO.getId() == null
					|| !empresaClienteContadorRepository.existsById(empresaClienteContadorDTO.getId())) {
				throw new IllegalArgumentException(Constantes.ECC_NOT_FOUND);
			}
			EmpresaClienteContadorEntity entity = empresaClienteContadorRepository
					.findById(empresaClienteContadorDTO.getId()).orElseThrow();
			empresaClienteContadorMapper.updateEntityFromDto(empresaClienteContadorDTO, entity);
			entity.setFechaModificacion(new Date());
			entity.setUsuarioModificacion(empresaClienteContadorDTO.getUsuarioModificacion());

			EmpresaClienteContadorEntity updated = empresaClienteContadorRepository.save(entity);
			EmpresaClienteContadorDTO updatedDTO = empresaClienteContadorMapper.entityToDto(updated);

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.UPDATED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(updatedDTO).build();

			return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
		} catch (Exception e) {
			log.error("Error actualizando la Empresa Cliente Contador", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.UPDATE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEmpresaId(Integer idEmpresa) {
		log.info("Buscar Empresa Cliente Contador por id de empresa: {}", idEmpresa);
		try {
			var list = empresaClienteContadorRepository.findByEmpresa_Id(idEmpresa);
			var dtoList = empresaClienteContadorMapper.listEntityToDtoList(list);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al consultar por id de empresa: {}", idEmpresa, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	/**
	 * @author nicope
	 * @version 1.0
	 *
	 *          Busca clientes de una empresa con filtros dinámicos y paginación;
	 *          retorna DTO.
	 */
	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findClientesByEmpresaId(Integer idEmpresa, Pageable pageable, String nombre,
			String cedula, String codigo, String departamento, String ciudad, String corregimiento, String telefono,
			String correo, String tipoDocumentoNombre) {

		log.info(
				"Buscar clientes por empresa: {}, filtros: [nombreLike={}, cedula={}, codigo={}, dep={}, ciudad={}, corr={}, tel={}, correo={}, tipoDocNombre={}]",
				idEmpresa, nombre, cedula, codigo, departamento, ciudad, corregimiento, telefono, correo,
				tipoDocumentoNombre);

		try {
			var spec = Specification.allOf(PersonaSpecification.empresaId(idEmpresa),
					PersonaSpecification.clienteNombreLike(nombre), PersonaSpecification.clienteCedulaIgual(cedula),
					PersonaSpecification.clienteCodigoIgual(codigo),
					PersonaSpecification.direccionDepartamentoNombreLike(departamento),
					PersonaSpecification.direccionCiudadNombreLike(ciudad),
					PersonaSpecification.direccionCorregimientoNombreLike(corregimiento),
					PersonaSpecification.clienteTelefonoLike(telefono), PersonaSpecification.clienteCorreoLike(correo),
					PersonaSpecification.clienteTipoDocumentoNombreLike(tipoDocumentoNombre));

			List<EmpresaClienteContadorEntity> all = empresaClienteContadorRepository
					.findAll((root, q, cb) -> (spec == null) ? cb.conjunction() : spec.toPredicate(root, q, cb));

			List<Integer> eccIds = all.stream().map(EmpresaClienteContadorEntity::getId).filter(Objects::nonNull)
					.toList();

			Map<Integer, RutaEmpleadoEntity> rutaPorEcc = Collections.emptyMap();
			if (!eccIds.isEmpty()) {
				List<RutaEmpleadoEntity> rutas = rutaEmpleadoRepository.findActivasByEmpresaClienteContadorIdIn(eccIds);
				rutaPorEcc = rutas.stream().collect(Collectors.toMap(re -> re.getEmpresaClienteContador().getId(),
						re -> re, (a, b) -> (a.getFechaCreacion().after(b.getFechaCreacion()) ? a : b)));
			}

			List<Map<String, Object>> rows = new ArrayList<>(all.size());
			for (var ecc : all) {
				var p = ecc.getCliente();
				Integer personaId = (p != null ? p.getId() : null);

				Integer tipoDocId = null;
				String tipoDocNombreVal = null;
				if (p != null && p.getTipoDocumento() != null) {
					tipoDocId = p.getTipoDocumento().getId();
					tipoDocNombreVal = p.getTipoDocumento().getNombre();
				}

				String nombreCompleto = null;
				if (p != null) {
					StringBuilder full = new StringBuilder();
					if (p.getNombre() != null)
						full.append(p.getNombre()).append(' ');
					if (p.getSegundoNombre() != null)
						full.append(p.getSegundoNombre()).append(' ');
					if (p.getApellido() != null)
						full.append(p.getApellido()).append(' ');
					if (p.getSegundoApellido() != null)
						full.append(p.getSegundoApellido());
					nombreCompleto = full.toString().trim().replaceAll("\\s+", " ");
				}

				Integer direccionId = null, depId = null, ciuId = null, corrId = null;
				String depNombre = null, ciudadNombre = null, corrNombre = null, dirDescripcion = null;
				if (p != null && p.getDireccion() != null) {
					var d = p.getDireccion();
					direccionId = d.getId();
					dirDescripcion = d.getDescripcion();
					depId = (d.getDepartamento() != null) ? d.getDepartamento().getId() : null;
					depNombre = (d.getDepartamento() != null) ? d.getDepartamento().getNombre() : null;
					ciuId = (d.getCiudad() != null) ? d.getCiudad().getId() : null;
					ciudadNombre = (d.getCiudad() != null) ? d.getCiudad().getNombre() : null;
					corrId = (d.getCorregimiento() != null) ? d.getCorregimiento().getId() : null;
					corrNombre = (d.getCorregimiento() != null) ? d.getCorregimiento().getNombre() : null;
				}

				String correoVal = (personaId != null)
						? correoGeneralRepository.findTop1ByPersonaIdAndActivoTrueOrderByIdDesc(personaId)
								.map(CorreoGeneralEntity::getCorreo).orElse(null)
						: null;

				String telVal = (personaId != null)
						? telefonoGeneralRepository.findTop1ByPersonaIdAndActivoTrueOrderByIdDesc(personaId)
								.map(TelefonoGeneralEntity::getNumero).orElse(null)
						: null;

				Integer empleadoEmpresaId = null;
				String empleadoNombreCompleto = null;

				RutaEmpleadoEntity ruta = rutaPorEcc.get(ecc.getId());
				if (ruta != null && ruta.getEmpleadoEmpresa() != null
						&& ruta.getEmpleadoEmpresa().getPersona() != null) {
					var pe = ruta.getEmpleadoEmpresa().getPersona();
					empleadoEmpresaId = ruta.getEmpleadoEmpresa().getId();

					StringBuilder fullEmp = new StringBuilder();
					if (pe.getNombre() != null)
						fullEmp.append(pe.getNombre()).append(' ');
					if (pe.getSegundoNombre() != null)
						fullEmp.append(pe.getSegundoNombre()).append(' ');
					if (pe.getApellido() != null)
						fullEmp.append(pe.getApellido()).append(' ');
					if (pe.getSegundoApellido() != null)
						fullEmp.append(pe.getSegundoApellido());
					empleadoNombreCompleto = fullEmp.toString().trim().replaceAll("\\s+", " ");
				}

				Map<String, Object> row = new LinkedHashMap<>();
				row.put("empresaClienteContadorId", ecc.getId());
				row.put("id", personaId);
				row.put("numeroCedula", p != null ? p.getNumeroCedula() : null);
				row.put("nombreCompleto", nombreCompleto);
				row.put("codigo", p != null ? p.getCodigo() : null);
				row.put("activo", p != null ? p.getActivo() : null);
				row.put("tipoDocumentoId", tipoDocId);
				row.put("tipoDocumentoNombre", tipoDocNombreVal);
				row.put("direccionId", direccionId);
				row.put("direccionDescripcion", dirDescripcion);
				row.put("departamentoNombre", depNombre);
				row.put("departamentoId", depId);
				row.put("ciudadNombre", ciudadNombre);
				row.put("ciudadId", ciuId);
				row.put("corregimientoNombre", corrNombre);
				row.put("corregimientoId", corrId);
				row.put("correo", correoVal);
				row.put("telefono", telVal);
				row.put("empleadoEmpresaId", empleadoEmpresaId);
				row.put("empleadoNombreCompleto", empleadoNombreCompleto);

				rows.add(row);
			}

			Comparator<Map<String, Object>> byActivoDesc = Comparator
					.comparing(m -> Boolean.FALSE.equals(m.get("activo")));
			Comparator<Map<String, Object>> byNombreCompletoAsc = Comparator
					.comparing(m -> ((String) m.getOrDefault("nombreCompleto", "")), String.CASE_INSENSITIVE_ORDER);

			rows.sort(byActivoDesc.thenComparing(byNombreCompletoAsc));

			int pageNumber = (pageable != null ? pageable.getPageNumber() : 0);
			int pageSize = (pageable != null ? pageable.getPageSize() : 20);
			int fromIndex = Math.min(pageNumber * pageSize, rows.size());
			int toIndex = Math.min(fromIndex + pageSize, rows.size());
			List<Map<String, Object>> pageContent = rows.subList(fromIndex, toIndex);

			long totalCount = rows.size();
			int totalPages = (int) Math.ceil(totalCount / (double) pageSize);

			if (pageContent.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron clientes para los filtros dados")
								.code(HttpStatus.NOT_FOUND.value()).response(pageContent).totalCount(totalCount)
								.pageSize(pageSize).currentPage(pageNumber).totalPages(totalPages).build());
			}

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message("Consulta exitosa")
					.code(HttpStatus.OK.value()).response(pageContent).totalCount(totalCount).pageSize(pageSize)
					.currentPage(pageNumber).totalPages(totalPages).build());

		} catch (Exception e) {
			log.error("Error al consultar clientes por id de empresa: {}", idEmpresa, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message("Error consultando").code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findContadoresByEmpresaId(Integer idEmpresa, Pageable pageable, String serial,
			String tipoContadorNombre, String direccionDescripcion, String nombreLike, String cedula) {

		log.info(
				"Buscar contadores por empresa: {}, filtros: [serial={}, tipoContador={}, dir={}, nombreLike={}, cedula={}]",
				idEmpresa, serial, tipoContadorNombre, direccionDescripcion, nombreLike, cedula);

		try {
			var spec = Specification.allOf(ContadorSpecification.belongsToEmpresa(idEmpresa),
					ContadorSpecification.isActivoTrue(), ContadorSpecification.serialLike(serial),
					ContadorSpecification.tipoContadorNombreLike(tipoContadorNombre),
					ContadorSpecification.direccionDescripcionLike(direccionDescripcion),
					ContadorSpecification.personaNombreLike(nombreLike),
					ContadorSpecification.personaCedulaEquals(cedula));

			Pageable pageToUse = (pageable != null ? pageable : Pageable.unpaged());
			Page<ContadorEntity> page = contadorRepository.findAll(spec, pageToUse);

			long totalCount = page.getTotalElements();
			int pageSize = page.getSize();
			int currentPage = page.getNumber();
			int totalPages = page.getTotalPages();

			if (page.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron contadores para los filtros dados")
								.code(HttpStatus.NOT_FOUND.value()).response(List.of()).totalCount(totalCount)
								.pageSize(pageSize).currentPage(currentPage).totalPages(totalPages).build());
			}

			List<Map<String, Object>> rows = new ArrayList<>(page.getNumberOfElements());

			for (ContadorEntity c : page.getContent()) {
				Map<String, Object> item = new LinkedHashMap<>();

				item.put("id", c.getId());
				item.put("serial", c.getSerial());
				item.put("activo", c.getActivo());

				Map<String, Object> tipoCont = new LinkedHashMap<>();
				if (c.getTipoContador() != null) {
					tipoCont.put("id", c.getTipoContador().getId());
					tipoCont.put("nombre", c.getTipoContador().getNombre());
				} else {
					tipoCont.put("id", null);
					tipoCont.put("nombre", null);
				}
				item.put("tipoContador", tipoCont);

				Map<String, Object> dir = new LinkedHashMap<>();
				if (c.getDescripcion() != null) {
					var d = c.getDescripcion();
					dir.put("id", d.getId());

					Map<String, Object> dep = new LinkedHashMap<>();
					if (d.getDepartamento() != null) {
						dep.put("id", d.getDepartamento().getId());
						dep.put("nombre", d.getDepartamento().getNombre());
					} else {
						dep.put("id", null);
						dep.put("nombre", null);
					}
					dir.put("departamento", dep);

					Map<String, Object> city = new LinkedHashMap<>();
					if (d.getCiudad() != null) {
						city.put("id", d.getCiudad().getId());
						city.put("nombre", d.getCiudad().getNombre());
					} else {
						city.put("id", null);
						city.put("nombre", null);
					}
					dir.put("ciudad", city);

					Map<String, Object> corr = new LinkedHashMap<>();
					if (d.getCorregimiento() != null) {
						corr.put("id", d.getCorregimiento().getId());
						corr.put("nombre", d.getCorregimiento().getNombre());
					} else {
						corr.put("id", null);
						corr.put("nombre", null);
					}
					dir.put("corregimiento", corr);

					dir.put("descripcion", d.getDescripcion());
				} else {
					dir.put("id", null);
					dir.put("departamento", Map.of("id", null, "nombre", null));
					dir.put("ciudad", Map.of("id", null, "nombre", null));
					dir.put("corregimiento", Map.of("id", null, "nombre", null));
					dir.put("descripcion", null);
				}
				item.put("descripcion", dir);

				String clienteNombreCompleto = null;
				String clienteCedula = null;
				var p = c.getCliente();
				if (p != null) {
					StringBuilder full = new StringBuilder();
					if (p.getNombre() != null)
						full.append(p.getNombre()).append(' ');
					if (p.getSegundoNombre() != null)
						full.append(p.getSegundoNombre()).append(' ');
					if (p.getApellido() != null)
						full.append(p.getApellido()).append(' ');
					if (p.getSegundoApellido() != null)
						full.append(p.getSegundoApellido());
					clienteNombreCompleto = full.toString().trim().replaceAll("\\s+", " ");
					clienteCedula = p.getNumeroCedula();
				}
				item.put("clienteNombreCompleto", clienteNombreCompleto);
				item.put("clienteCedula", clienteCedula);

				rows.add(item);
			}

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(rows).totalCount(totalCount).pageSize(pageSize)
					.currentPage(currentPage).totalPages(totalPages).build());

		} catch (Exception e) {
			log.error("Error al consultar contadores por id de empresa: {}", idEmpresa, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message(Constantes.CONSULTING_ERROR).code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEmpresaIdResponseId(Integer idEmpresa) {
		log.info("Buscar Empresa Cliente Contador por id de empresa: {}", idEmpresa);
		try {
			var list = empresaClienteContadorRepository.findByEmpresa_Id(idEmpresa);

			if (list.isEmpty()) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false)
						.message("No se encontraron registros para la empresa con id: " + idEmpresa)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}

			List<Map<String, Object>> idList = list.stream().map(e -> Map.<String, Object>of("id", e.getId())).toList();

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(idList).build();

			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al consultar por id de empresa: {}", idEmpresa, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
		log.info("Buscar Empresa Cliente Contador por id: {}", id);
		try {
			Optional<EmpresaClienteContadorEntity> empresaClienteContador = empresaClienteContadorRepository
					.findById(id);
			if (empresaClienteContador.isPresent()) {
				EmpresaClienteContadorDTO dto = empresaClienteContadorMapper.entityToDto(empresaClienteContador.get());
				ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
						.code(HttpStatus.OK.value()).response(dto).build();
				return ResponseEntity.ok(responseDTO);
			} else {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
		} catch (Exception e) {
			log.error("Error al buscar  Empresa Cliente Contador por id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todos las Empresa Cliente Contador");
		try {
			var list = empresaClienteContadorRepository.findAll();
			var dtoList = empresaClienteContadorMapper.listEntityToDtoList(list);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar las Empresa Cliente Contador", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar Empresa Cliente Contador por id: {}", id);
		try {
			if (!empresaClienteContadorRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			empresaClienteContadorRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar Empresa Cliente Contador con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Transactional(readOnly = true)
	public Map<String, Object> clientesEmpresaMes(Integer idEmpresa, Integer anio, Integer mes, String rangoPor,
			Boolean exclusivo) {
		try {
			StringBuilder sql = new StringBuilder("SELECT public.fn_clientes_empresa_mes(:idEmpresa, :anio, :mes");

			MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idEmpresa", idEmpresa)
					.addValue("anio", anio).addValue("mes", mes);

			if (rangoPor != null && exclusivo != null) {
				sql.append(", :rangoPor, :exclusivo)");
				parameters.addValue("rangoPor", rangoPor);
				parameters.addValue("exclusivo", exclusivo);
			} else if (rangoPor != null) {
				sql.append(", :rangoPor)");
				parameters.addValue("rangoPor", rangoPor);
			} else if (exclusivo != null) {
				sql.append(", :rangoPor, :exclusivo)");
				parameters.addValue("rangoPor", "emision");
				parameters.addValue("exclusivo", exclusivo);
			} else {
				sql.append(")");
			}

			Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql.toString(), parameters);

			Object wrappedValue = rawResult.get("fn_clientes_empresa_mes");

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
			return Collections.singletonMap(Constantes.ERROR_KEY, Constantes.PROCCESSING_ERROR + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.singletonMap(Constantes.ERROR_KEY, Constantes.UNEXPECTED_ERROR + e.getMessage());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEmpresaAndPersona(Integer idEmpresa, Integer idPersona) {
		log.info("Buscar EmpresaClienteContador por empresa={} y persona={}", idEmpresa, idPersona);
		try {
			if (idEmpresa == null || idPersona == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
						.message("idEmpresa e idPersona son requeridos").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			List<EmpresaClienteContadorEntity> entities = empresaClienteContadorRepository
					.findByEmpresaIdAndClienteId(idEmpresa, idPersona);

			if (entities.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron registros para la empresa/persona indicadas")
								.code(HttpStatus.NOT_FOUND.value()).response(List.of()).build());
			}

			List<EmpresaClienteContadorDTO> dtos = entities.stream().map(empresaClienteContadorMapper::entityToDto)
					.toList();

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message("Consulta exitosa")
					.code(HttpStatus.OK.value()).response(dtos).build());

		} catch (Exception e) {
			log.error("Error consultando EmpresaClienteContador por empresa/persona: {}, {}", idEmpresa, idPersona, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message("Error consultando").code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

}
