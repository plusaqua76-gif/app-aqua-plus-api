package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IPlantillaService;
import com.aqua.plus.commons.dtos.PlantillaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.PlantillaEntity;
import com.aqua.plus.commons.maps.PlantillaMapper;
import com.aqua.plus.commons.repositories.PlantillaRepository;
import com.aqua.plus.commons.utils.Constantes;

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
public class PlantillaServiceImpl implements IPlantillaService {

	private final PlantillaRepository plantillaRepository;
	private final PlantillaMapper plantillaMapper;

	/**
	 * Carga la plantilla por código y reemplaza los placeholders $P{CLAVE} con los
	 * valores provistos en 'params'. Si falta un valor, se reemplaza por "".
	 *
	 * @param codigoPlantilla Código único de la plantilla
	 * @param params          Mapa CLAVE -> valor (sin $P{})
	 * @return texto renderizado (o "" si no existe/está vacía la plantilla)
	 */
	@Transactional(readOnly = true)
	public String renderByCodigo(String codigoPlantilla, Map<String, String> params) {
		if (codigoPlantilla == null || codigoPlantilla.isBlank()) {
			log.warn("renderByCodigo: codigoPlantilla vacío");
			return "";
		}
		String template = plantillaRepository.findByCodigo(codigoPlantilla).map(PlantillaEntity::getContenido)
				.orElse("");
		return renderFromString(template, params);
	}

	/**
	 * Mismo que {@link #renderByCodigo}, pero aplica defaults mínimos: -
	 * AVISOTITULO: "IMPORTANTE"
	 *
	 * NOTA: NO pone valores por defecto para FECHAGENERACION ni DERECHOSANIO; si no
	 * vienen en 'params', quedarán vacíos para que los setee el dispositivo móvil.
	 */
	@Transactional(readOnly = true)
	public String renderByCodigoWithDefaults(String codigoPlantilla, Map<String, String> params) {
		Map<String, String> defaults = new HashMap<>();
		defaults.put("AVISOTITULO", "IMPORTANTE");

		Map<String, String> merged = new HashMap<>(defaults);
		if (params != null) {
			params.forEach(
					(k, v) -> merged.put(k == null ? "" : k.trim().toUpperCase(Locale.ROOT), v == null ? "" : v));
		}

		return renderByCodigo(codigoPlantilla, merged);
	}

	/**
	 * Reemplaza en un template en memoria todos los $P{CLAVE} usando 'params'. Si
	 * un placeholder no tiene valor en 'params', se deja en "".
	 */
	public String renderFromString(String template, Map<String, String> params) {
		if (template == null || template.isBlank())
			return "";
		Map<String, String> values = (params != null) ? params : java.util.Collections.emptyMap();

		Pattern p = Pattern.compile("\\$P\\{([^}]+)\\}");
		Matcher m = p.matcher(template);
		StringBuffer sb = new StringBuffer();

		while (m.find()) {
			String rawKey = m.group(1);
			String key = (rawKey == null) ? "" : rawKey.trim().toUpperCase(java.util.Locale.ROOT);
			String val = values.getOrDefault(key, "");
			val = val.replace("\\", "\\\\").replace("$", "\\$");
			m.appendReplacement(sb, val);
		}
		m.appendTail(sb);
		return sb.toString();
	}

	/** ************************************************ **/

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(PlantillaDTO plantillaDTO) {
		log.info("Guardar/Actualizar plantilla");

		try {

			if (plantillaDTO == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("Objeto Plantilla es requerido").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			boolean isUpdate = plantillaDTO.getId() != null;

			if (!isUpdate) {
				if (plantillaDTO.getEmpresa() == null || plantillaDTO.getEmpresa().getId() == null) {
					return ResponseEntity.badRequest()
							.body(ResponseDTO.builder().success(false)
									.message("Debe indicar la empresa de la plantilla")
									.code(HttpStatus.BAD_REQUEST.value()).build());
				}

				if (plantillaDTO.getContenido() == null || plantillaDTO.getContenido().isBlank()) {
					return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
							.message("El contenido es obligatorio").code(HttpStatus.BAD_REQUEST.value()).build());
				}

				if (plantillaDTO.getCodigo() == null || plantillaDTO.getCodigo().isBlank()) {
					return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
							.message("El código es obligatorio").code(HttpStatus.BAD_REQUEST.value()).build());
				}
			}

			PlantillaEntity entity;

			if (isUpdate) {
				entity = plantillaRepository.findById(plantillaDTO.getId())
						.orElseThrow(() -> new RuntimeException("No existe plantilla con el ID indicado"));

				plantillaMapper.updateEntityFromDto(plantillaDTO, entity);

				entity.setFechaModificacion(new Date());
				entity.setUsuarioModificacion(plantillaDTO.getUsuarioModificacion());

			} else {
				entity = plantillaMapper.dtoToEntity(plantillaDTO);
				entity.setFechaCreacion(new Date());
				entity.setUsuarioCreacion(plantillaDTO.getUsuarioCreacion());
				entity.setActivo(true);
			}

			PlantillaEntity saved = plantillaRepository.save(entity);
			PlantillaDTO savedDTO = plantillaMapper.entityToDto(saved);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int status = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			return ResponseEntity.status(status)
					.body(ResponseDTO.builder().success(true).message(message).code(status).response(savedDTO).build());

		} catch (Exception e) {

			Throwable root = e;
			while (root.getCause() != null && root.getCause() != root) {
				root = root.getCause();
			}

			Map<String, Object> errorInfo = new LinkedHashMap<>();
			errorInfo.put("exception", e.getClass().getName());
			errorInfo.put("message", e.getMessage());
			errorInfo.put("rootCause", root.getMessage());

			log.error("Error guardando plantilla. rootCause={}", root.getMessage(), e);

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false)
							.message("Error guardando plantilla: "
									+ (root.getMessage() != null ? root.getMessage() : "ver detalle en 'response'"))
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(errorInfo).build());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEmpresa(Integer idEmpresa) {
		log.info("Buscar todas las Plantillas por empresaId={}", idEmpresa);

		try {
			if (idEmpresa == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("Parámetro requerido: idEmpresa").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			List<PlantillaEntity> entities = plantillaRepository.findByEmpresa_Id(idEmpresa);

			if (entities == null || entities.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron plantillas para la empresa indicada")
								.code(HttpStatus.NOT_FOUND.value()).totalCount(0L).response(List.of()).build());
			}

			// === Mapear ===
			List<PlantillaDTO> dtos = entities.stream().map(plantillaMapper::entityToDto).toList();

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtos).totalCount((long) dtos.size()).build());

		} catch (Exception e) {
			log.error("Error al buscar Plantillas por empresaId={}", idEmpresa, e);

			// Obtener root cause real
			Throwable root = e;
			while (root.getCause() != null && root.getCause() != root) {
				root = root.getCause();
			}

			Map<String, Object> errorInfo = new LinkedHashMap<>();
			errorInfo.put("exception", e.getClass().getName());
			errorInfo.put("message", e.getMessage());
			errorInfo.put("rootCause", root.getMessage());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false)
							.message("Error al consultar plantillas: "
									+ (root.getMessage() != null ? root.getMessage() : "ver detalle en 'response'"))
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(errorInfo).build());
		}
	}

}
