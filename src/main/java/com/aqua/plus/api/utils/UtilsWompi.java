package com.aqua.plus.api.utils;

import com.aqua.plus.commons.entities.EmpresaWompiEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class UtilsWompi {

    @Value("${wompi.url-base}")
    private String urlBase;

    public HttpHeaders getHeaderPublico() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public HttpHeaders getHeaderMerchantPara(String clavePublicaParam) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + clavePublicaParam);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }


    public HttpHeaders getHeaderPrivadoPara(String clavePrivadaParam) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + clavePrivadaParam);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Valida que la configuración Wompi sea usable (activa y con campos obligatorios presentes).
     * Las credenciales sensibles llegan ya descifradas desde la capa de servicio.
     */
    public WompiCredenciales resolverCredenciales(EmpresaWompiEntity config) {

        if (config == null
                || !Boolean.TRUE.equals(config.getActivo())
                || !tieneValor(config.getWompiClavePublica())
                || !tieneValor(config.getWompiClavePrivada())
                || !tieneValor(config.getWompiSecretoIntegridad())) {

            throw new RuntimeException("La empresa no tiene configuradas las credenciales de Wompi");
        }

        return new WompiCredenciales(
                config.getWompiClavePublica(),
                config.getWompiClavePrivada(),
                config.getWompiSecretoIntegridad(),
                config.getWompiSecretoEventos());
    }

    private boolean tieneValor(String s) {
        return s != null && !s.isBlank();
    }

    public String getUrlBase() {
        return urlBase;
    }
}
