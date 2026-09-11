package com.aqua.plus.api.service.wompi;

import com.aqua.plus.api.service.IFacturaService;
import com.aqua.plus.api.wompi.WompiFeeCalculator;
import com.aqua.plus.api.wompi.WompiReferenceRules;
import com.aqua.plus.api.wompi.WompiTransaction;
import com.aqua.plus.commons.dtos.PagoFacturaRequestDTO;
import com.aqua.plus.commons.dtos.PagoItemDTO;
import com.aqua.plus.commons.dtos.ProcesoPagoResponseDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.FacturaEntity;
import com.aqua.plus.commons.entities.PagoEntity;
import com.aqua.plus.commons.repositories.FacturaRepository;
import com.aqua.plus.commons.repositories.PagoRepository;
import com.aqua.plus.commons.utils.Constantes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Wompi solo informa el estado. La factura se marca pagada únicamente con {@code procesarPagos}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WompiPagoConfirmacionService {

    private final FacturaRepository facturaRepository;
    private final PagoRepository pagoRepository;
    private final IFacturaService facturaService;

    @Transactional
    public void confirmar(Integer facturaId, WompiTransaction tx, String origen) {
        if (facturaId == null || tx == null) {
            return;
        }
        FacturaEntity factura = facturaRepository.findActivaByIdWithRelations(facturaId).orElse(null);
        if (factura == null) {
            log.warn("No se encontró factura id={} para confirmar Wompi", facturaId);
            return;
        }
        confirmar(factura, tx, origen);
    }

    @Transactional
    public void confirmar(FacturaEntity factura, WompiTransaction tx, String origen) {
        if (factura == null || tx == null) {
            return;
        }

        String codigoEstado = codigoEstado(factura);
        if (Constantes.ESTADO_PAGADA.equalsIgnoreCase(codigoEstado)
                || Constantes.ESTADO_PAGO_PARCIAL.equalsIgnoreCase(codigoEstado)) {
            log.info("Factura id={} ya está {} — no se toca", factura.getId(), codigoEstado);
            return;
        }

        if (!WompiReferenceRules.perteneceAFactura(tx.reference(), factura.getId())) {
            log.warn("Reference Wompi no es de la factura — factura={} reference={}",
                    factura.getId(), tx.reference());
            return;
        }

        String status = tx.status() != null ? tx.status().trim().toUpperCase() : "";
        if (!Constantes.PAGO_ESTADO_APPROVED.equals(status)) {
            registrarEstadoPago(factura.getId(), status, tx, origen);
            log.info("Wompi status={} — no se marca PAG factura={}", status, factura.getId());
            return;
        }

        long esperado;
        try {
            esperado = WompiFeeCalculator.calcular(factura.getPrecio()).getTotalAmountInCents();
        } catch (IllegalArgumentException e) {
            log.warn("No se pudo recalcular fee Wompi factura={}: {}", factura.getId(), e.getMessage());
            return;
        }

        if (tx.amountInCents() == null || tx.amountInCents() != esperado
                || tx.currency() == null || !Constantes.COP.equalsIgnoreCase(tx.currency())) {
            log.warn("Monto/currency Wompi no cuadran — factura={} esperado={} COP recibido={} {}",
                    factura.getId(), esperado, tx.amountInCents(), tx.currency());
            return;
        }

        Integer idEmpresa = factura.getEmpresaClienteContador().getEmpresa().getId();
        PagoItemDTO item = new PagoItemDTO();
        item.setIdFactura(factura.getId());
        item.setValorPago(factura.getPrecio());

        PagoFacturaRequestDTO request = new PagoFacturaRequestDTO();
        request.setIdEmpresa(idEmpresa);
        request.setUsuarioCreacion(origenUsuario(factura, origen));
        request.setPagos(List.of(item));

        ResponseEntity<ResponseDTO> respuesta = facturaService.procesarPagos(request);
        ResponseDTO body = respuesta.getBody();
        if (body == null || !(body.getResponse() instanceof ProcesoPagoResponseDTO proceso)
                || proceso.getPagosCompletos() <= 0) {
            log.error("procesarPagos no dejó la factura en PAG — factura={} success={}",
                    factura.getId(), body != null ? body.getSuccess() : null);
            return;
        }

        FacturaEntity recargada = facturaRepository.findById(factura.getId()).orElse(factura);
        if (!Constantes.ESTADO_PAGADA.equalsIgnoreCase(codigoEstado(recargada))) {
            log.error("Factura id={} no quedó en PAG después de procesarPagos — no se asume pagada",
                    factura.getId());
            return;
        }

        registrarEstadoPago(factura.getId(), Constantes.PAGO_ESTADO_APPROVED, tx, origen);
        log.info("Factura id={} marcada PAG vía procesarPagos — transacción={} origen={}",
                factura.getId(), tx.id(), origen);
    }

    private void registrarEstadoPago(Integer facturaId, String estado, WompiTransaction tx, String origen) {
        if (estado == null || estado.isBlank()) {
            return;
        }
        pagoRepository.findTopByIdFacturaOrderByFechaCreacionDesc(facturaId).ifPresent(pago -> {
            if (Constantes.PAGO_ESTADO_PENDING.equalsIgnoreCase(pago.getEstado())) {
                String metodo = tx.paymentMethodType() != null ? tx.paymentMethodType() : pago.getMetodoPago();
                pagoRepository.actualizarEstadoSiPendiente(
                        pago.getReferencia(), estado, tx.id(), metodo, origen);
            }
        });
    }

    private String origenUsuario(FacturaEntity factura, String origen) {
        return pagoRepository.findTopByIdFacturaOrderByFechaCreacionDesc(factura.getId())
                .map(PagoEntity::getUsuarioCreacion)
                .filter(u -> u != null && !u.isBlank())
                .orElse(origen);
    }

    private String codigoEstado(FacturaEntity factura) {
        return factura.getEstado() != null && factura.getEstado().getCodigo() != null
                ? factura.getEstado().getCodigo().trim()
                : "";
    }
}
