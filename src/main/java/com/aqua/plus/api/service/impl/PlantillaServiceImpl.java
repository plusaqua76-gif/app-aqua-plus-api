package com.aqua.plus.api.service.impl;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.commons.entities.PlantillaEntity;
import com.aqua.plus.commons.repositories.PlantillaRepository;

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
public class PlantillaServiceImpl {

	private final PlantillaRepository plantillaRepository;

	/**
	 * Carga la plantilla por código y reemplaza los placeholders $P{CLAVE} con los
	 * valores provistos en 'params'. Si falta un valor, se reemplaza por "".
	 *
	 * @param codigoPlantilla Código único de la plantilla
	 * @param params          Mapa CLAVE -> valor (sin $P{})
	 * @return HTML renderizado (o "" si no existe/está vacía la plantilla)
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
	 * Reemplaza en un template en memoria todos los $P{CLAVE} usando 'params'. Si
	 * un placeholder no tiene valor en 'params', se deja en "".
	 */
	public String renderFromString(String template, Map<String, String> params) {
		if (template == null || template.isBlank())
			return "";
		Map<String, String> values = (params != null) ? params : java.util.Collections.emptyMap();

		java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\$P\\{([^}]+)\\}");
		java.util.regex.Matcher m = p.matcher(template);
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
}
