package com.aqua.plus.api.service.external;
import com.aqua.plus.commons.dtos.external.TokenizarTarjetaRequest;
import com.aqua.plus.commons.dtos.external.TokenizarTarjetaResponse;
import com.aqua.plus.commons.dtos.external.WompiTransaccionRequest;
import java.util.List;
import java.util.Map;

public interface IWompiService {

    Map<String, Object> getMerchantInfo();

    Map<String, Object> getMerchantInfo(String clavePublica);

    String extraerAcceptanceToken(Map<String, Object> merchantInfo);

    Map<String, Object> crearTransaccion(WompiTransaccionRequest request);

    Map<String, Object> crearTransaccion(WompiTransaccionRequest request, String clavePrivada);

    Map<String, Object> consultarTransaccion(String idTransaccion);

    Map<String, Object> consultarTransaccion(String idTransaccion, String clavePrivada);

    List<Map<String, Object>> getInstitucionesFinancieras();

    List<Map<String, Object>> getInstitucionesFinancieras(String clavePublica);

    TokenizarTarjetaResponse tokenizarTarjeta(TokenizarTarjetaRequest request);

    TokenizarTarjetaResponse tokenizarTarjeta(TokenizarTarjetaRequest request, String clavePublica);
}
