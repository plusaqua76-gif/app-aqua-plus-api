package com.aqua.plus.api.helpers;

import java.util.*;

import com.aqua.plus.api.service.impl.FacturaServiceImpl;
import com.aqua.plus.api.tx.FacturaTxComponent;
import com.aqua.plus.commons.dtos.InvoiceDto;
import com.aqua.plus.commons.dtos.TarifaConceptoDianDto;
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
import com.aqua.plus.commons.dtos.ResponseDTO;
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

        boolean tomado = facturaTxService.marcarEnProceso(factura.getId());

        if (!tomado) {
            log.warn("YA FUE TOMADA POR OTRO PROCESO:{} " , factura.getId());
            return; // alguien más lo tomó
        }

        try {
            List<TarifaConceptoDianDto> tarifas = mapearConceptos( factura);
            if (Objects.nonNull(tarifas) && !tarifas.isEmpty()) {
                ResponseEntity<ResponseDTO> response = dianService.crearFacturaElectronica(FacturaDianMapper.INSTANCE.mapFactura(factura, obtenerProducto(), obtenerProductoUnidad(), iva, formaCredito, formaContado, usuario, tarifas));
                if (!response.getStatusCode().equals(HttpStatus.OK) && !response.getStatusCode().equals(HttpStatus.CREATED)) {
                    this.facturaTxService.actualizarEstadoFinal(factura.getId(), this.estadoReintento);
                }
            } else {
                this.facturaTxService.actualizarEstadoFinal(factura.getId(), this.estadoSinTarifas);
            }


        } catch (Exception e) {
            log.error("Error: {} " ,e.getLocalizedMessage());
            this.facturaTxService.actualizarEstadoFinal(factura.getId(), this.estadoReintento);
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
}
