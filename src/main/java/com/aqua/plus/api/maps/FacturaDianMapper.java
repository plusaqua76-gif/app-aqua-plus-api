package com.aqua.plus.api.maps;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import com.aqua.plus.commons.dtos.CorreoGeneralDTO;
import com.aqua.plus.commons.dtos.EmpresaDTO;
import com.aqua.plus.commons.dtos.PersonaDTO;
import com.aqua.plus.commons.dtos.ResolutionDto;
import com.aqua.plus.commons.dtos.external.RequestFacturaDto;
import com.aqua.plus.commons.dtos.external.RequestFacturaDto.Producto;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.Resolution;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.StandardCode;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.Taxe;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.TotalAmounts;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.Company;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.Customer;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.Item;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.Payment;
import com.aqua.plus.commons.enums.DocumentTypeDianEnum;
import com.aqua.plus.commons.enums.OrganizationTypeDianEnum;
import com.aqua.plus.commons.enums.StandardCodeEnum;
import com.aqua.plus.commons.enums.TaxTypeEnum;
import com.aqua.plus.commons.enums.TypeDocumentDianEnum;
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
	@Mapping(target = "organizationType", expression = "java(getTypeOrganizationCustomer())")
	@Mapping(target = "identificationType", source = "tipoDocumento.idTipoDian")
	@Mapping(target = "identificationNumber", source = "numeroCedula")
	@Mapping(target = "email", source = "correo")
	Customer clienteDtoToCustomerDian(PersonaDTO persona);

	@Named("getTypeOrganization")
	default Integer getTypeOrganization() {
		return OrganizationTypeDianEnum.PERSONA_JURIDICA.getId();
	}

	@Named("getTypeOrganizationCustomer")
	default Integer getTypeOrganizationCustomer() {
		return OrganizationTypeDianEnum.PERSONA_NATURAL.getId();
	}

	@Named("getTypeDocument")
	default Integer getTypeDocument() {
		return TypeDocumentDianEnum.NIT.getId();
	}

	default RequestInvoiceDto mapDataFacturaEletronica(ResolutionDto resolucionDto, EmpresaDTO empresa,
			PersonaDTO persona, RequestFacturaDto factura, CorreoGeneralDTO correoPersona) {
		RequestInvoiceDto rq = new RequestInvoiceDto();
		rq.setDocumentType(DocumentTypeDianEnum.ESTANDAR.getCodigo());
		rq.setResolution(resolucionDtoToResolucionDian(resolucionDto));
		rq.setCompany(empresaDtoToCompanyDian(empresa));
		persona.setCorreo(correoPersona.getCorreo());
		rq.setCustomer(clienteDtoToCustomerDian(persona));
		List<Payment> mediosPagos = new ArrayList<>(0);
		mediosPagos.add(Payment.builder().paymentForm(factura.getMedioPago().getForma())
				.paymentMethod(factura.getMedioPago().getMedio()).build());
		rq.setPayments(mediosPagos);
		rq.setItems(mapProductos(factura));
		rq.setTotalAmounts(mapTotalAmount(factura));
		rq.setNumber(13);
		return rq;
	}

	default List<Item> mapProductos(RequestFacturaDto factura) {
		List<Item> articulos = new ArrayList<>(0);
		if (Objects.nonNull(factura) && Objects.nonNull(factura.getProductos()) && !factura.getProductos().isEmpty()) {
			for (Producto producto : factura.getProductos()) {
				List<Taxe> taxes = new ArrayList<>(0);
				BigDecimal precio = producto.getPrecio().multiply(producto.getCantidad());
				BigDecimal iva = precio.multiply(producto.getIva()).divide(BigDecimal.valueOf(100));
				BigDecimal descuento = precio.multiply(producto.getDescuento()).divide(BigDecimal.valueOf(100));
				BigDecimal cargoDescuento = precio.multiply(producto.getCargo()).divide(BigDecimal.valueOf(100));
				Taxe taxe = Taxe.builder().taxCode(TaxTypeEnum.IVA.getCodigo()).taxAmount(iva)
						.taxPercentage(producto.getIva().toString()).taxableAmount(precio).build();
				taxes.add(taxe);
				Item item = Item.builder().standardCode(StandardCode.builder().id(StandardCodeEnum.CERO_CERO_UNO.getCodigo()).identificationId(StandardCodeEnum.CERO_CERO_UNO.getCodigo()).build()).charge(producto.getCargo()).chargeAmount(cargoDescuento).taxes(taxes)
						.description(producto.getNombre()).price(producto.getPrecio()).discount(producto.getDescuento())
						.discountAmount(descuento).quantity(producto.getCantidad())
						.unitCode(producto.getCodigoUnidadMedida()).subtotal(precio).taxAmount(iva)
						.total(precio.add(iva).subtract(cargoDescuento).subtract(descuento)).build();
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
				BigDecimal iva = precio.multiply(producto.getIva()).divide(BigDecimal.valueOf(100));
				BigDecimal descuento = precio.multiply(producto.getDescuento()).divide(BigDecimal.valueOf(100));
				BigDecimal cargoDescuento = precio.multiply(producto.getCargo()).divide(BigDecimal.valueOf(100));
				totalBruto = totalBruto.add(precio);
				totalImponible =  totalImponible.add(precio);
				totalImpuesto =  totalImpuesto.add(iva);
				descuentoTotal =  descuentoTotal.add(descuento);
				cargoTotal =  cargoTotal.add(cargoDescuento);
				totalPagar = totalPagar.add(precio).add(iva).subtract(descuento).subtract(cargoDescuento);
			}
			total = TotalAmounts.builder().advanceTotal(factura.getTotalAnticipado()).grossTotal(totalBruto)
					.taxableTotal(totalImponible).taxTotal(totalImpuesto).discountTotal(descuentoTotal)
					.chargeTotal(cargoTotal).advanceTotal(totalAnticipado).payableTotal(totalPagar)
					.currencyCode(Constantes.COP).build();
		}

		return total;
	}
}
