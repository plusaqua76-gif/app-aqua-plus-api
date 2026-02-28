package com.aqua.plus.api.maps;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.aqua.plus.commons.dtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import com.aqua.plus.api.utils.Utils;
import com.aqua.plus.commons.dtos.external.RequestFacturaDto;
import com.aqua.plus.commons.dtos.external.RequestFacturaDto.CodigoEstandar;
import com.aqua.plus.commons.dtos.external.RequestFacturaDto.Descuento;
import com.aqua.plus.commons.dtos.external.RequestFacturaDto.Pago;
import com.aqua.plus.commons.dtos.external.RequestFacturaDto.Producto;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.Resolution;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.StandardCode;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.Taxe;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.TotalAmounts;
import com.aqua.plus.commons.entities.InvoiceEntity;
import com.aqua.plus.commons.entities.ProductEntity;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.Company;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.Customer;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.DiscountsAndCharges;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.InvoicePeriod;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.Item;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.Payment;
import com.aqua.plus.commons.enums.DocumentTypeDianEnum;
import com.aqua.plus.commons.enums.OrganizationTypeDianEnum;
import com.aqua.plus.commons.enums.TaxTypeEnum;
import com.aqua.plus.commons.enums.TypeDocumentDianEnum;
import com.aqua.plus.commons.enums.TypeDocumentEnum;
import com.aqua.plus.commons.maps.ResolucionMapper;
import com.aqua.plus.commons.utils.Constantes;

@Mapper(uses = { ResolucionMapper.class })
public interface FacturaDianMapper {

	FacturaDianMapper INSTANCE = Mappers.getMapper(FacturaDianMapper.class);

	@Mapping(target = "resolutionNumber", source = "numero")
	@Mapping(target = "prefix", source = "prefijo")
	@Mapping(target = "minNumber", source = "numeroMinimo")
	@Mapping(target = "maxNumber", source = "numeroMaximo")
	@Mapping(target = "startDate", source = "fechaInicio", dateFormat = "yyyy-MM-dd")
	@Mapping(target = "endDate", source = "fechaFin", dateFormat = "yyyy-MM-dd")
	@Mapping(target = "technicalKey", source = "claveTecnica")
	Resolution resolucionDtoToResolucionDian(ResolutionDto dto);

	@Mapping(target = "id", source = "idEmpresaDian")
	@Mapping(target = "organizationType", expression = "java(getTypeOrganization())")
	@Mapping(target = "identificationType", expression = "java(getTypeDocument())")
	@Mapping(target = "identificationNumber", source = "nit")
	@Mapping(target = "name", source = "nombre")
	Company empresaDtoToCompanyDian(EmpresaDTO empresa);

	@Mapping(target = "name", source = "nombre")
	@Mapping(target = "organizationType", source = "tipoDocumento", qualifiedByName = "getTypeOrganizationCustomer")
	@Mapping(target = "identificationType", source = "tipoDocumento.idTipoDian")
	@Mapping(target = "identificationNumber", source = "numeroCedula")
	@Mapping(target = "email", source = "correo")
	@Mapping(target = "regimeCode", source = "codigosResidenciaFiscal")
	Customer clienteDtoToCustomerDian(PersonaDTO persona);

	@Named("getTypeOrganization")
	default Integer getTypeOrganization() {
		return OrganizationTypeDianEnum.PERSONA_JURIDICA.getId();
	}

	@Named("getTypeOrganizationCustomer")
	default Integer getTypeOrganizationCustomer(final TipoDocumentoDTO tipoDocumento) {
		if(TypeDocumentEnum.NIT.getCodigo().equals(tipoDocumento.getCodigo())) {
			return OrganizationTypeDianEnum.PERSONA_JURIDICA.getId();
		}else {
			return OrganizationTypeDianEnum.PERSONA_NATURAL.getId();
		}
	}

	@Named("getTypeDocument")
	default Integer getTypeDocument() {
		return TypeDocumentDianEnum.NIT.getId();
	}

	default RequestInvoiceDto mapDataFacturaEletronica(ResolutionDto resolucionDto, EmpresaDTO empresa,
			PersonaDTO persona, RequestFacturaDto factura, CorreoGeneralDTO correoPersona,Long numeroFactura, Integer periodosFacturados) {
		RequestInvoiceDto rq = new RequestInvoiceDto();
		rq.setDocumentType(DocumentTypeDianEnum.ESTANDAR.getCodigo());
		rq.setResolution(resolucionDtoToResolucionDian(resolucionDto));
		rq.setCompany(empresaDtoToCompanyDian(empresa));
		persona.setCorreo(correoPersona.getCorreo());
		rq.setCustomer(clienteDtoToCustomerDian(persona));
		List<Payment> mediosPagos = new ArrayList<>(0);
		mediosPagos.add(Payment.builder().paymentForm(factura.getMedioPago().getForma())
				.paymentMethod(factura.getMedioPago().getMedio()).paymentDueDate(factura.getMedioPago().getFechaFin()).build());
		rq.setPayments(mediosPagos);
		rq.setItems(mapProductos(factura));
		rq.setTotalAmounts(mapTotalAmount(factura));
		rq.setNumber(numeroFactura);
		rq.setDiscountsAndCharges(aplicarDescuento(factura));
		if(Objects.nonNull(factura.getFechaEmision())) {
			rq.setInvoicePeriod(InvoicePeriod.builder().startDate(Utils.formatDate(Utils.restarMes(factura.getFechaEmision(), periodosFacturados), "yyyy-MM-dd")).endDate(Utils.formatDate(factura.getFechaEmision(), "yyyy-MM-dd")).build());
		}
		rq.setIdCliente(persona.getId());
		rq.setIdEmpresa(empresa.getId());
		return rq;
	}
	
	default List<DiscountsAndCharges> aplicarDescuento(RequestFacturaDto factura){
		List<DiscountsAndCharges> descuentos = new ArrayList<>(0);
		
		if (Objects.nonNull(factura) && Objects.nonNull(factura.getProductos()) && !factura.getProductos().isEmpty()) {
			BigDecimal totalBruto = new BigDecimal(0);
			BigDecimal descuentoTotal = new BigDecimal(0);
			BigDecimal cargoTotal = new BigDecimal(0);
			BigDecimal cantidadDescuento =new BigDecimal(0);
			BigDecimal cantidadCargo =new BigDecimal(0);
			for (Producto producto : factura.getProductos()) {
				BigDecimal precio = producto.getPrecio().multiply(producto.getCantidad());
				BigDecimal descuento = precio.multiply(producto.getDescuento()).divide(BigDecimal.valueOf(100));
				cantidadDescuento = cantidadDescuento.add(producto.getDescuento());
				cantidadCargo=cantidadCargo.add(producto.getCargo());
				BigDecimal cargoDescuento = precio.multiply(producto.getCargo()).divide(BigDecimal.valueOf(100));
				
				totalBruto = totalBruto.add(precio);
				descuentoTotal =  descuentoTotal.add(descuento);
				cargoTotal =  cargoTotal.add(cargoDescuento);
			}
			if(Objects.nonNull(factura.getDescuentos()) && !factura.getDescuentos().isEmpty()) {
				
				if(Objects.nonNull(cargoTotal) && cargoTotal.intValue()!=0) {
					Descuento descuento = factura.getDescuentos().stream().filter(item -> item.getIndCargo()).findFirst().orElse(null);
					DiscountsAndCharges descuentoCargo = DiscountsAndCharges.builder().isCharge(Boolean.TRUE).reasonCode(descuento.getCodigoRazon()).percentageAmount(cantidadCargo).amount(cargoTotal).baseAmount(totalBruto).reason(descuento.getRazon()).build();
					descuentos.add(descuentoCargo);
				}
				
				if(Objects.nonNull(descuentoTotal) && descuentoTotal.intValue()!=0) {
					Descuento descuento = factura.getDescuentos().stream().filter(item -> !item.getIndCargo()).findFirst().orElse(null);
					DiscountsAndCharges descuentoFinal = DiscountsAndCharges.builder().isCharge(Boolean.FALSE).reasonCode(descuento.getCodigoRazon()).percentageAmount(cantidadDescuento).amount(descuentoTotal).baseAmount(totalBruto).reason(descuento.getRazon()).build();
					descuentos.add(descuentoFinal);
				}
			}
			
			
		}
		
		return descuentos;
	}

	default List<Item> mapProductos(RequestFacturaDto factura) {
		List<Item> articulos = new ArrayList<>(0);
		if (Objects.nonNull(factura) && Objects.nonNull(factura.getProductos()) && !factura.getProductos().isEmpty()) {
			for (Producto producto : factura.getProductos()) {
				List<Taxe> taxes = new ArrayList<>(0);
				BigDecimal precio = producto.getPrecio().multiply(producto.getCantidad());
				BigDecimal descuento = precio.multiply(producto.getDescuento()).divide(BigDecimal.valueOf(100));
				BigDecimal cargoDescuento = precio.multiply(producto.getCargo()).divide(BigDecimal.valueOf(100));
				BigDecimal precioFinal = precio.subtract(cargoDescuento).subtract(descuento) ;
				BigDecimal iva = precioFinal.multiply(producto.getIva()).divide(BigDecimal.valueOf(100));

				Taxe taxe = Taxe.builder().taxCode(TaxTypeEnum.IVA.getCodigo()).taxAmount(iva)
						.taxPercentage(producto.getIva().toString()).taxableAmount(precioFinal).build();
				taxes.add(taxe);

				Item item = Item.builder().standardCode(StandardCode.builder().id(producto.getCodigoEstandar().getId()).identificationId(producto.getCodigoEstandar().getIdIdentificacion()).build()).charge(producto.getCargo()).chargeAmount(cargoDescuento).taxes(taxes)
						.description(producto.getNombre()).price(producto.getPrecio()).discount(producto.getDescuento())
						.discountAmount(descuento).quantity(producto.getCantidad())
						.unitCode(producto.getCodigoUnidadMedida()).subtotal(precio).taxAmount(iva)
						.total(precioFinal.add(iva)).build();
				articulos.add(item);
			}
		}

		return articulos;
	}

	default TotalAmounts mapTotalAmount(RequestFacturaDto factura) {
		TotalAmounts total = null;
		if (Objects.nonNull(factura) && Objects.nonNull(factura.getProductos()) && !factura.getProductos().isEmpty()) {
			BigDecimal totalBruto = new BigDecimal(0);
			BigDecimal totalImponible =new BigDecimal(0);
			BigDecimal totalImpuesto =new BigDecimal(0);
			BigDecimal descuentoTotal = new BigDecimal(0);
			BigDecimal cargoTotal = new BigDecimal(0);
			BigDecimal totalAnticipado =new BigDecimal(0);
			BigDecimal totalPagar =new BigDecimal(0);
			for (Producto producto : factura.getProductos()) {
				BigDecimal precio = producto.getPrecio().multiply(producto.getCantidad());
				BigDecimal descuento = precio.multiply(producto.getDescuento()).divide(BigDecimal.valueOf(100));
				BigDecimal cargoDescuento = precio.multiply(producto.getCargo()).divide(BigDecimal.valueOf(100));
				BigDecimal precioFinal = precio.subtract(cargoDescuento).subtract(descuento) ;
				BigDecimal iva = precioFinal.multiply(producto.getIva()).divide(BigDecimal.valueOf(100));
				
				totalBruto = totalBruto.add(precio);
				totalImponible =  totalImponible.add(precioFinal);
				totalImpuesto =  totalImpuesto.add(iva);
				descuentoTotal =  descuentoTotal.add(descuento);
				cargoTotal =  cargoTotal.add(cargoDescuento);
				totalPagar = totalPagar.add(precioFinal).add(iva);
			}
			total = TotalAmounts.builder().advanceTotal(factura.getTotalAnticipado()).grossTotal(totalBruto)
					.taxableTotal(totalImponible).taxTotal(totalImpuesto).discountTotal(descuentoTotal)
					.chargeTotal(cargoTotal).advanceTotal(totalAnticipado).payableTotal(totalPagar)
					.currencyCode(Constantes.COP).build();
		}

		return total;
	}
	
	default RequestFacturaDto mapFactura(InvoiceEntity factura, ProductEntity productoMc, ProductEntity productoUnidad, Integer iva, String formaPago, String usuario, List<TarifaConceptoDianDto> tarifas) {
		RequestFacturaDto request =RequestFacturaDto.builder().build();
		request.setId(factura.getId());
		request.setIdCliente(factura.getCliente().getId());
		request.setIdEmpresa(factura.getEmpresa().getId());
		List<Producto> productos = new ArrayList<RequestFacturaDto.Producto>(0);
		for(TarifaConceptoDianDto item: tarifas){
			for(TarifaConceptoDianDto.ConceptoDto concepto : item.getConceptos()){
				if(Objects.nonNull(concepto.getConsumoCliente()) && concepto.getConsumoCliente() !=0 && !concepto.getValor().equals("0")) {
					productos.add(Producto.builder().codigoEstandar(CodigoEstandar.builder().id(productoMc.getCodigoEstandar()).idIdentificacion(String.valueOf(productoMc.getId())).build()).precio(new BigDecimal(concepto.getValor())).cantidad(new BigDecimal(concepto.getConsumoCliente())).iva(new BigDecimal(iva)).nombre(item.getNombre().concat("-").concat(concepto.getNombre())).codigoUnidadMedida(productoMc.getCodigoUnidad()).descuento(new BigDecimal(0)).cargo(new BigDecimal(0)).build());
				}else if(Objects.nonNull(concepto.getValor()) && !concepto.getValor().equals("0")){
					productos.add(Producto.builder().codigoEstandar(CodigoEstandar.builder().id(productoUnidad.getCodigoEstandar()).idIdentificacion(String.valueOf(productoUnidad.getId())).build()).precio(new BigDecimal(concepto.getValor())).cantidad(new BigDecimal(1)).iva(new BigDecimal(iva)).nombre(item.getNombre().concat("-").concat(concepto.getNombre())).codigoUnidadMedida(productoUnidad.getCodigoUnidad()).descuento(new BigDecimal(0)).cargo(new BigDecimal(0)).build());
				}

			}
		}

		request.setProductos(productos);
		request.setMedioPago(Pago.builder().forma(formaPago).medio(factura.getFactura().getTipoPago().getCodigoDian()).fechaFin(formatFechaFin(factura.getFactura().getFechaFin())).build());
		request.setUsuario(usuario);
		request.setFechaUltimoIntento(factura.getFechaUltimoIntento());
		request.setFechaEmision(factura.getFactura().getFechaEmision());
		return request;
	}
	
	default String formatFechaFin(Date fecha) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
		return sdf.format(fecha);
	}
}
