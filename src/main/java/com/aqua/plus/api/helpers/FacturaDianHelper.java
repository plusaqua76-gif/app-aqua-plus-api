package com.aqua.plus.api.helpers;

import java.util.Date;
import java.util.List;

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

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FacturaDianHelper {
	
	@Value("${app.jobs.facturas.electronica.limite}")
	private Integer limiteFactura;
	
	@Value("${dian.codigo-unidades.metro-cubico}")
	private String codigoProductoMetroCubicos;
	
	@Value("${dian.formas-pago.credito}")
	private String formaCredito;
	
	@Value("${dian.estados.en-proceso}")
	private String estadoEnProceso;
	
	@Value("${dian.estados.er-reintento}")
	private String estadoReintento;
	
	@Value("${dian.iva}")
	private Integer iva;
	
	@Value("${dian.usuario}")
	private String usuario;

	private final InvoiceRepository facturaRepository;
	private final FacturaDianServiceImpl dianService;
	private final ProductRepository  productoRepository;

	@Async("facturaExecutor")
	@Transactional
	public void procesar(Long id) {

		InvoiceEntity f = facturaRepository.findByIdForUpdate(id);

		try {
			f.setEstado(this.estadoEnProceso);
			f.setFechaUltimoIntento(new Date());

			 ResponseEntity<ResponseDTO> response =dianService.crearFacturaElectronica(FacturaDianMapper.INSTANCE.mapFactura(f, obtenerProducto(), iva, formaCredito, usuario));
			 if(!response.getStatusCode().equals(HttpStatus.OK) && !response.getStatusCode().equals(HttpStatus.CREATED)) {
				 f.setEstado(this.estadoReintento);
			 }

		} catch (Exception e) {
			f.setEstado(this.estadoReintento);
		}
	}
	
	@Transactional
    public List<Long> tomarFacturasPendientes() {
        return this.facturaRepository.obtenerIdsPendientes(this.limiteFactura);
    }
	
	public ProductEntity obtenerProducto() {
		return this.productoRepository.findByCodigoUnidad(codigoProductoMetroCubicos).orElse(null);
	}
}
