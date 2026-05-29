package com.aqua.plus.api.service.external;

import com.aqua.plus.commons.dtos.PagoDTO;
import com.aqua.plus.commons.dtos.external.CrearTransaccionRequest;
import com.aqua.plus.commons.dtos.external.IniciarPagoRequest;
import com.aqua.plus.commons.dtos.external.IniciarPagoResponse;
import com.aqua.plus.commons.dtos.external.TransaccionResponse;
import com.aqua.plus.commons.dtos.external.WebhookEventDTO;

import java.util.List;
import java.util.Map;


public interface IPagoService {

    IniciarPagoResponse iniciarPago(IniciarPagoRequest req, String usuarioActual, String ipAddress);

    Map<String, Object> obtenerMerchant(Integer idEmpresa, String usuarioActual);

    List<Map<String, Object>> obtenerBancosPse(Integer idEmpresa, String usuarioActual);

    TransaccionResponse crearTransaccion(CrearTransaccionRequest req, String usuarioActual);

    void procesarWebhook(WebhookEventDTO evento, String firmaRecibida);

    PagoDTO consultarYSincronizar(String referencia, String usuarioActual);

    PagoDTO sincronizarEstado(String referencia);

    String obtenerUrlRedireccion(String referencia,
                                 String usuarioActual,
                                 String deviceId, 
                                 String sessionId, 
                                 String ipCliente);
}
