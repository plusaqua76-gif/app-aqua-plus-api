package com.aqua.plus.api.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ParametrosSistemaEntity;
import com.aqua.plus.commons.entities.PlantillaEntity;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import com.aqua.plus.commons.repositories.ParametrosSistemaRepository;
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
public class NotificacionServiceImpl {

	private final PlantillaRepository plantillaRepository;
	private final EmailServiceImpl emailServiceImpl;
	private final ParametrosSistemaRepository parametrosSistemaRepository;
	
	
	
    
    public ResponseEntity<ResponseDTO> enviarNotificacion(String correo,String codigo, Map<String, Object> valores) {
    	ResponseDTO response = ResponseDTO.builder().build();
    	  PlantillaEntity plantilla = plantillaRepository.findByCodigo(codigo)
    	            .orElseThrow(() -> new ProcessGenericException("Plantilla no encontrada: " + codigo));
    	this.emailServiceImpl.sendEmail(correo, fillValuesPlantilla(plantilla.getAsunto(), valores), fillValuesPlantilla(plantilla.getContenido(), valores));
    	
    	return new ResponseEntity<>(response,HttpStatus.OK);
    }
    
    private String formatStringPlaceholder(String valor) {
		return "$P_{" + valor + "}";
	}
	
	private Map<String, Object> tokenizePlantilla(String textoPlantilla){
		log.info("Inicio metodo tokenizePlantilla : {} ", textoPlantilla);
		Map<String,Object> placeholders = new HashMap<>();
		String expr = "\\$P_\\{[a-zA-Z]+\\}";
		Pattern pattern = Pattern.compile(expr);
		Matcher matcher = pattern.matcher(textoPlantilla);
		while (matcher.find()) {
			placeholders.put(matcher.group(0), matcher.group(0));
		}
		log.info("Fin metodo tokenizePlantilla");
		return placeholders;
	}
	
	public String fillValuesPlantilla(String textPlantilla, Map<String,Object> userValues) {
		log.info("Inicio metodo fillValuesPlantilla : {},{} ", textPlantilla, userValues);
		Map<String,Object> placeholdersPlantilla = this.tokenizePlantilla(textPlantilla);
		String finalText = textPlantilla;
		for(String item: userValues.keySet()) {
			if(placeholdersPlantilla.get(this.formatStringPlaceholder(item))!=null) {
				finalText=finalText.replace(this.formatStringPlaceholder(item), userValues.get(item).toString());
			}
		}
		log.info("Fin metodo fillValuesPlantilla");
		return finalText;
	}
	
	/**
	 * Obtiene el valor de vigencia en segundos desde la base de datos y lo convierte en una cadena legible (ej: "10 horas").
	 *
	 * @param key Clave del parámetro en BD (ej: "TIEMPO_VIGENCIA_EXTERNO")
	 * @return Cadena con tiempo legible, ej. "1 hora", "2 horas"
	 * @author nicope
	 * @version 1.0
	 */
	public String obtenerTiempoVigenciaLegible(String key) {
	    try {
	        ParametrosSistemaEntity param = parametrosSistemaRepository.findByLlave(key)
	                .orElseThrow(() -> new ProcessGenericException("Parámetro no encontrado: " + key));

	        int segundos = Integer.parseInt(param.getValorParametro());

	        int horas = segundos / 3600;
	        int minutos = (segundos % 3600) / 60;

	        if (horas > 0) {
	            return horas + (horas == 1 ? " hora" : " horas");
	        } else if (minutos > 0) {
	            return minutos + (minutos == 1 ? " minuto" : " minutos");
	        } else {
	            return "menos de 1 minuto";
	        }
	    } catch (Exception e) {
	        log.error("Error al obtener y convertir el tiempo de vigencia", e);
	        return "N/A";
	    }
	}
}
