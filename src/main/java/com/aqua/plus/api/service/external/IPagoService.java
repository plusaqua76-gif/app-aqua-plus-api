package com.aqua.plus.api.service.external;

import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.CrearTransaccionRequest;
import com.aqua.plus.commons.dtos.external.IniciarPagoRequest;
import com.aqua.plus.commons.dtos.external.WebhookEventDTO;
import org.springframework.http.ResponseEntity;


public interface IPagoService {

    ResponseEntity<ResponseDTO> iniciarPago(IniciarPagoRequest req, String ipAddress);

    ResponseEntity<ResponseDTO> obtenerMerchant(Integer idEmpresa);

    ResponseEntity<ResponseDTO> obtenerBancosPse(Integer idEmpresa);

    ResponseEntity<ResponseDTO> crearTransaccion(CrearTransaccionRequest req);

    void procesarWebhook(WebhookEventDTO evento, String firmaRecibida);

    ResponseEntity<ResponseDTO> consultarYSincronizar(String referencia);

    ResponseEntity<ResponseDTO> sincronizarEstado(String referencia);

    ResponseEntity<ResponseDTO> obtenerUrlRedireccion(String referencia,
                                                       String deviceId,
                                                       String sessionId,
                                                       String ipCliente);
}
