package com.aqua.plus.api.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.postgresql.util.PGobject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IRutaEmpleadoService;
import com.aqua.plus.commons.dtos.AsignacionMasivaRequestDTO;
import com.aqua.plus.commons.dtos.AsignacionMasivaResponseDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.RutaEmpleadoDTO;
import com.aqua.plus.commons.entities.EmpleadoEmpresaEntity;
import com.aqua.plus.commons.entities.EmpresaClienteContadorEntity;
import com.aqua.plus.commons.entities.ParametrosEmpresaEntity;
import com.aqua.plus.commons.entities.RutaEmpleadoEntity;
import com.aqua.plus.commons.maps.RutaEmpleadoMapper;
import com.aqua.plus.commons.repositories.EmpleadoEmpresaRepository;
import com.aqua.plus.commons.repositories.EmpresaClienteContadorRepository;
import com.aqua.plus.commons.repositories.ParametrosEmpresaRepository;
import com.aqua.plus.commons.repositories.RutaEmpleadoRepository;
import com.aqua.plus.commons.utils.Constantes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RutaEmpleadoServiceImpl implements IRutaEmpleadoService {

	private final RutaEmpleadoRepository rutaEmpleadoRepository;
	private final RutaEmpleadoMapper rutaEmpleadoMapper;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final ObjectMapper objectMapper;
	private final DocumentoServiceImpl documentoServiceImpl;
	private final PlantillaServiceImpl plantillaService;
	private final ParametrosEmpresaRepository parametrosEmpresaRepository;
	private final EmpleadoEmpresaRepository empleadoEmpresaRepository;
	private final EmpresaClienteContadorRepository empresaClienteContadorRepository;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(RutaEmpleadoDTO rutaEmpleadoDTO) {
		log.info("Creando Ruta Empleado");
		try {
			RutaEmpleadoEntity entity = rutaEmpleadoMapper.dtoToEntity(rutaEmpleadoDTO);
			entity.setFechaCreacion(new Date());
			entity.setUsuarioCreacion(rutaEmpleadoDTO.getUsuarioCreacion());
			entity.setActivo(true);

			RutaEmpleadoEntity saved = rutaEmpleadoRepository.save(entity);
			RutaEmpleadoDTO savedDTO = rutaEmpleadoMapper.entityToDto(saved);

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.SAVED_SUCCESSFULLY)
					.code(HttpStatus.CREATED.value()).response(savedDTO).build();

			return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
		} catch (Exception e) {
			log.error("Error creando la Ruta Empleado", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> update(RutaEmpleadoDTO rutaEmpleadoDTO) {
		log.info("Actualizando Ruta Empleado");
		try {
			if (rutaEmpleadoDTO.getId() == null || !rutaEmpleadoRepository.existsById(rutaEmpleadoDTO.getId())) {
				throw new IllegalArgumentException(Constantes.RUT_NOT_FOUND);
			}

			RutaEmpleadoEntity entity = rutaEmpleadoRepository.findById(rutaEmpleadoDTO.getId()).orElseThrow();
			rutaEmpleadoMapper.updateEntityFromDto(rutaEmpleadoDTO, entity);
			entity.setFechaModificacion(new Date());
			entity.setUsuarioModificacion(rutaEmpleadoDTO.getUsuarioModificacion());

			RutaEmpleadoEntity updated = rutaEmpleadoRepository.save(entity);
			RutaEmpleadoDTO updatedDTO = rutaEmpleadoMapper.entityToDto(updated);

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.UPDATED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(updatedDTO).build();

			return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
		} catch (Exception e) {
			log.error("Error actualizando la Ruta Empleado", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.UPDATE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
		log.info("Buscar Ruta Empleado por id: {}", id);
		try {
			Optional<RutaEmpleadoEntity> rutaEmpleado = rutaEmpleadoRepository.findById(id);
			if (rutaEmpleado.isPresent()) {
				RutaEmpleadoDTO dto = rutaEmpleadoMapper.entityToDto(rutaEmpleado.get());
				ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
						.code(HttpStatus.OK.value()).response(dto).build();
				return ResponseEntity.ok(responseDTO);
			} else {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
		} catch (Exception e) {
			log.error("Error al buscar  Ruta Empleado por id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todos las Ruta Empleado");
		try {
			var list = rutaEmpleadoRepository.findAll();
			var dtoList = rutaEmpleadoMapper.listEntityToDtoList(list);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar las Ruta Empleado", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar Ruta Empleado por id: {}", id);
		try {
			if (!rutaEmpleadoRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			rutaEmpleadoRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar Ruta Empleado con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	/**
	 * Sincroniza datos de rutas asignadas a un lector usando su ID. Llama un SP en
	 * PostgreSQL que retorna estructura JSON con empresas, clientes y lecturas.
	 * 
	 * @author nicope
	 * @version 1.0
	 */
	@Transactional
	public Map<String, Object> syncLectorData(Integer idPersona, Integer offset, Integer limit) {
		try {
			StringBuilder sql = new StringBuilder("SELECT * FROM public.sync_lector_data(:idPersona");

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("idPersona", idPersona);

			if (offset != null && limit != null) {
				sql.append(", :offset, :limit)");
				parameters.addValue("offset", offset);
				parameters.addValue("limit", limit);
			} else {
				sql.append(")");
			}

			Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql.toString(), parameters);

			Object wrappedValue = rawResult.get("sync_lector_data");

			if (wrappedValue instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {
				String jsonValue = pgObject.getValue();
				return objectMapper.readValue(jsonValue, new TypeReference<Map<String, Object>>() {
				});
			}

			return Map.of(Constantes.ERROR_KEY, Constantes.RESULT_COULD_NOT_PROCESSED);

		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return Collections.singletonMap(Constantes.ERROR_KEY, Constantes.PROCCESSING_ERROR + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.singletonMap(Constantes.ERROR_KEY, Constantes.UNEXPECTED_ERROR + e.getMessage());
		}
	}

	/**
	 * 
	 * @author nicope
	 * @version 1.0
	 */
	@Transactional(readOnly = true)
	public Map<String, Object> syncConfigEnterprise(Integer idPersona, Integer offset, Integer limit) {
		if (idPersona == null) {
			return Collections.singletonMap(Constantes.ERROR_KEY, "idPersona es obligatorio");
		}
		if ((offset != null && offset < 0) || (limit != null && limit < 0)) {
			return Collections.singletonMap(Constantes.ERROR_KEY, "offset y limit deben ser >= 0");
		}

		try {
			StringBuilder sql = new StringBuilder("SELECT public.sync_config_empresa(:idPersona");
			MapSqlParameterSource sqlParams = new MapSqlParameterSource().addValue("idPersona", idPersona);
			if (offset != null && limit != null) {
				sql.append(", :offset, :limit");
				sqlParams.addValue("offset", offset).addValue("limit", limit);
			}
			sql.append(")::text");

			String jsonText = namedParameterJdbcTemplate.queryForObject(sql.toString(), sqlParams, String.class);
			if (jsonText == null || jsonText.isBlank()) {
				return Collections.singletonMap(Constantes.ERROR_KEY, Constantes.RESULT_COULD_NOT_PROCESSED);
			}

			var root = objectMapper.readTree(jsonText);
			if (!(root instanceof com.fasterxml.jackson.databind.node.ObjectNode rootObj)) {
				return objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {
				});
			}

			var responseObj = asObject(rootObj.path("response"));
			if (responseObj == null)
				return objectMapper.convertValue(rootObj, new TypeReference<Map<String, Object>>() {
				});
			var dataObj = asObject(responseObj.path("data"));
			if (dataObj == null)
				return objectMapper.convertValue(rootObj, new TypeReference<Map<String, Object>>() {
				});
			var empresaObj = asObject(dataObj.path("empresa"));
			if (empresaObj == null)
				return objectMapper.convertValue(rootObj, new TypeReference<Map<String, Object>>() {
				});

			Integer empresaId = parseIntSafe(empresaObj.get("id"));
			if (empresaId != null) {
				try {
					Map<String, String> tplParams = new java.util.HashMap<>();
					tplParams.putAll(cargarParamsEmpresaFooter(empresaId));

					tplParams.put(Constantes.PARAMETRO_EMPRESAS_NOMBRE, textOrEmpty(empresaObj, "nombre"));
					tplParams.put(Constantes.PARAMETRO_SOPORTE_TELEFONO, textOrEmpty(empresaObj, "telefonoEmpresa"));
					tplParams.put(Constantes.PARAMETRO_SOPORTE_CORREO, textOrEmpty(empresaObj, "correoEmpresa"));

					String aviso = plantillaService.renderByCodigoWithDefaults(Constantes.COD_FOOTER, tplParams);
					String pie = plantillaService.renderByCodigoWithDefaults(Constantes.COD_AVISO, tplParams);

					empresaObj.put("avisoFactura", aviso != null ? aviso : "");
					empresaObj.put("piePagina", pie != null ? pie : "");

					log.info("avisoFactura y piePagina inyectados para empresaId={} (avisoLen={}, pieLen={})",
							empresaId, (aviso == null ? 0 : aviso.length()), (pie == null ? 0 : pie.length()));

				} catch (Exception ex) {
					log.warn("Fallo al inyectar aviso/pie para empresaId={}: {}", empresaId, ex.getMessage());
					empresaObj.put("avisoFactura", "");
					empresaObj.put("piePagina", "");
				}

				try {
					var logoObj = buildSingleDocNombreImagen(empresaId, Constantes.TYPE_LOGO);
					empresaObj.set("logoEmpresa", logoObj);
					log.info("logoEmpresa inyectado para empresaId={} (nombre?={} imagen?={})", empresaId,
							logoObj.hasNonNull("nombre"), logoObj.hasNonNull("imagen"));
				} catch (Exception ex) {
					log.warn("Fallo al inyectar logoEmpresa para empresaId={}: {}", empresaId, ex.getMessage());
					empresaObj.set("logoEmpresa", objectMapper.createObjectNode());
				}
			} else {
				empresaObj.set("logoEmpresa", objectMapper.createObjectNode());
			}

			if (empresaId != null) {
				try {
					var puntosPagoArr = buildDocsArrayNombreImagen(empresaId, "PUPA");
					empresaObj.set("puntosPago", puntosPagoArr);
					log.info("puntosPago inyectado para empresaId={} (count={})", empresaId, puntosPagoArr.size());
				} catch (Exception ex) {
					log.warn("Fallo al inyectar puntosPago para empresaId={}: {}", empresaId, ex.getMessage());
					empresaObj.set("puntosPago", objectMapper.createArrayNode());
				}
			} else {
				empresaObj.set("puntosPago", objectMapper.createArrayNode());
			}

			try {
				var codigoQrObj = buildCodigoQrNombreImagen(empresaId);
				empresaObj.set("codigoQr", codigoQrObj);
				log.info("codigoQr inyectado para empresaId={} (tieneImagen?={})", empresaId,
						codigoQrObj.hasNonNull("imagen"));
			} catch (Exception ex) {
				log.warn("Fallo al inyectar codigoQr para empresaId={}: {}", empresaId, ex.getMessage());
				empresaObj.set("codigoQr", objectMapper.createObjectNode());
			}

			dataObj.set("empresa", empresaObj);
			responseObj.set("data", dataObj);
			rootObj.set("response", responseObj);

			return objectMapper.convertValue(rootObj, new TypeReference<Map<String, Object>>() {
			});

		} catch (JsonProcessingException e) {
			log.error("Error parseando JSON en syncConfigEnterprise", e);
			return Collections.singletonMap(Constantes.ERROR_KEY, Constantes.PROCCESSING_ERROR + e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado en syncConfigEnterprise", e);
			return Collections.singletonMap(Constantes.ERROR_KEY, Constantes.UNEXPECTED_ERROR + e.getMessage());
		}
	}

	/* ========================= Helpers ========================= */

	private ObjectNode asObject(JsonNode n) {
		return (n instanceof ObjectNode) ? (ObjectNode) n : null;
	}

	private Integer parseIntSafe(JsonNode n) {
		if (n == null || n.isNull())
			return null;
		if (n.isInt())
			return n.intValue();
		if (n.isTextual()) {
			try {
				return Integer.valueOf(n.textValue().trim());
			} catch (Exception ignore) {
			}
		}
		return null;
	}

	private String extraerNombreDeRuta(String ruta) {
		if (ruta == null || ruta.isBlank())
			return "sin_nombre";
		int slash = Math.max(ruta.lastIndexOf('/'), ruta.lastIndexOf('\\'));
		return (slash >= 0 && slash < ruta.length() - 1) ? ruta.substring(slash + 1) : ruta;
	}

	/** LOGO: primer documento de una categoría -> OBJETO { nombre, imagen }. */
	private ObjectNode buildSingleDocNombreImagen(Integer empresaId, String categoriaCodigo) {
		ResponseEntity<ResponseDTO> resp = documentoServiceImpl.listarPorEmpresaYCategoriaCodigo(empresaId,
				categoriaCodigo);

		var empty = objectMapper.createObjectNode();

		if (resp == null || !resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null
				|| !Boolean.TRUE.equals(resp.getBody().getSuccess())) {
			return empty;
		}

		Object payload = resp.getBody().getResponse();
		if (!(payload instanceof List<?> lista) || lista.isEmpty())
			return empty;

		Object first = lista.get(0);

		var node = objectMapper.valueToTree(first);

		String b64 = null;
		if (node.hasNonNull("imagen"))
			b64 = node.get("imagen").asText();
		else if (node.hasNonNull("imagenBase64"))
			b64 = node.get("imagenBase64").asText();

		String nombre = null;
		if (node.hasNonNull("nombre"))
			nombre = node.get("nombre").asText();
		else if (node.hasNonNull("ruta"))
			nombre = extraerNombreDeRuta(node.get("ruta").asText());

		if (b64 == null || b64.isBlank())
			return empty;
		if (nombre == null || nombre.isBlank())
			nombre = "documento";

		var obj = objectMapper.createObjectNode();
		obj.put("nombre", nombre);
		obj.put("imagen", b64);
		return obj;
	}

	/**
	 * PUNTOS DE PAGO: TODOS los documentos de una categoría -> ARRAY [{ nombre,
	 * imagen }, ...].
	 */
	private ArrayNode buildDocsArrayNombreImagen(Integer empresaId, String categoriaCodigo) {
		ResponseEntity<ResponseDTO> resp = documentoServiceImpl.listarPorEmpresaYCategoriaCodigo(empresaId,
				categoriaCodigo);

		var out = objectMapper.createArrayNode();

		if (resp == null || !resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null
				|| !Boolean.TRUE.equals(resp.getBody().getSuccess())) {
			return out;
		}

		Object payload = resp.getBody().getResponse();
		if (!(payload instanceof java.util.List<?> lista) || lista.isEmpty())
			return out;

		for (Object item : lista) {
			var node = objectMapper.valueToTree(item);

			String b64 = null;
			if (node.hasNonNull("imagen"))
				b64 = node.get("imagen").asText();
			else if (node.hasNonNull("imagenBase64"))
				b64 = node.get("imagenBase64").asText();

			String nombre = null;
			if (node.hasNonNull("nombre"))
				nombre = node.get("nombre").asText();
			else if (node.hasNonNull("ruta"))
				nombre = extraerNombreDeRuta(node.get("ruta").asText());

			if (b64 != null && !b64.isBlank()) {
				if (nombre == null || nombre.isBlank())
					nombre = "documento";
				var obj = objectMapper.createObjectNode();
				obj.put("nombre", nombre);
				obj.put("imagen", b64);
				out.add(obj);
			}
		}
		return out;
	}

	/**
	 * Toma la primera entrada de ResponseDTO y la convierte a {nombre, imagen};
	 * null si no hay.
	 */
	private ObjectNode toFirstNombreImagen(ResponseEntity<ResponseDTO> resp) {
		if (resp == null || !resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null
				|| !Boolean.TRUE.equals(resp.getBody().getSuccess())) {
			return null;
		}
		Object payload = resp.getBody().getResponse();
		if (!(payload instanceof java.util.List<?> lista) || lista.isEmpty())
			return null;

		var node = objectMapper.valueToTree(lista.get(0));
		String b64 = null;
		if (node.hasNonNull("imagen"))
			b64 = node.get("imagen").asText();
		else if (node.hasNonNull("imagenBase64"))
			b64 = node.get("imagenBase64").asText();

		String nombre = null;
		if (node.hasNonNull("nombre"))
			nombre = node.get("nombre").asText();
		else if (node.hasNonNull("ruta"))
			nombre = extraerNombreDeRuta(node.get("ruta").asText());

		if (b64 == null || b64.isBlank())
			return null;
		if (nombre == null || nombre.isBlank())
			nombre = "documento";

		var obj = objectMapper.createObjectNode();
		obj.put("nombre", nombre);
		obj.put("imagen", b64);
		return obj;
	}

	/**
	 * CODIGO QR: objeto { nombre, imagen } con fallback: (empresa + "QR") -> ("QR"
	 * global).
	 */
	private ObjectNode buildCodigoQrNombreImagen(Integer empresaId) {
		final String CATEGORIA_QR = "QR";
		var empty = objectMapper.createObjectNode();

		if (empresaId != null) {
			ResponseEntity<ResponseDTO> respEmp = documentoServiceImpl.listarPorEmpresaYCategoriaCodigo(empresaId,
					CATEGORIA_QR);
			ObjectNode objEmp = toFirstNombreImagen(respEmp);
			if (objEmp != null)
				return objEmp;
		}

		ResponseEntity<ResponseDTO> respGlobal = documentoServiceImpl.listarPorEmpresaYCategoriaCodigo(null,
				CATEGORIA_QR);
		ObjectNode objGlobal = toFirstNombreImagen(respGlobal);
		return (objGlobal != null) ? objGlobal : empty;
	}

	private Map<String, String> cargarParamsEmpresaFooter(Integer empresaId) {
		final Set<String> TARGET_KEYS = Set.of(Constantes.PARAMETRO_AVISO_TITULO, Constantes.PARAMETRO_AVISO_TEXTO,
				Constantes.PARAMETRO_SITIO_WEB);

		Map<String, String> out = new HashMap<>();

		for (ParametrosEmpresaEntity pe : parametrosEmpresaRepository.findGlobalDefaultsActivos()) {
			if (pe.getLlave() == null)
				continue;
			String k = pe.getLlave().trim().toUpperCase(Locale.ROOT);
			if (TARGET_KEYS.contains(k)) {
				out.put(k, pe.getValorParametro() == null ? "" : pe.getValorParametro());
			}
		}

		if (empresaId != null) {
			for (ParametrosEmpresaEntity pe : parametrosEmpresaRepository.findByEmpresa_IdAndActivoTrue(empresaId)) {
				if (pe.getLlave() == null)
					continue;
				String k = pe.getLlave().trim().toUpperCase(Locale.ROOT);
				if (TARGET_KEYS.contains(k)) {
					out.put(k, pe.getValorParametro() == null ? "" : pe.getValorParametro());
				}
			}
		}

		return out;
	}

	private static String textOrEmpty(JsonNode obj, String... keys) {
		for (String k : keys) {
			var n = obj.get(k);
			if (n != null && !n.isNull() && n.isTextual() && !n.asText().isBlank()) {
				return n.asText();
			}
		}
		return "";
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> asignarClientesMasivo(AsignacionMasivaRequestDTO request) {
		log.info("Asignación masiva de clientes - empleado: {}, total clientes: {}", request.getIdEmpleadoEmpresa(),
				request.getIdsEmpresaClienteContador().size());
		try {
			EmpleadoEmpresaEntity empleado = empleadoEmpresaRepository
					.findByIdAndActivoTrue(request.getIdEmpleadoEmpresa()).orElse(null);

			if (empleado == null) {
				ResponseDTO notFound = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFound);
			}

			List<EmpresaClienteContadorEntity> clientes = empresaClienteContadorRepository
					.findAllByIdInAndActivoTrue(request.getIdsEmpresaClienteContador());

			if (clientes.isEmpty()) {
				ResponseDTO notFound = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFound);
			}

			Set<Integer> idsValidos = clientes.stream().map(EmpresaClienteContadorEntity::getId)
					.collect(Collectors.toSet());

			List<Integer> idsOmitidos = new ArrayList<>();
			List<String> errores = new ArrayList<>();

			request.getIdsEmpresaClienteContador().stream().filter(id -> !idsValidos.contains(id)).forEach(id -> {
				idsOmitidos.add(id);
				errores.add("Cliente no encontrado o inactivo: " + id);
			});

			int totalReemplazados = 0;
			if (Boolean.TRUE.equals(request.getReemplazarAsignacionPrevia())) {
				totalReemplazados = rutaEmpleadoRepository
						.desactivarPorEmpresaClienteContadorIds(request.getIdsEmpresaClienteContador());
				log.info("Asignaciones previas desactivadas: {}", totalReemplazados);
			}

			List<RutaEmpleadoEntity> nuevasAsignaciones = new ArrayList<>();

			for (EmpresaClienteContadorEntity cliente : clientes) {
				boolean yaAsignado = rutaEmpleadoRepository
						.existsByEmpresaClienteContador_IdAndEmpleadoEmpresa_IdAndActivoTrue(cliente.getId(),
								empleado.getId());

				if (yaAsignado) {
					log.debug("Cliente {} ya asignado al empleado {}, se omite", cliente.getId(), empleado.getId());
					idsOmitidos.add(cliente.getId());
					errores.add("Cliente " + cliente.getId() + " ya está asignado a este empleado");
					continue;
				}

				RutaEmpleadoEntity ruta = new RutaEmpleadoEntity();
				ruta.setEmpresaClienteContador(cliente);
				ruta.setEmpleadoEmpresa(empleado);
				ruta.setLectura(null);
				ruta.setActivo(true);

				nuevasAsignaciones.add(ruta);
			}

			List<RutaEmpleadoDTO> asignados = new ArrayList<>();
			if (!nuevasAsignaciones.isEmpty()) {
				List<RutaEmpleadoEntity> guardadas = rutaEmpleadoRepository.saveAll(nuevasAsignaciones);
				asignados = guardadas.stream().map(rutaEmpleadoMapper::entityToDto).toList();
				log.info("Total asignaciones guardadas: {}", asignados.size());
			}

			AsignacionMasivaResponseDTO responseData = AsignacionMasivaResponseDTO.builder()
					.totalRecibidos(request.getIdsEmpresaClienteContador().size()).totalAsignados(asignados.size())
					.totalOmitidos(idsOmitidos.size()).totalReemplazados(totalReemplazados).asignados(asignados)
					.idsOmitidos(idsOmitidos).errores(errores).build();

			ResponseDTO ok = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(responseData).build();
			return ResponseEntity.ok(ok);

		} catch (Exception e) {
			log.error("Error en asignación masiva de clientes - empleado: {}", request.getIdEmpleadoEmpresa(), e);
			ResponseDTO err = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
		}
	}

}
