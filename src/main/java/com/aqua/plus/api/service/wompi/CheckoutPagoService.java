package com.aqua.plus.api.service.wompi;

import com.aqua.plus.api.utils.EncriptarDesencriptar;
import com.aqua.plus.api.wompi.WompiEmpresaConfig;
import com.aqua.plus.api.wompi.WompiFeeCalculator;
import com.aqua.plus.api.wompi.WompiReferenceGenerator;
import com.aqua.plus.api.wompi.WompiSignatureService;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.CheckoutPagoRequest;
import com.aqua.plus.commons.dtos.external.CheckoutPagoResponse;
import com.aqua.plus.commons.dtos.external.EstadoPagoResponse;
import com.aqua.plus.commons.entities.EmpresaEntity;
import com.aqua.plus.commons.entities.EmpresaWompiEntity;
import com.aqua.plus.commons.entities.FacturaEntity;
import com.aqua.plus.commons.entities.PagoEntity;
import com.aqua.plus.commons.entities.UsuarioEntity;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import com.aqua.plus.commons.exceptions.SecureRequestException;
import com.aqua.plus.commons.repositories.EmpresaRepository;
import com.aqua.plus.commons.repositories.EmpresaWompiRepository;
import com.aqua.plus.commons.repositories.FacturaRepository;
import com.aqua.plus.commons.repositories.PagoRepository;
import com.aqua.plus.commons.repositories.UsuarioRepository;
import com.aqua.plus.commons.utils.Constantes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutPagoService {

    private final FacturaRepository facturaRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final EmpresaWompiRepository empresaWompiRepository;
    private final EncriptarDesencriptar encriptarDesencriptar;
    private final WompiSignatureService signatureService;
    private final WompiReferenceGenerator referenceGenerator;

    @Transactional
    public ResponseEntity<ResponseDTO> crearCheckout(CheckoutPagoRequest request) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        UsuarioEntity usuario = usuarioRepository.findByNombre(usuarioActual)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado: " + usuarioActual));

        FacturaEntity factura = facturaRepository.findActivaByIdWithRelations(request.getFacturaId())
                .orElseThrow(() -> new ProcessGenericException(
                        "Factura no encontrada: " + request.getFacturaId()));

        Integer idEmpresa = factura.getEmpresaClienteContador().getEmpresa().getId();
        validarAccesoFactura(usuario, factura, idEmpresa);
        validarFacturaPagable(factura);

        WompiFeeCalculator.FeeBreakdown fee = WompiFeeCalculator.calcular(factura.getPrecio());
        long amountInCents = fee.getTotalAmountInCents();
        WompiEmpresaConfig config = cargarConfigEmpresa(idEmpresa);

        String reference = referenceGenerator.generar(factura.getId());
        String firma = signatureService.generarFirmaIntegridad(
                reference, amountInCents, Constantes.COP, config.integritySecret());

        PagoEntity pago = PagoEntity.builder()
                .idUsuario(usuario.getId())
                .idFactura(factura.getId())
                .idEmpresa(idEmpresa)
                .referencia(reference)
                .montoCentavos(amountInCents)
                .moneda(Constantes.COP)
                .estado(Constantes.PAGO_ESTADO_PENDING)
                .metodoPago(Constantes.PAGO_METODO_WEB_CHECKOUT)
                .usuarioCreacion(usuarioActual)
                .build();
        pagoRepository.save(pago);

        String redirectUrl = construirRedirectUrl(config.redirectUrl(), factura.getId());
        advertirRedirectNoPublico(redirectUrl);
        String paymentUrl = construirPaymentUrl(
                config.checkoutUrl(),
                config.publicKey(),
                Constantes.COP,
                amountInCents,
                reference,
                firma,
                redirectUrl);

        CheckoutPagoResponse response = CheckoutPagoResponse.builder()
                .checkoutUrl(config.checkoutUrl())
                .paymentUrl(paymentUrl)
                .publicKey(config.publicKey())
                .currency(Constantes.COP)
                .facturaAmountInCents(fee.getFacturaAmountInCents())
                .comisionInCents(fee.getComisionInCents())
                .ivaInCents(fee.getIvaInCents())
                .feeTotalInCents(fee.getFeeTotalInCents())
                .amountInCents(amountInCents)
                .reference(reference)
                .signatureIntegrity(firma)
                .redirectUrl(redirectUrl)
                .build();

        log.info("Checkout creado - referencia={} facturaId={} facturaCentavos={} feeCentavos={} totalCentavos={}",
                reference, factura.getId(), fee.getFacturaAmountInCents(), fee.getFeeTotalInCents(), amountInCents);

        return ResponseEntity.ok(ResponseDTO.builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Checkout creado")
                .response(response)
                .build());
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> consultarEstado(Integer facturaId) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        UsuarioEntity usuario = usuarioRepository.findByNombre(usuarioActual)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado: " + usuarioActual));

        FacturaEntity factura = facturaRepository.findActivaByIdWithRelations(facturaId)
                .orElseThrow(() -> new ProcessGenericException("Factura no encontrada: " + facturaId));

        Integer idEmpresa = factura.getEmpresaClienteContador().getEmpresa().getId();
        validarAccesoFactura(usuario, factura, idEmpresa);

        String estadoPago = pagoRepository.findTopByIdFacturaOrderByFechaCreacionDesc(facturaId)
                .map(PagoEntity::getEstado)
                .orElse(null);

        String estadoFactura = factura.getEstado() != null ? factura.getEstado().getCodigo() : null;

        EstadoPagoResponse response = EstadoPagoResponse.builder()
                .facturaId(facturaId)
                .estadoPago(estadoPago)
                .estadoFactura(estadoFactura)
                .build();

        return ResponseEntity.ok(ResponseDTO.builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message(Constantes.CONSULTED_SUCCESSFULLY)
                .response(response)
                .build());
    }

    public WompiEmpresaConfig cargarConfigEmpresa(Integer idEmpresa) {
        EmpresaWompiEntity config = empresaWompiRepository.findByEmpresa_Id(idEmpresa)
                .orElseThrow(() -> new ProcessGenericException(
                        "La empresa no tiene configuradas las credenciales de Wompi"));

        if (!Boolean.TRUE.equals(config.getActivo())) {
            throw new ProcessGenericException("Las credenciales Wompi de la empresa están inactivas");
        }
        if (isBlank(config.getCheckoutUrl()) || isBlank(config.getRedirectUrl())) {
            throw new ProcessGenericException(
                    "La empresa debe configurar checkout_url y redirect_url de Wompi");
        }
        if (isBlank(config.getWompiClavePublica())
                || isBlank(config.getWompiSecretoIntegridad())
                || isBlank(config.getWompiSecretoEventos())) {
            throw new ProcessGenericException("Credenciales Wompi incompletas para la empresa");
        }

        return new WompiEmpresaConfig(
                config.getWompiClavePublica(),
                desencriptar(config.getWompiSecretoIntegridad()),
                desencriptar(config.getWompiSecretoEventos()),
                config.getCheckoutUrl().trim(),
                config.getRedirectUrl().trim()
        );
    }

    /**
     * Permite pago si el usuario es el cliente de la factura, o staff de la misma empresa.
     */
    private void validarAccesoFactura(UsuarioEntity usuario, FacturaEntity factura, Integer idEmpresa) {
        Integer personaId = usuario.getPersona() != null ? usuario.getPersona().getId() : null;
        Integer clienteFactura = factura.getEmpresaClienteContador().getCliente() != null
                ? factura.getEmpresaClienteContador().getCliente().getId()
                : null;

        if (personaId != null && personaId.equals(clienteFactura)) {
            return;
        }

        Integer empresaStaff = empresaRepository.findByUsuario_Id(usuario.getId())
                .map(EmpresaEntity::getId)
                .orElse(null);
        if (empresaStaff != null && empresaStaff.equals(idEmpresa)) {
            return;
        }

        throw new SecureRequestException("No tienes permiso para operar esta factura", HttpStatus.FORBIDDEN);
    }

    private void validarFacturaPagable(FacturaEntity factura) {
        if (factura.getPrecio() == null || factura.getPrecio() <= 0) {
            throw new SecureRequestException("La factura no tiene un valor válido para pagar", HttpStatus.BAD_REQUEST);
        }
        if (factura.getEstado() == null) {
            throw new SecureRequestException("La factura no tiene estado", HttpStatus.BAD_REQUEST);
        }
        String codigo = factura.getEstado().getCodigo() != null
                ? factura.getEstado().getCodigo().trim().toUpperCase()
                : "";
        String nombre = factura.getEstado().getNombre() != null
                ? factura.getEstado().getNombre().trim().toUpperCase()
                : "";

        if (Constantes.ESTADO_PAGADA.equalsIgnoreCase(codigo) || "PAGADA".equalsIgnoreCase(nombre)) {
            throw new SecureRequestException("La factura ya está pagada", HttpStatus.CONFLICT);
        }

        boolean porCodigo = Constantes.ESTADOS_FACTURA_PAGABLES_CODIGO.stream()
                .anyMatch(c -> c.equalsIgnoreCase(codigo));
        boolean porNombre = Constantes.ESTADOS_FACTURA_PAGABLES_NOMBRE.stream()
                .anyMatch(n -> n.equalsIgnoreCase(nombre) || n.replace('_', ' ').equalsIgnoreCase(nombre));

        if (!porCodigo && !porNombre) {
            throw new SecureRequestException(
                    "La factura no se puede pagar en estado: " + (nombre.isBlank() ? codigo : nombre),
                    HttpStatus.CONFLICT);
        }
    }

    private String construirRedirectUrl(String base, Integer facturaId) {
        String separator = base.contains("?") ? "&" : "?";
        return base + separator + "facturaId="
                + URLEncoder.encode(String.valueOf(facturaId), StandardCharsets.UTF_8);
    }

    /**
     * Arma la URL GET que Wompi Web Checkout espera.
     * El nombre del parámetro de firma debe ir como {@code signature%3Aintegrity}
     * (los ":" del nombre sí se percent-encodean). CloudFront/WAF de Wompi
     * también bloquea {@code redirect-url} hacia localhost (SSRF).
     */
    private String construirPaymentUrl(String checkoutUrl,
                                       String publicKey,
                                       String currency,
                                       Long amountInCents,
                                       String reference,
                                       String signatureIntegrity,
                                       String redirectUrl) {
        String base = checkoutUrl == null ? "" : checkoutUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/?"
                + "public-key=" + enc(publicKey)
                + "&currency=" + enc(currency)
                + "&amount-in-cents=" + amountInCents
                + "&reference=" + enc(reference)
                + "&signature%3Aintegrity=" + enc(signatureIntegrity)
                + "&redirect-url=" + enc(redirectUrl);
    }

    private void advertirRedirectNoPublico(String redirectUrl) {
        if (redirectUrl == null) {
            return;
        }
        String lower = redirectUrl.toLowerCase();
        if (lower.contains("localhost") || lower.contains("127.0.0.1")) {
            log.warn("redirect-url apunta a localhost. El WAF de Wompi (CloudFront) suele "
                    + "bloquear el checkout con 403. Usa HTTPS público (ngrok o el dominio de test). url={}",
                    redirectUrl);
        }
    }

    private String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String desencriptar(String valor) {
        try {
            return encriptarDesencriptar.desencriptar(valor);
        } catch (Exception e) {
            return valor;
        }
    }

    private boolean isBlank(String v) {
        return v == null || v.isBlank();
    }
}
