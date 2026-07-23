package com.aqua.plus.api.maps;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.aqua.plus.commons.entities.ProductEntity;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.Company;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.Customer;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.Address;
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
	BigDecimal CIEN = BigDecimal.valueOf(100);
	String AJUSTE_CENTENA = "Ajuste a la centena";

	record MontosProducto(BigDecimal precio, BigDecimal descuento, BigDecimal cargo, BigDecimal iva,
			BigDecimal precioFinal, BigDecimal pctDescuento, BigDecimal pctCargo) {
	}

	record FacturaCalculada(BigDecimal totalBruto, BigDecimal totalImponible, BigDecimal totalIva,
			BigDecimal totalDescuento, BigDecimal totalCargo, BigDecimal totalPagar,
			BigDecimal sumaPctDescuento, BigDecimal sumaPctCargo, BigDecimal ajuste) {
	}

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
	@Mapping(target = "dv", source = "digitoVerificacion")
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

	default String nombreCompleto(PersonaDTO persona) {
		return Stream.of(persona.getNombre(), persona.getSegundoNombre(), persona.getApellido(), persona.getSegundoApellido())
				.filter(s -> s != null && !s.isBlank())
				.map(String::trim)
				.collect(Collectors.joining(" "));
	}

	default Address buildDireccionCliente(final String direccionContador, final EmpresaDTO empresa) {
		String city = null;
		String department = null;
		if (empresa != null && empresa.getDireccion() != null) {
			if (empresa.getDireccion().getCiudad() != null) {
				city = empresa.getDireccion().getCiudad().getCodigoDian();
			}
			if (empresa.getDireccion().getDepartamento() != null) {
				department = empresa.getDireccion().getDepartamento().getCodigoDian();
			}
		}
		if (direccionContador == null && city == null && department == null) {
			return null;
		}
		return Address.builder().address(direccionContador).city(city).department(department).country("CO").build();
	}

	default RequestInvoiceDto mapDataFacturaEletronica(ResolutionDto resolucionDto, EmpresaDTO empresa,
			PersonaDTO persona, RequestFacturaDto factura, CorreoGeneralDTO correoPersona, Address direccionCliente,
			Long numeroFactura, Integer periodosFacturados) {
		RequestInvoiceDto rq = new RequestInvoiceDto();
		rq.setDocumentType(DocumentTypeDianEnum.ESTANDAR.getCodigo());
		rq.setResolution(resolucionDtoToResolucionDian(resolucionDto));
		rq.setCompany(empresaDtoToCompanyDian(empresa));
		persona.setCorreo(correoPersona.getCorreo());
		persona.setNombre(nombreCompleto(persona));
		Customer customer = clienteDtoToCustomerDian(persona);
		customer.setAddress(direccionCliente);
		rq.setCustomer(customer);
		List<Payment> mediosPagos = new ArrayList<>(0);
		mediosPagos.add(Payment.builder().paymentForm(factura.getMedioPago().getForma())
				.paymentMethod(factura.getMedioPago().getMedio()).paymentDueDate(factura.getMedioPago().getFechaFin()).build());
		rq.setPayments(mediosPagos);

		FacturaCalculada calc = calcularFactura(factura.getProductos());
		BigDecimal ajuste = factura.isAjusteCentena() ? calc.ajuste() : BigDecimal.ZERO;
		if (ajuste.signum() > 0) {
			crearProductoAjusteCentena(factura, ajuste);
		}

		rq.setItems(mapProductos(factura));
		rq.setTotalAmounts(mapTotalAmount(factura, calc, ajuste));
		rq.setNumber(numeroFactura);
		rq.setDiscountsAndCharges(aplicarDescuento(factura, calc, ajuste));
		if(Objects.nonNull(factura.getFechaEmision())) {
			rq.setInvoicePeriod(InvoicePeriod.builder().startDate(Utils.formatDate(Utils.restarMes(factura.getFechaEmision(), periodosFacturados), "yyyy-MM-dd")).endDate(Utils.formatDate(factura.getFechaEmision(), "yyyy-MM-dd")).build());
		}
		rq.setIdCliente(persona.getId());
		rq.setIdEmpresa(empresa.getId());
		return rq;
	}

	default MontosProducto calcularMontosProducto(Producto producto) {
		BigDecimal pctDescuento = Objects.requireNonNullElse(producto.getDescuento(), BigDecimal.ZERO);
		BigDecimal pctCargo = Objects.requireNonNullElse(producto.getCargo(), BigDecimal.ZERO);
		BigDecimal ivaPct = Objects.requireNonNullElse(producto.getIva(), BigDecimal.ZERO);
		BigDecimal precio = producto.getPrecio().multiply(producto.getCantidad());
		BigDecimal descuento = precio.multiply(pctDescuento).divide(CIEN);
		BigDecimal cargo = precio.multiply(pctCargo).divide(CIEN);
		BigDecimal precioFinal = precio.subtract(cargo).subtract(descuento);
		BigDecimal iva = precioFinal.multiply(ivaPct).divide(CIEN);
		return new MontosProducto(precio, descuento, cargo, iva, precioFinal, pctDescuento, pctCargo);
	}

	default FacturaCalculada calcularFactura(List<Producto> productos) {
		BigDecimal totalBruto = BigDecimal.ZERO;
		BigDecimal totalImponible = BigDecimal.ZERO;
		BigDecimal totalIva = BigDecimal.ZERO;
		BigDecimal totalDescuento = BigDecimal.ZERO;
		BigDecimal totalCargo = BigDecimal.ZERO;
		BigDecimal totalPagar = BigDecimal.ZERO;
		BigDecimal sumaPctDescuento = BigDecimal.ZERO;
		BigDecimal sumaPctCargo = BigDecimal.ZERO;
		if (Objects.nonNull(productos)) {
			for (Producto producto : productos) {
				MontosProducto montos = calcularMontosProducto(producto);
				totalBruto = totalBruto.add(montos.precio());
				totalImponible = totalImponible.add(montos.precioFinal());
				totalIva = totalIva.add(montos.iva());
				totalDescuento = totalDescuento.add(montos.descuento());
				totalCargo = totalCargo.add(montos.cargo());
				totalPagar = totalPagar.add(montos.precioFinal()).add(montos.iva());
				sumaPctDescuento = sumaPctDescuento.add(montos.pctDescuento());
				sumaPctCargo = sumaPctCargo.add(montos.pctCargo());
			}
		}
		BigDecimal ajuste = totalPagar.divide(CIEN, 0, RoundingMode.HALF_UP).multiply(CIEN).subtract(totalPagar);
		return new FacturaCalculada(totalBruto, totalImponible, totalIva, totalDescuento, totalCargo, totalPagar,
				sumaPctDescuento, sumaPctCargo, ajuste);
	}

	default void crearProductoAjusteCentena(RequestFacturaDto factura, BigDecimal monto) {
		Producto plantilla = factura.getProductos().stream()
				.filter(p -> Objects.nonNull(p.getCantidad()) && p.getCantidad().compareTo(BigDecimal.ONE) == 0)
				.findFirst()
				.orElse(factura.getProductos().get(0));
		factura.getProductos().add(Producto.builder()
				.codigoEstandar(plantilla.getCodigoEstandar())
				.precio(monto)
				.cantidad(BigDecimal.ONE)
				.iva(BigDecimal.ZERO)
				.descuento(BigDecimal.ZERO)
				.cargo(BigDecimal.ZERO)
				.nombre(AJUSTE_CENTENA)
				.codigoUnidadMedida(plantilla.getCodigoUnidadMedida())
				.build());
	}

	default Descuento resolverMetaDescuento(RequestFacturaDto factura, boolean esCargo) {
		if (Objects.nonNull(factura.getDescuentos()) && !factura.getDescuentos().isEmpty()) {
			Descuento meta = factura.getDescuentos().stream()
					.filter(item -> Objects.equals(item.getIndCargo(), esCargo))
					.findFirst()
					.orElse(null);
			if (Objects.nonNull(meta) && Objects.nonNull(meta.getCodigoRazon())) {
				return meta;
			}
		}
		return Descuento.builder()
				.indCargo(esCargo)
				.codigoRazon("01")
				.razon(esCargo ? "Cargo" : "Descuento")
				.build();
	}

	default List<DiscountsAndCharges> aplicarDescuento(RequestFacturaDto factura, FacturaCalculada calc, BigDecimal ajuste) {
		List<DiscountsAndCharges> descuentos = new ArrayList<>(0);
		if (Objects.isNull(calc)) {
			return descuentos;
		}
		if (calc.totalCargo().signum() != 0) {
			Descuento meta = resolverMetaDescuento(factura, true);
			descuentos.add(DiscountsAndCharges.builder()
					.isCharge(Boolean.TRUE)
					.reasonCode(meta.getCodigoRazon())
					.percentageAmount(calc.sumaPctCargo().signum() != 0 ? calc.sumaPctCargo() : CIEN)
					.amount(calc.totalCargo())
					.baseAmount(calc.totalBruto())
					.reason(meta.getRazon())
					.build());
		}
		if (calc.totalDescuento().signum() != 0) {
			Descuento meta = resolverMetaDescuento(factura, false);
			descuentos.add(DiscountsAndCharges.builder()
					.isCharge(Boolean.FALSE)
					.reasonCode(meta.getCodigoRazon())
					.percentageAmount(calc.sumaPctDescuento().signum() != 0 ? calc.sumaPctDescuento() : CIEN)
					.amount(calc.totalDescuento())
					.baseAmount(calc.totalBruto())
					.reason(meta.getRazon())
					.build());
		}
		if (ajuste.signum() < 0) {
			BigDecimal monto = ajuste.abs();
			descuentos.add(DiscountsAndCharges.builder()
					.isCharge(Boolean.FALSE)
					.reasonCode("01")
					.percentageAmount(CIEN)
					.amount(monto)
					.baseAmount(monto)
					.reason(AJUSTE_CENTENA)
					.build());
		}
		return descuentos;
	}

	default List<Item> mapProductos(RequestFacturaDto factura) {
		List<Item> articulos = new ArrayList<>(0);
		if (Objects.nonNull(factura) && Objects.nonNull(factura.getProductos()) && !factura.getProductos().isEmpty()) {
			for (Producto producto : factura.getProductos()) {
				MontosProducto montos = calcularMontosProducto(producto);
				Taxe taxe = Taxe.builder().taxCode(TaxTypeEnum.IVA.getCodigo()).taxAmount(montos.iva())
						.taxPercentage(Objects.requireNonNullElse(producto.getIva(), BigDecimal.ZERO).toString())
						.taxableAmount(montos.precioFinal()).build();
				articulos.add(Item.builder()
						.standardCode(StandardCode.builder().id(producto.getCodigoEstandar().getId())
								.identificationId(producto.getCodigoEstandar().getIdIdentificacion()).build())
						.charge(producto.getCargo()).chargeAmount(montos.cargo()).taxes(List.of(taxe))
						.description(producto.getNombre()).price(producto.getPrecio()).discount(producto.getDescuento())
						.discountAmount(montos.descuento()).quantity(producto.getCantidad())
						.unitCode(producto.getCodigoUnidadMedida()).subtotal(montos.precio()).taxAmount(montos.iva())
						.total(montos.precioFinal().add(montos.iva())).build());
			}
		}
		return articulos;
	}

	default TotalAmounts mapTotalAmount(RequestFacturaDto factura, FacturaCalculada calc, BigDecimal ajuste) {
		if (Objects.isNull(calc)) {
			return null;
		}
		BigDecimal deltaPositivo = ajuste.signum() > 0 ? ajuste : BigDecimal.ZERO;
		BigDecimal descuentoTotal = calc.totalDescuento();
		BigDecimal totalPagar = calc.totalPagar().add(ajuste);
		if (ajuste.signum() < 0) {
			descuentoTotal = descuentoTotal.add(ajuste.abs());
		}
		return TotalAmounts.builder()
				.advanceTotal(Objects.requireNonNullElse(factura.getTotalAnticipado(), BigDecimal.ZERO))
				.grossTotal(calc.totalBruto().add(deltaPositivo))
				.taxableTotal(calc.totalImponible().add(deltaPositivo))
				.taxTotal(calc.totalIva())
				.discountTotal(descuentoTotal)
				.chargeTotal(calc.totalCargo())
				.payableTotal(totalPagar)
				.currencyCode(Constantes.COP).build();
	}

	default RequestFacturaDto mapFactura(InvoiceDto factura, ProductEntity productoMc, ProductEntity productoUnidad, Integer iva, String formaPagoCredito, String formaPagoContado, String usuario, List<TarifaConceptoDianDto> tarifas) {
		RequestFacturaDto request =RequestFacturaDto.builder().build();
		request.setId(factura.getId());
		request.setAjusteCentena(true);

		Integer idCliente = null;
		Integer idEmpresa = null;
		if (factura.getFactura() != null && factura.getFactura().getEmpresaClienteContador() != null) {
			var ecc = factura.getFactura().getEmpresaClienteContador();
			if (ecc.getCliente() != null) {
				idCliente = ecc.getCliente().getId();
			}
			if (ecc.getEmpresa() != null) {
				idEmpresa = ecc.getEmpresa().getId();
			}
		}
		if (idCliente == null && factura.getCliente() != null) {
			idCliente = factura.getCliente().getId();
		}
		if (idEmpresa == null && factura.getEmpresa() != null) {
			idEmpresa = factura.getEmpresa().getId();
		}
		request.setIdCliente(idCliente);
		request.setIdEmpresa(idEmpresa);
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
		Pago pago =null;
		Date actual = Utils.obtenerFechaActual();
		if(factura.getFactura().getFechaFin().after(actual)){
			pago =Pago.builder().forma(formaPagoCredito).medio(factura.getFactura().getTipoPago().getCodigoDian()).fechaFin(formatFechaFin(factura.getFactura().getFechaFin())).build();
		}else{
			pago =Pago.builder().forma(formaPagoContado).medio(factura.getFactura().getTipoPago().getCodigoDian()).build();
		}
		request.setMedioPago(pago);
		request.setUsuario(usuario);
		request.setFechaUltimoIntento(new Date());
		request.setFechaEmision(factura.getFactura().getFechaEmision());
		return request;
	}
	default String formatFechaFin(Date fecha) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(fecha);
	}
}
