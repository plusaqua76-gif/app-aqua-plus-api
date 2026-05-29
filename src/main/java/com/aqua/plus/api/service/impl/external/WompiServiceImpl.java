package com.aqua.plus.api.service.impl.external;

import com.aqua.plus.api.config.RestTemplateConfig;
import com.aqua.plus.api.service.external.IWompiService;
import com.aqua.plus.api.utils.UtilsWompi;
import com.aqua.plus.commons.dtos.external.TokenizarTarjetaRequest;
import com.aqua.plus.commons.dtos.external.TokenizarTarjetaResponse;
import com.aqua.plus.commons.dtos.external.WompiTransaccionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WompiServiceImpl implements IWompiService {

    @Value("${wompi.end-point.merchants}")
    private String endPointMerchants;

    @Value("${wompi.end-point.transactions}")
    private String endPointTransactions;

    @Value("${wompi.end-point.pse-institutions}")
    private String endPointPseInstitutions;

    @Value("${wompi.end-point.tokens-cards}")
    private String endPointTokensCards;

    private final RestTemplateConfig restTemplateConfig;
    private final UtilsWompi utilsWompi;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMerchantInfo() {
        throw new IllegalStateException("La consulta de merchant Wompi requiere clave publica de empresa_wompi");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMerchantInfo(String clavePublicaParam) {
        String clavePublica = validarCredencial(clavePublicaParam, "clave publica");
        String url = utilsWompi.getUrlBase()
                     + endPointMerchants
                     + "/" + clavePublica;

        log.info("GET merchant info: {}", url);

        ResponseEntity<Map> response = restTemplateConfig.restTemplate()
            .exchange(url, HttpMethod.GET,
                      new HttpEntity<>(utilsWompi.getHeaderPublico()),
                      Map.class);

        log.info("Merchant info obtenido - status: {}", response.getStatusCode());
        return response.getBody();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String extraerAcceptanceToken(Map<String, Object> merchantInfo) {
        Map<String, Object> data               = (Map<String, Object>) merchantInfo.get("data");
        Map<String, Object> presignedAcceptance = (Map<String, Object>) data.get("presigned_acceptance");
        return (String) presignedAcceptance.get("acceptance_token");
    }


    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> crearTransaccion(WompiTransaccionRequest request) {
        throw new IllegalStateException("Crear transaccion Wompi requiere clave privada de empresa_wompi");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> crearTransaccion(WompiTransaccionRequest request, String clavePrivadaParam) {
        String url = utilsWompi.getUrlBase() + endPointTransactions;

        log.info("POST transaccion Wompi - referencia: {}", request.getReference());

        HttpHeaders headers = utilsWompi.getHeaderPrivadoPara(
                validarCredencial(clavePrivadaParam, "clave privada"));

        HttpEntity<WompiTransaccionRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplateConfig.restTemplate()
            .exchange(url, HttpMethod.POST, entity, Map.class);

        log.info("Transaccion Wompi creada - status HTTP: {}", response.getStatusCode());

        // La respuesta de Wompi envuelve la transacción en "data"
        Map<String, Object> body = response.getBody();
        return (Map<String, Object>) body.get("data");
    }


    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> consultarTransaccion(String idTransaccion) {
        throw new IllegalStateException("Consultar transaccion Wompi requiere clave privada de empresa_wompi");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> consultarTransaccion(String idTransaccion, String clavePrivadaParam) {
        String url = utilsWompi.getUrlBase() + endPointTransactions + "/" + idTransaccion;

        log.info("GET transaccion Wompi - id: {}", idTransaccion);

        HttpHeaders headers = utilsWompi.getHeaderPrivadoPara(
                validarCredencial(clavePrivadaParam, "clave privada"));

        ResponseEntity<Map> response = restTemplateConfig.restTemplate()
            .exchange(url, HttpMethod.GET,
                      new HttpEntity<>(headers),
                      Map.class);

        Map<String, Object> body = response.getBody();
        return (Map<String, Object>) body.get("data");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getInstitucionesFinancieras() {
        throw new IllegalStateException("Consultar bancos PSE requiere clave publica de empresa_wompi");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getInstitucionesFinancieras(String clavePublicaParam) {
        String url = utilsWompi.getUrlBase() + endPointPseInstitutions;

        log.info("GET instituciones financieras PSE: {}", url);

        HttpHeaders headers = utilsWompi.getHeaderMerchantPara(
                validarCredencial(clavePublicaParam, "clave publica"));

        ResponseEntity<Map> response = restTemplateConfig.restTemplate()
            .exchange(url, HttpMethod.GET,
                      new HttpEntity<>(headers),
                      Map.class);

        Map<String, Object> body = response.getBody();
        return (List<Map<String, Object>>) body.get("data");
    }

    @Override
    @SuppressWarnings("unchecked")
    public TokenizarTarjetaResponse tokenizarTarjeta(TokenizarTarjetaRequest request) {
        throw new IllegalStateException("Tokenizar tarjeta Wompi requiere clave publica de empresa_wompi");
    }

    @Override
    @SuppressWarnings("unchecked")
    public TokenizarTarjetaResponse tokenizarTarjeta(TokenizarTarjetaRequest request, String clavePublicaParam) {
        String url = utilsWompi.getUrlBase() + endPointTokensCards;

        // Intencionalmente no se loguea el número de tarjeta ni el CVC (PCI DSS)
        log.info("POST tokenizar tarjeta — titular: {}", request.getCardHolder());

        HttpHeaders headers = utilsWompi.getHeaderMerchantPara(
                validarCredencial(clavePublicaParam, "clave publica"));

        HttpEntity<TokenizarTarjetaRequest> entity =
            new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplateConfig.restTemplate()
            .exchange(url, HttpMethod.POST, entity, Map.class);

        log.info("Tokenización completada — status HTTP: {}", response.getStatusCode());

        Map<String, Object> body = response.getBody();
        Map<String, Object> data = (Map<String, Object>) body.get("data");

        TokenizarTarjetaResponse resp = new TokenizarTarjetaResponse();
        resp.setId((String) data.get("id"));
        resp.setBrand((String) data.get("brand"));
        resp.setName((String) data.get("name"));
        resp.setLastFour((String) data.get("last_four"));
        resp.setExpMonth((String) data.get("exp_month"));
        resp.setExpYear((String) data.get("exp_year"));
        resp.setCardHolder((String) data.get("card_holder"));
        resp.setCreatedAt((String) data.get("created_at"));
        resp.setExpiresAt((String) data.get("expires_at"));
        return resp;
    }

    private String validarCredencial(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("La " + nombre + " Wompi de empresa_wompi es requerida");
        }
        return valor;
    }
}
