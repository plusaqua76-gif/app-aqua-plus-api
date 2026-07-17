package com.aqua.plus.api.helpers;

import java.util.*;

import com.aqua.plus.api.service.impl.FacturaServiceImpl;
import com.aqua.plus.api.tx.FacturaTxComponent;
import com.aqua.plus.commons.dtos.*;
import com.aqua.plus.commons.maps.InvoiceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.aqua.plus.api.maps.FacturaDianMapper;
import com.aqua.plus.api.service.impl.external.FacturaDianServiceImpl;
import com.aqua.plus.commons.dtos.external.RequestFacturaDto;
import com.aqua.plus.commons.entities.InvoiceEntity;
import com.aqua.plus.commons.entities.ProductEntity;
import com.aqua.plus.commons.repositories.InvoiceRepository;
import com.aqua.plus.commons.repositories.ProductRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacturaDianHelper {

    @Value("${app.jobs.facturas.electronica.limite}")
    private Integer limiteFactura;

    @Value("${dian.codigo-unidades.metro-cubico}")
    private String codigoProductoMetroCubicos;

    @Value("${dian.codigo-unidades.unidad}")
    private String codigoProductoUnidad;

    @Value("${dian.formas-pago.credito}")
    private String formaCredito;

    @Value("${dian.formas-pago.contado}")
    private String formaContado;

    @Value("${dian.estados.er-reintento}")
    private String estadoReintento;

    @Value("${dian.estados.sin-tarifas}")
    private String estadoSinTarifas;

    @Value("${dian.iva}")
    private Integer iva;

    @Value("${dian.usuario}")
    private String usuario;

    private final FacturaDianServiceImpl dianService;
    private final ProductRepository productoRepository;
    private final FacturaServiceImpl facturaService;
    private final InvoiceRepository facturaRepository;
    private final FacturaTxComponent facturaTxService;

    @Async("facturaExecutor")
    public void procesar(InvoiceDto factura) {
        Integer invoiceId = factura != null ? factura.getId() : null;
        log.warn("Job DIAN inicio procesar invoiceId={}", invoiceId);

        boolean tomado = facturaTxService.marcarEnProceso(invoiceId);
        if (!tomado) {
            log.warn("YA FUE TOMADA POR OTRO PROCESO:{} ", invoiceId);
            return;
        }

        boolean estadoFinalizado = false;
        try {
            List<TarifaConceptoDianDto> tarifas = mapearConceptos(factura);
            if (Objects.nonNull(tarifas) && !tarifas.isEmpty()) {
                var request = FacturaDianMapper.INSTANCE.mapFactura(factura, obtenerProducto(), obtenerProductoUnidad(),
                        iva, formaCredito, formaContado, usuario, tarifas);
                // Alinear customer con ECC de la factura 
                alinearClienteEmpresaDesdeEcc(request);
                log.warn("Job DIAN request invoiceId={} idCliente={} idEmpresa={}",
                        request.getId(), request.getIdCliente(), request.getIdEmpresa());
                ResponseEntity<ResponseDTO> response = dianService.crearFacturaElectronica(request);
                if (response != null && (response.getStatusCode().equals(HttpStatus.OK)
                        || response.getStatusCode().equals(HttpStatus.CREATED))) {
                    // guardarFactura actualizó el estado Alegra (SENT/REJECTED/etc.)
                    estadoFinalizado = true;
                    log.warn("Job DIAN OK invoiceId={} status={}", invoiceId, response.getStatusCode());
                } else {
                    this.facturaTxService.actualizarEstadoFinal(invoiceId, this.estadoReintento);
                    estadoFinalizado = true;
                    log.warn("Job DIAN reintento invoiceId={} status={}", invoiceId,
                            response != null ? response.getStatusCode() : null);
                }
            } else {
                this.facturaTxService.actualizarEstadoFinal(invoiceId, this.estadoSinTarifas);
                estadoFinalizado = true;
                log.warn("Job DIAN sin tarifas invoiceId={}", invoiceId);
            }
        } catch (Exception e) {
            log.error("Job DIAN error invoiceId={}", invoiceId, e);
            try {
                this.facturaTxService.actualizarEstadoFinal(invoiceId, this.estadoReintento);
                estadoFinalizado = true;
            } catch (Exception ex) {
                log.error("Job DIAN no pudo marcar reintento invoiceId={}", invoiceId, ex);
            }
        } finally {
            if (!estadoFinalizado) {
                log.error("Job DIAN quedó sin estado final, forzando reintento invoiceId={}", invoiceId);
                try {
                    this.facturaTxService.actualizarEstadoFinal(invoiceId, this.estadoReintento);
                } catch (Exception ex) {
                    log.error("Job DIAN fallo crítico dejando EN_PROCESO invoiceId={}", invoiceId, ex);
                }
            }
        }
    }

    @Transactional
    public List<InvoiceDto> tomarFacturasPendientes() {
        List<InvoiceEntity> entities = this.facturaRepository.obtenerIdsPendientes(this.limiteFactura);
        return InvoiceMapper.INSTANCE.listEntityToDtoList(entities);
    }

    public ProductEntity obtenerProducto() {
        return this.productoRepository.findByCodigoUnidad(codigoProductoMetroCubicos).orElse(null);
    }

    public ProductEntity obtenerProductoUnidad() {
        return this.productoRepository.findByCodigoUnidad(codigoProductoUnidad).orElse(null);
    }

    private List<TarifaConceptoDianDto> mapearConceptos( InvoiceDto dto) {

        Map<String, Object> detalle = this.facturaService.obtenerDetalleFacturaDian(dto.getFactura().getId());
        ObjectMapper mapper = new ObjectMapper();
        if (Objects.nonNull(detalle.get("code")) && Integer.parseInt(detalle.get("code").toString()) == HttpStatus.OK.value()) {
            Object value = detalle.get("tarifas");

            if (value instanceof List<?> list) {
                return list.stream()
                        .map(item -> mapper.convertValue(item, TarifaConceptoDianDto.class))
                        .toList();
            }
        }

        return null;
    }

    /**
     * El job debe facturar con el cliente/empresa de la ECC de la factura,
     * no solo con invoice.id_customer (puede desalinear nombre vs dirección del contador).
     */
    private void alinearClienteEmpresaDesdeEcc(RequestFacturaDto request) {
        if (request == null || request.getId() == null) {
            return;
        }
        facturaRepository.findClienteIdByInvoiceId(request.getId()).ifPresent(request::setIdCliente);
        facturaRepository.findEmpresaIdByInvoiceId(request.getId()).ifPresent(request::setIdEmpresa);
    }
}
