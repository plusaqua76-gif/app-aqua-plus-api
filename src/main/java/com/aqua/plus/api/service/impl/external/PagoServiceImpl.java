package com.aqua.plus.api.service.impl.external;

import com.aqua.plus.api.service.IFacturaService;
import com.aqua.plus.api.service.external.IPagoService;
import com.aqua.plus.api.service.external.IWompiService;
import com.aqua.plus.api.utils.EncriptarDesencriptar;
import com.aqua.plus.api.utils.JwePseService;
import com.aqua.plus.api.utils.WompiCredenciales;
import com.aqua.plus.api.utils.WompiIntegrityUtil;
import com.aqua.plus.api.utils.UtilsWompi;
import com.aqua.plus.commons.dtos.EstadoDTO;
import com.aqua.plus.commons.dtos.FacturaDTO;
import com.aqua.plus.commons.dtos.PagoDTO;
import com.aqua.plus.commons.dtos.PagoFacturaRequestDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.*;
import com.aqua.plus.commons.entities.EmpresaEntity;
import com.aqua.plus.commons.entities.EmpresaWompiEntity;
import com.aqua.plus.commons.entities.PagoEntity;
import com.aqua.plus.commons.entities.UsuarioEntity;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import com.aqua.plus.commons.exceptions.SecureRequestException;
import com.aqua.plus.commons.maps.PagoMapper;
import com.aqua.plus.commons.repositories.EmpresaClienteContadorRepository;
import com.aqua.plus.commons.repositories.EmpresaRepository;
import com.aqua.plus.commons.repositories.EmpresaWompiRepository;
import com.aqua.plus.commons.repositories.PagoRepository;
import com.aqua.plus.commons.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements IPagoService {

    private static final String KEY_STATUS               = "status";
    private static final String KEY_PAYMENT_METHOD_TYPE  = "payment_method_type";
    private static final String MEDIO_CARD               = "CARD";
    private static final String MEDIO_NEQUI              = "NEQUI";
    private static final String MEDIO_PSE                = "PSE";
    private static final String MEDIO_BANCOLOMBIA        = "BANCOLOMBIA_TRANSFER";

    private static final List<String> ESTADOS_TERMINALES =
        List.of("APPROVED", "DECLINED", "ERROR", "VOIDED");

    private static final List<String> MEDIOS_REDIRECT =
        List.of(MEDIO_PSE, MEDIO_BANCOLOMBIA);

    private final PagoRepository                    pagoRepo;
    private final EmpresaWompiRepository            empresaWompiRepo;
    private final EmpresaRepository                 empresaRepo;
    private final EmpresaClienteContadorRepository  empresaClienteContadorRepo;
    private final UsuarioRepository                 usuarioRepo;
    private final IWompiService                     wompiService;
    private final WompiIntegrityUtil                integrityUtil;
    private final UtilsWompi                        utilsWompi;
    private final JwePseService                     jwePseService;
    private final PagoMapper                        pagoMapper;
    private final PagoAsyncService                  pagoAsyncService;
    private final IFacturaService                   facturaService;
    private final EncriptarDesencriptar             encriptarDesencriptar;


    @Override
    @Transactional
    public ResponseEntity<ResponseDTO> iniciarPago(IniciarPagoRequest req, String ipAddress) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();

        String referencia = "AQP-" + UUID.randomUUID()
            .toString().replace("-", "").substring(0, 16).toUpperCase();

        PagoEntity pago = PagoEntity.builder()
            .idUsuario(req.getIdUsuario())
            .idFactura(req.getIdFactura())
            .idEmpresa(req.getIdEmpresa())
            .referencia(referencia)
            .montoCentavos(req.getMontoCentavos())
            .moneda("COP")
            .estado("PENDING")
            .emailCliente(cifrar(req.getEmailCliente()))
            .ipAddress(cifrar(ipAddress))
            .usuarioCreacion(usuarioActual)
            .build();
        pagoRepo.save(pago);

        WompiCredenciales creds = obtenerCredencialesPorUsuario(req.getIdUsuario());

        Map<String, Object> merchantInfo = wompiService.getMerchantInfo(creds.clavePublica());
        String acceptanceToken = wompiService.extraerAcceptanceToken(merchantInfo);

        String firma = integrityUtil.generarFirmaTransaccion(
            referencia, req.getMontoCentavos(), "COP", creds.secretoIntegridad());

        log.info("Pago iniciado - referencia: {} idUsuario: {} idFactura: {} idEmpresa: {}",
            referencia, req.getIdUsuario(), req.getIdFactura(), req.getIdEmpresa());

        IniciarPagoResponse response = IniciarPagoResponse.builder()
            .referencia(referencia)
            .montoCentavos(req.getMontoCentavos())
            .moneda("COP")
            .acceptanceToken(acceptanceToken)
            .clavePublica(creds.clavePublica())
            .firma(firma)
            .build();

        return ResponseEntity.ok(ResponseDTO.builder()
            .success(true)
            .code(HttpStatus.OK.value())
            .response(response)
            .build());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> obtenerMerchant(Integer idEmpresa) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        WompiCredenciales creds = obtenerCredenciales(idEmpresa, usuarioActual);
        return ResponseEntity.ok(ResponseDTO.builder()
            .success(true)
            .code(HttpStatus.OK.value())
            .response(wompiService.getMerchantInfo(creds.clavePublica()))
            .build());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> obtenerBancosPse(Integer idEmpresa) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        WompiCredenciales creds = obtenerCredenciales(idEmpresa, usuarioActual);
        return ResponseEntity.ok(ResponseDTO.builder()
            .success(true)
            .code(HttpStatus.OK.value())
            .response(wompiService.getInstitucionesFinancieras(creds.clavePublica()))
            .build());
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDTO> crearTransaccion(CrearTransaccionRequest req) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();

        PagoEntity pago = pagoRepo.findByReferencia(req.getReferencia())
            .orElseThrow(() -> new ProcessGenericException(
                "Pago no encontrado: " + req.getReferencia()));

        if (!"PENDING".equals(pago.getEstado())) {
            throw new SecureRequestException(
                "El pago ya fue procesado con estado: " + pago.getEstado(), HttpStatus.CONFLICT);
        }
        UsuarioEntity usuario = usuarioRepo.findByNombre(usuarioActual)
            .orElseThrow(() -> new SecurityException("Usuario no encontrado: " + usuarioActual));
        if (!usuario.getId().equals(pago.getIdUsuario())) {
            log.warn("Intento de crear transacción sobre pago ajeno — usuario: {} referencia: {}",
                usuarioActual, req.getReferencia());
            throw new SecurityException("No tienes permiso para operar este pago");
        }
        WompiCredenciales creds = obtenerCredencialesPorUsuario(pago.getIdUsuario());

        String firma = integrityUtil.generarFirmaTransaccion(
            pago.getReferencia(), pago.getMontoCentavos(), pago.getMoneda(),
            creds.secretoIntegridad());

        PaymentMethodDTO paymentMethod = construirPaymentMethod(req);

        String deviceSessionId = construirDeviceSessionId(req.getDeviceId(), req.getSessionId());
        WompiTransaccionRequest wompiReq = WompiTransaccionRequest.builder()
            .acceptanceToken(req.getAcceptanceToken())
            .amountInCents(pago.getMontoCentavos())
            .currency(pago.getMoneda())
            .customerEmail(descifrar(pago.getEmailCliente()))
            .reference(pago.getReferencia())
            .signature(firma)
            .paymentMethod(paymentMethod)
            .customerData(construirCustomerData(req))
            .deviceSessionId(deviceSessionId)
            .redirectUrl(req.getRedirectUrl())
            .build();

        Map<String, Object> wompiResp = wompiService.crearTransaccion(wompiReq, creds.clavePrivada());
        String idWompi = (String) wompiResp.get("id");

        pagoRepo.actualizarIdWompiYDispositivo(
            pago.getReferencia(), idWompi, req.getTipoMedio(),
            cifrar(req.getDeviceId()),
            cifrar(req.getSessionId()));

        if (MEDIO_CARD.equals(req.getTipoMedio())) {
            String estadoInmediato = (String) wompiResp.get(KEY_STATUS);
            if (ESTADOS_TERMINALES.contains(estadoInmediato)) {
                pagoRepo.actualizarEstado(
                    pago.getReferencia(), estadoInmediato, idWompi, "CARD", "INMEDIATO");
                log.info("CARD resuelta de forma inmediata — referencia: {} estado: {}",
                    pago.getReferencia(), estadoInmediato);
            }
        }

        log.info("Transacción creada - id Wompi: {} tipo: {} referencia: {}",
            idWompi, req.getTipoMedio(), pago.getReferencia());

        String redirectPath = null;
        if (MEDIOS_REDIRECT.contains(req.getTipoMedio())) {
            pagoAsyncService.obtenerYGuardarRedirectUrl(idWompi, pago.getReferencia(), creds.clavePrivada());
            redirectPath = "/api/v1/pagos/redirigir/" + pago.getReferencia();
        }

        TransaccionResponse transaccionResponse = construirRespuestaPorMedio(wompiResp, req.getTipoMedio(), redirectPath);
        return ResponseEntity.ok(ResponseDTO.builder()
            .success(true)
            .code(HttpStatus.OK.value())
            .response(transaccionResponse)
            .build());
    }


    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public void procesarWebhook(WebhookEventDTO evento, String firmaRecibida) {

        Map<String, Object> data   = evento.getData();
        Map<String, Object> txData = (Map<String, Object>) data.get("transaction");

        String idWompi    = (String) txData.get("id");
        String estado     = (String) txData.get(KEY_STATUS);
        String referencia = (String) txData.get("reference");
        String metodoPago = (String) txData.get(KEY_PAYMENT_METHOD_TYPE);

        PagoEntity pago = pagoRepo.findByReferencia(referencia)
            .orElseThrow(() -> new ProcessGenericException(
                "Pago no encontrado para referencia: " + referencia));
        WompiCredenciales creds = obtenerCredencialesPorEmpresa(pago.getIdEmpresa());

        if (!integrityUtil.validarFirmaWebhook(evento, firmaRecibida, creds.secretoEventos())) {
            log.warn("Webhook rechazado — firma inválida para transacción: {}", idWompi);
            throw new SecurityException("Firma de webhook inválida");
        }

        if (!ESTADOS_TERMINALES.contains(estado)) {
            log.info("Webhook ignorado — estado no terminal: {} para transacción: {}",
                estado, idWompi);
            return;
        }

        pagoRepo.actualizarEstado(referencia, estado, idWompi, metodoPago, "WEBHOOK");

        log.info("Pago actualizado por webhook - referencia: {} estado: {}", referencia, estado);

        if ("APPROVED".equals(estado)) {
            actualizarFacturaAlAprobado(referencia);
        }
    }


    @Override
    @Transactional
    public ResponseEntity<ResponseDTO> obtenerUrlRedireccion(String referencia,
                                                              String deviceId, String sessionId, String ipCliente) {

        PagoEntity pago = pagoRepo.findByReferencia(referencia)
            .orElseThrow(() -> new ProcessGenericException(
                "Pago no encontrado: " + referencia));

        if (pago.getRedirectUrlWompi() == null) {
            throw new ProcessGenericException(
                "URL de redirección no disponible aún para: " + referencia);
        }

        if (Boolean.TRUE.equals(pago.getRedirectConsumido())) {
            log.warn("Reintento de redirect ya consumido — referencia: {}", referencia);
            throw new SecurityException("La URL de redirección ya fue utilizada");
        }

        if (pago.getRedirectExpiraEn() != null
                && LocalDateTime.now().isAfter(pago.getRedirectExpiraEn())) {
            log.warn("Link de redirect expirado — referencia: {} expiró: {}",
                referencia, pago.getRedirectExpiraEn());
            throw new SecurityException("El link de redirección ha expirado. Inicia un nuevo pago.");
        }

        String ipGuardada      = descifrar(pago.getIpAddress());
        String deviceGuardado  = descifrar(pago.getDeviceId());
        String sessionGuardado = descifrar(pago.getSessionId());
        String redirectUrl     = descifrar(pago.getRedirectUrlWompi());

        if (ipGuardada != null && ipCliente != null && !ipGuardada.equals(ipCliente)) {
            log.warn("IP no coincide en redirect — referencia: {} registrada: {} actual: {}",
                referencia, ipGuardada, ipCliente);
            throw new SecurityException("La petición no proviene del mismo origen del pago");
        }

        if (deviceGuardado != null && !deviceGuardado.isBlank()
                && !deviceGuardado.equals(deviceId)) {
            log.warn("Device ID no coincide — referencia: {}", referencia);
            throw new SecurityException("Dispositivo no autorizado para este pago");
        }

        if (sessionGuardado != null && !sessionGuardado.isBlank()
                && !sessionGuardado.equals(sessionId)) {
            log.warn("Session ID no coincide — referencia: {}", referencia);
            throw new SecurityException("Sesión no autorizada para este pago");
        }

        int afectados = pagoRepo.consumirRedirect(referencia);
        if (afectados == 0) {
            throw new SecurityException("La URL de redirección ya fue utilizada");
        }

        log.info("Redirect autorizado y consumido — referencia: {} ip: {}",
            referencia, ipCliente);
        return toOk(Map.of("url", redirectUrl));
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDTO> consultarYSincronizar(String referencia) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();

        PagoEntity pago = pagoRepo.findByReferencia(referencia)
            .orElseThrow(() -> new ProcessGenericException(
                "Pago no encontrado para referencia: " + referencia));
        UsuarioEntity usuario = usuarioRepo.findByNombre(usuarioActual)
            .orElseThrow(() -> new SecurityException("Usuario no encontrado: " + usuarioActual));
        if (!usuario.getId().equals(pago.getIdUsuario())) {
            log.warn("Acceso denegado a pago ajeno — usuario: {} referencia: {}",
                usuarioActual, referencia);
            throw new SecurityException("No tienes permiso para consultar este pago");
        }

        if (ESTADOS_TERMINALES.contains(pago.getEstado())) {
            return toOk(pagoMapper.entityToDto(pago));
        }
        boolean esMedioRedireccion = pago.getMetodoPago() != null
                && MEDIOS_REDIRECT.contains(pago.getMetodoPago());
        boolean usuarioVisitoBanco = Boolean.TRUE.equals(pago.getRedirectConsumido());

        if (pago.getIdTransaccionWompi() != null
                && (!esMedioRedireccion || usuarioVisitoBanco)) {

            WompiCredenciales creds = obtenerCredencialesPorUsuario(pago.getIdUsuario());
            Map<String, Object> tx = wompiService.consultarTransaccion(
                pago.getIdTransaccionWompi(), creds.clavePrivada());
            String estadoWompi = (String) tx.get(KEY_STATUS);
            String metodoPago  = (String) tx.get(KEY_PAYMENT_METHOD_TYPE);

            if (ESTADOS_TERMINALES.contains(estadoWompi)) {
                pagoRepo.actualizarEstado(
                    referencia, estadoWompi,
                    pago.getIdTransaccionWompi(),
                    metodoPago, "RETURN_URL");
                pago.setEstado(estadoWompi);
                pago.setMetodoPago(metodoPago);

                if ("APPROVED".equals(estadoWompi)) {
                    actualizarFacturaAlAprobado(referencia);
                }

                return toOk(pagoMapper.entityToDto(pago));
            }
            log.info("Estado aún PENDING al volver del banco — referencia: {} estado Wompi: {}",
                referencia, estadoWompi);
        }

        return toOk(pagoMapper.entityToDto(pago));
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDTO> sincronizarEstado(String referencia) {
        PagoEntity pago = pagoRepo.findByReferencia(referencia)
            .orElseThrow(() -> new ProcessGenericException(
                "Pago no encontrado para referencia: " + referencia));

        if (pago.getIdTransaccionWompi() == null) {
            throw new ProcessGenericException(
                "El pago aún no tiene transacción Wompi: " + referencia);
        }

        if (ESTADOS_TERMINALES.contains(pago.getEstado())) {
            return toOk(pagoMapper.entityToDto(pago));
        }

        WompiCredenciales creds = obtenerCredencialesPorUsuario(pago.getIdUsuario());
        Map<String, Object> txData = wompiService.consultarTransaccion(
            pago.getIdTransaccionWompi(), creds.clavePrivada());
        String estadoWompi = (String) txData.get(KEY_STATUS);
        String metodoPago  = (String) txData.get(KEY_PAYMENT_METHOD_TYPE);

        log.info("Sincronizando - referencia: {} estado Wompi: {}", referencia, estadoWompi);

        if (ESTADOS_TERMINALES.contains(estadoWompi) && !estadoWompi.equals(pago.getEstado())) {
            pagoRepo.actualizarEstado(referencia, estadoWompi,
                pago.getIdTransaccionWompi(), metodoPago, "MANUAL_SYNC");
            pago.setEstado(estadoWompi);
            pago.setMetodoPago(metodoPago);

            if ("APPROVED".equals(estadoWompi)) {
                actualizarFacturaAlAprobado(referencia);
            }
        }

        return toOk(pagoMapper.entityToDto(pago));
    }


    // ─── Helpers de cifrado ──────────────────────────────────────────────────

    private String cifrar(String valor) {
        if (valor == null || valor.isBlank()) return valor;
        return encriptarDesencriptar.encriptar(valor);
    }

    /**
     * Descifra un valor. Si el dato fue almacenado en texto plano (migración),
     * desencriptar() retorna vacío; en ese caso se retorna el valor original
     * para garantizar compatibilidad con registros existentes.
     */
    private String descifrar(String valor) {
        if (valor == null || valor.isBlank()) return valor;
        String resultado = encriptarDesencriptar.desencriptar(valor);
        return (resultado == null || resultado.isEmpty()) ? valor : resultado;
    }


    // ─── Credenciales Wompi ──────────────────────────────────────────────────

    private WompiCredenciales obtenerCredenciales(Integer idEmpresa, String usuarioActual) {
        if (idEmpresa != null) {
            return obtenerCredencialesPorEmpresa(idEmpresa);
        }
        UsuarioEntity usuario = usuarioRepo.findByNombre(usuarioActual)
            .orElseThrow(() -> new SecurityException("Usuario no encontrado: " + usuarioActual));
        return obtenerCredencialesPorEmpresa(resolverEmpresaIdPorUsuario(usuario));
    }

    private WompiCredenciales obtenerCredencialesPorUsuario(Integer idUsuario) {
        UsuarioEntity usuario = usuarioRepo.findById(idUsuario)
            .orElseThrow(() -> new SecurityException("Usuario no encontrado con id: " + idUsuario));
        return obtenerCredencialesPorEmpresa(resolverEmpresaIdPorUsuario(usuario));
    }

    private Integer resolverEmpresaIdPorUsuario(UsuarioEntity usuario) {
        Integer personaId = usuario.getPersona() != null ? usuario.getPersona().getId() : null;
        if (personaId != null) {
            Integer empresaId = empresaClienteContadorRepo.findFirstEmpresaIdByClienteId(personaId).orElse(null);
            if (empresaId != null) return empresaId;
        }
        return empresaRepo.findByUsuario_Id(usuario.getId())
            .map(EmpresaEntity::getId)
            .orElseThrow(() -> new ProcessGenericException(
                "No se pudo resolver la empresa para el usuario id: " + usuario.getId()));
    }

    private WompiCredenciales obtenerCredencialesPorEmpresa(Integer idEmpresa) {
        if (idEmpresa == null) {
            throw new ProcessGenericException("No se pudo resolver la empresa para credenciales Wompi");
        }
        EmpresaWompiEntity config = empresaWompiRepo.findByEmpresa_Id(idEmpresa).orElse(null);
        if (config == null || !Boolean.TRUE.equals(config.getActivo())) {
            throw new RuntimeException("La empresa no tiene configuradas las credenciales de Wompi");
        }
        // Descifrar credenciales sensibles almacenadas con EncriptarDesencriptar
        return new WompiCredenciales(
            config.getWompiClavePublica(),
            descifrar(config.getWompiClavePrivada()),
            descifrar(config.getWompiSecretoIntegridad()),
            descifrar(config.getWompiSecretoEventos())
        );
    }


    // ─── Construcción de objetos ─────────────────────────────────────────────

    private String construirDeviceSessionId(String deviceId, String sessionId) {
        if (deviceId != null && !deviceId.isBlank() && sessionId != null && !sessionId.isBlank()) {
            return deviceId + ":" + sessionId;
        }
        if (sessionId != null && !sessionId.isBlank()) return sessionId;
        if (deviceId  != null && !deviceId.isBlank())  return deviceId;
        return null;
    }

    private PaymentMethodDTO construirPaymentMethod(CrearTransaccionRequest req) {
        return switch (req.getTipoMedio()) {

            case MEDIO_CARD -> {
                CardPaymentMethodDTO card = new CardPaymentMethodDTO();
                card.setType("CARD");
                card.setToken(req.getToken());
                card.setInstallments(req.getCuotas() != null ? req.getCuotas() : 1);
                yield card;
            }

            case MEDIO_NEQUI -> {
                NequiPaymentMethodDTO nequi = new NequiPaymentMethodDTO();
                nequi.setType(MEDIO_NEQUI);
                nequi.setPhoneNumber(req.getTelefono());
                yield nequi;
            }

            case MEDIO_PSE -> {
                PsePaymentMethodDTO pse = new PsePaymentMethodDTO();
                pse.setType("PSE");
                pse.setUserType(req.getTipoUsuario());
                pse.setUserLegalId(req.getDocumento());
                pse.setUserLegalIdType(req.getTipoDocumento());
                pse.setFinancialInstitutionCode(req.getCodigoBanco());
                pse.setPaymentDescription("Pago " + req.getReferencia());
                // Cifrado JWE (RSA-OAEP + A256GCM) — Wompi lo descifra automáticamente
                String ipOrigen  = req.getIpCliente() != null ? req.getIpCliente() : "";
                String fechaHoy  = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
                String documento = req.getDocumento() != null ? req.getDocumento() : "";
                pse.setReferenceOne(jwePseService.cifrar(ipOrigen));
                pse.setReferenceTwo(jwePseService.cifrar(fechaHoy));
                pse.setReferenceThree(jwePseService.cifrar(documento));
                yield pse;
            }

            case MEDIO_BANCOLOMBIA -> {
                BancolombiaPaymentMethodDTO bancolombia = new BancolombiaPaymentMethodDTO();
                bancolombia.setType(MEDIO_BANCOLOMBIA);
                bancolombia.setUserType("PERSON");
                bancolombia.setPaymentDescription("Pago " + req.getReferencia());
                yield bancolombia;
            }

            default -> throw new IllegalArgumentException(
                "Tipo de medio no soportado: " + req.getTipoMedio());
        };
    }

    private CustomerDataDTO construirCustomerData(CrearTransaccionRequest req) {
        if (!"PSE".equals(req.getTipoMedio())) return null;
        return CustomerDataDTO.builder()
            .fullName(req.getNombreCompleto())
            .phoneNumber(req.getTelefonoCliente())
            .build();
    }

    @SuppressWarnings("unchecked")
    private TransaccionResponse construirRespuestaPorMedio(
            Map<String, Object> wompiResp, String tipoMedio, String redirectUrl) {

        String idWompi    = (String) wompiResp.get("id");
        String referencia = (String) wompiResp.get("reference");
        String estado     = (String) wompiResp.get(KEY_STATUS);

        TransaccionResponse.TransaccionResponseBuilder builder = TransaccionResponse.builder()
            .idWompi(idWompi)
            .referencia(referencia)
            .estado(estado)
            .tipoMedio(tipoMedio)
            .redirectUrl(redirectUrl);

        if (MEDIO_CARD.equals(tipoMedio)) {
            Map<String, Object> pm    = (Map<String, Object>) wompiResp.get("payment_method");
            Map<String, Object> extra = pm != null ? (Map<String, Object>) pm.get("extra") : null;

            if (extra != null) {
                builder.marca((String) extra.get("brand"))
                       .ultimosCuatro((String) extra.get("last_four"))
                       .codigoRespuestaProcesador((String) extra.get("processor_response_code"));
            }
            if (pm != null && pm.get("installments") != null) {
                builder.cuotas(((Number) pm.get("installments")).intValue());
            }

            String statusMessage = (String) wompiResp.get("status_message");
            builder.estadoMensaje(statusMessage);

            String mensaje = switch (estado != null ? estado : "") {
                case "APPROVED" -> "Pago aprobado. Tu transacción fue procesada exitosamente.";
                case "DECLINED" -> "Pago rechazado: " +
                    (statusMessage != null ? statusMessage : "contacta a tu banco.");
                case "ERROR"    -> "Error al procesar el pago. Por favor intenta de nuevo.";
                default         -> "Procesando pago con tarjeta, espera un momento.";
            };
            return builder.mensaje(mensaje).build();
        }

        String mensaje = switch (tipoMedio) {
            case MEDIO_NEQUI      -> "Confirma el pago en tu app Nequi.";
            case MEDIO_PSE        -> "Serás redirigido a tu banco para completar el pago.";
            case MEDIO_BANCOLOMBIA -> "Serás redirigido a Bancolombia para completar el pago.";
            default               -> "Procesando pago...";
        };

        return builder.mensaje(mensaje).build();
    }

    private ResponseEntity<ResponseDTO> toOk(Object data) {
        return ResponseEntity.ok(ResponseDTO.builder()
            .success(true)
            .code(HttpStatus.OK.value())
            .response(data)
            .build());
    }

    private void actualizarFacturaAlAprobado(String referencia) {
        try {
            PagoEntity pago = pagoRepo.findByReferencia(referencia)
                .orElseThrow(() -> new ProcessGenericException(
                    "Pago no encontrado para referencia: " + referencia));

            if (pago.getIdFactura() == null) {
                log.warn("Pago referencia: {} sin idFactura — se omite actualización de factura", referencia);
                return;
            }

            EstadoDTO estadoPagada = EstadoDTO.builder().id(12).build();

            FacturaDTO facturaDTO = new FacturaDTO();
            facturaDTO.setId(pago.getIdFactura());
            facturaDTO.setEstado(estadoPagada);
            facturaDTO.setUsuarioModificacion(
                pago.getUsuarioCreacion() != null ? pago.getUsuarioCreacion() : "WOMPI_WEBHOOK");

            ResponseEntity<ResponseDTO> respuesta = facturaService.update(facturaDTO);
            ResponseDTO body = respuesta.getBody();

            if (body != null && Boolean.TRUE.equals(body.getSuccess())) {
                log.info("Factura id={} marcada como PAGADA — referencia: {}", pago.getIdFactura(), referencia);
            } else {
                String msg = body != null ? body.getMessage() : "sin respuesta";
                log.error("Error al marcar factura como PAGADA — referencia: {} factura id={} motivo: {}",
                    referencia, pago.getIdFactura(), msg);
            }

        } catch (Exception e) {
            log.error("Error inesperado actualizando factura — referencia: {} — {}",
                referencia, e.getMessage(), e);
        }
    }
}
