package com.aqua.plus.api.service.impl.external;

import java.util.Objects;

import com.aqua.plus.api.service.impl.CorreoGeneralServiceImpl;
import com.aqua.plus.api.service.impl.EmpresaClienteContadorServiceImpl;
import com.aqua.plus.api.service.impl.EmpresaServiceImpl;
import com.aqua.plus.api.service.impl.ParametrosEmpresaServiceImpl;
import com.aqua.plus.api.service.impl.PersonaServiceImpl;
import com.aqua.plus.api.service.impl.external.facade.FacturaDianFacade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.aqua.plus.api.config.RestTemplateConfig;
import com.aqua.plus.api.maps.FacturaDianMapper;
import com.aqua.plus.api.service.external.IFacturaDianService;
import com.aqua.plus.api.utils.UtilsRestemplate;
import com.aqua.plus.commons.dtos.EmpresaDTO;
import com.aqua.plus.commons.dtos.PersonaDTO;
import com.aqua.plus.commons.dtos.ResolutionDto;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.RequestFacturaDto;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto;
import com.aqua.plus.commons.dtos.external.RequestSetPruebaDto;
import com.aqua.plus.commons.dtos.external.ResponseInvoiceDto;
import com.aqua.plus.commons.enums.DocumentTypeDianEnum;
import com.aqua.plus.commons.enums.LegalStatusEnum;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import com.aqua.plus.commons.utils.Constantes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacturaDianServiceImpl implements IFacturaDianService {

	@Value("${alegra.url}")
	private String url;

	@Value("${alegra.end-point.factura.invoices}")
	private String endPointFactura;

	@Value("${alegra.end-point.factura.test-sets}")
	private String endPointTestSets;

	@Value("${alegra.end-point.factura.credit-notes}")
	private String endPointNotaCredito;

	private final RestTemplateConfig restTemplateConfig;

	private final UtilsRestemplate utilsRestemplate;

	private final ResolucionDianServiceImpl resolucionDianService;

	private final FacturaDianFacade facturaDianFacade;

	private final PersonaServiceImpl personaService;

	private final CorreoGeneralServiceImpl correoGeneralService;

	private final ParametrosEmpresaServiceImpl parametrosEmpresaService;

	private final EmpresaServiceImpl empresaService;

	private final EmpresaClienteContadorServiceImpl empresaClienteContadorService;

	@Override
	public ResponseEntity<ResponseDTO> crearFacturaElectronica(final RequestFacturaDto request) {
		log.warn("Inicio metodo crearFacturaElectronica: {},{},{} ", request.getIdCliente(), request.getIdEmpresa(),
				request.getProductos().size());
		ResponseEntity<ResponseInvoiceDto> response = null;
		ResolutionDto resolucion = null;
		PersonaDTO persona = null;
		EmpresaDTO empresa = null;
		Long numeroFactura = null;
		RequestInvoiceDto rq = null;
		try {
			resolucion = this.resolucionDianService.getResolucion(request);
			persona = this.personaService.getCliente(request);
			empresa = this.empresaService.getEmpresa(request);
			numeroFactura = this.resolucionDianService.actualizarResolucion(request, resolucion);
			resolucion.setNumeroActual(numeroFactura);
			String direccionContador = this.empresaClienteContadorService.getDireccionContadorCliente(request);
			rq = FacturaDianMapper.INSTANCE.mapDataFacturaEletronica(resolucion, empresa, persona, request,
					this.correoGeneralService.getCorreoPersona(request),
					FacturaDianMapper.INSTANCE.buildDireccionCliente(direccionContador, empresa),
					numeroFactura, this.parametrosEmpresaService.obtenerMesesPeriodo(request.getIdEmpresa()));
			log.warn("DIAN customer armado invoiceId={} idCliente={} name={} address={} city={} dept={}",
					request.getId(), request.getIdCliente(),
					rq.getCustomer() != null ? rq.getCustomer().getName() : null,
					rq.getCustomer() != null && rq.getCustomer().getAddress() != null
							? rq.getCustomer().getAddress().getAddress() : null,
					rq.getCustomer() != null && rq.getCustomer().getAddress() != null
							? rq.getCustomer().getAddress().getCity() : null,
					rq.getCustomer() != null && rq.getCustomer().getAddress() != null
							? rq.getCustomer().getAddress().getDepartment() : null);
			HttpEntity<RequestInvoiceDto> entity = new HttpEntity<>(rq, utilsRestemplate.getHeader());
			print("#############REQUEST ################: {} ", entity.getBody());
			response = this.restTemplateConfig.restTemplate().exchange(this.url.concat(this.endPointFactura),
					HttpMethod.POST, entity, ResponseInvoiceDto.class);
			print("################RESPONSE ############## :{} ", response.getBody());
			log.info("Fin metodo crearFacturaElectronica:{} ", response.getStatusCode());
			String descripcion = obtenerDescripcion(response.getBody());
			this.facturaDianFacade.guardarFactura(request, persona, empresa, numeroFactura, descripcion, response.getBody(), rq);
			if (response.getStatusCode().equals(HttpStatus.CREATED) && Objects.nonNull(response.getBody())
					&& Objects.nonNull(response.getBody().getInvoice())
					&& (response.getBody().getInvoice().getLegalStatus().equals(LegalStatusEnum.ACCEPTED.getCodigo()) || response.getBody().getInvoice().getLegalStatus().equals(LegalStatusEnum.ACCEPTED_WITH_OBSERVATIONS.getCodigo()))) {
				return new ResponseEntity<>(ResponseDTO.builder().code(HttpStatus.CREATED.value())
						.message(descripcion).response(response.getBody()).build(), HttpStatus.CREATED);
			} else {
				log.error("Reponse crearFacturaElectronica: {} ", response);
				return new ResponseEntity<>(
						ResponseDTO.builder().code(HttpStatus.CONFLICT.value()).message(descripcion).build(),
						HttpStatus.CONFLICT);
			}
		} catch (HttpClientErrorException.BadRequest ex) {
			log.error(ex.getLocalizedMessage());
			ResponseInvoiceDto error = getResponse(ex.getResponseBodyAsString());
			log.info("################RESPONSE ############## {}", ex.getResponseBodyAsString());
			String descripcion = obtenerDescripcion(error);
			this.facturaDianFacade.guardarFactura(request, persona, empresa, numeroFactura, descripcion, error, rq);
			log.info("error: {} ", error);
			return new ResponseEntity<>(
					ResponseDTO.builder().code(HttpStatus.BAD_REQUEST.value()).message(descripcion).build(),
					HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return new ResponseEntity<>(
					ResponseDTO.builder().code(HttpStatus.CONFLICT.value())
							.message(Constantes.ER_CONSUME_SERVICE_DIAN.concat(this.endPointFactura)).build(),
					HttpStatus.CONFLICT);
		}
	}

	private ResponseInvoiceDto getResponse(String descripcion) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(descripcion, ResponseInvoiceDto.class);
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
		}
		return null;
	}

	private String obtenerDescripcion(ResponseInvoiceDto response) {
		log.info("Inicio metodo obtenerDescripcion");
		if (Objects.nonNull(response) && Objects.nonNull(response.getInvoice())
				&& Objects.nonNull(response.getInvoice().getGovernmentResponse())) {
			log.info("Fin metodo obtenerDescripcion:{} ", response.getInvoice().getGovernmentResponse().getMessage());
			return response.getInvoice().getGovernmentResponse().getMessage().concat(" ")
					.concat(Objects.nonNull(response.getInvoice().getGovernmentResponse().getErrorMessages())
							&& !response.getInvoice().getGovernmentResponse().getErrorMessages().isEmpty()
									? response.getInvoice().getGovernmentResponse().getErrorMessages().get(0)
									: "");
		} else if (Objects.nonNull(response) && Objects.nonNull(response.getErrors())
				&& !response.getErrors().isEmpty()) {
			log.info("Fin metodo obtenerDescripcion:{} ", response.getErrors().get(0).getMessage());
			return response.getErrors().get(0).getMessage();
		} else {
			log.info("Fin metodo obtenerDescripcion:{} ",
					Constantes.ER_CONSUME_SERVICE_DIAN.concat(this.endPointFactura));
			return Constantes.ER_CONSUME_SERVICE_DIAN.concat(this.endPointFactura);
		}
	}
	
	private String obtenerDescripcionNotaCredito(ResponseInvoiceDto response) {
		log.info("Inicio metodo obtenerDescripcionNotaCredito");
		if (Objects.nonNull(response) && Objects.nonNull(response.getCreditNote())
				&& Objects.nonNull(response.getCreditNote().getGovernmentResponse())) {
			log.info("Fin metodo obtenerDescripcionNotaCredito:{} ", response.getCreditNote().getGovernmentResponse().getMessage());
			return response.getCreditNote().getGovernmentResponse().getMessage().concat(" ")
					.concat(Objects.nonNull(response.getCreditNote().getGovernmentResponse().getErrorMessages())
							&& !response.getCreditNote().getGovernmentResponse().getErrorMessages().isEmpty()
									? response.getCreditNote().getGovernmentResponse().getErrorMessages().get(0)
									: "");
		} else if (Objects.nonNull(response) && Objects.nonNull(response.getErrors())
				&& !response.getErrors().isEmpty()) {
			log.info("Fin metodo obtenerDescripcionNotaCredito:{} ", response.getErrors().get(0).getMessage());
			return response.getErrors().get(0).getMessage();
		} else {
			log.info("Fin metodo obtenerDescripcionNotaCredito:{} ",
					Constantes.ER_CONSUME_SERVICE_DIAN.concat(this.endPointNotaCredito));
			return Constantes.ER_CONSUME_SERVICE_DIAN.concat(this.endPointNotaCredito);
		}
	}


	@Override
	public ResponseEntity<ResponseDTO> crearSetPrueba(RequestSetPruebaDto request) {
		log.info("Inicio metodo crearSetPrueba: {} ", request.getGovernmentId());
		HttpEntity<RequestSetPruebaDto> entity = new HttpEntity<>(request, utilsRestemplate.getHeader());
		FacturaDianServiceImpl.print("##########REQUEST########## ", entity);
		ResponseEntity<Object> response = this.restTemplateConfig.restTemplate()
				.exchange(this.url.concat(this.endPointTestSets), HttpMethod.POST, entity, Object.class);
		FacturaDianServiceImpl.print("##########RESPONSE########## ", response);
		log.info("Fin metodo crearSetPrueba:{} ", response.getStatusCode());
		if (response.getStatusCode().equals(HttpStatus.CREATED)) {
			return new ResponseEntity<>(ResponseDTO.builder().code(HttpStatus.CREATED.value())
					.message(HttpStatus.CREATED.name()).response(response.getBody()).build(), HttpStatus.CREATED);
		} else {
			log.error("Reponse crearSetPrueba: {} ", response);
		}
		return new ResponseEntity<>(
				ResponseDTO.builder().code(HttpStatus.CONFLICT.value())
						.message(Constantes.ER_CONSUME_SERVICE_DIAN.concat(this.endPointTestSets)).build(),
				HttpStatus.CONFLICT);
	}

	public static void print(String nombre, Object objeto) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			log.warn(nombre, mapper.writeValueAsString(objeto));
		} catch (JsonProcessingException e) {
			log.error(e.getLocalizedMessage());
		}
	}

	@Override
	public ResponseEntity<ResponseDTO> consultarDocumentoPorEmpresa(String idEmpresaDian, String tipo) {
		log.info("Inicio metodo consultarDocumentoPorEmpresa: {},{} ", idEmpresaDian, tipo);
		HttpEntity<Void> entity = new HttpEntity<>(utilsRestemplate.getHeader());
		String url = this.url.concat(endPointFactura).concat("/").concat(idEmpresaDian).concat("/").concat("files")
				.concat("/").concat(tipo);
		log.info("URL: {} ", url);
		ResponseEntity<Object> response = this.restTemplateConfig.restTemplate().exchange(url, HttpMethod.GET, entity,
				Object.class);
		log.info("Fin metodo consultarDocumentoPorEmpresa:{} ", response.getStatusCode());
		if (response.getStatusCode().equals(HttpStatus.OK) && Objects.nonNull(response.getBody())) {
			return new ResponseEntity<>(ResponseDTO.builder().code(HttpStatus.OK.value())
					.message(HttpStatus.OK.name()).response(response.getBody()).build(), HttpStatus.OK);
		} else {
			log.error("Reponse consultarDocumentoPorEmpresa: {} ", response);
		}
		return new ResponseEntity<>(ResponseDTO.builder().code(HttpStatus.CONFLICT.value())
				.message(Constantes.ER_CONSUME_SERVICE_DIAN.concat(url)).build(), HttpStatus.CONFLICT);
	}

	public ResponseEntity<ResponseDTO> crearNotaCredito(final RequestInvoiceDto rq) {
		log.info("Inicio metodo crearNotaCredito: {},{},{} ", rq.getIdCliente(), rq.getIdEmpresa(),
				rq.getItems().size());
		ResponseEntity<ResponseInvoiceDto> response = null;
		ResolutionDto resolucion = null;
		Long numeroFactura = null;
		RequestFacturaDto requestFactura = RequestFacturaDto.builder().id(rq.getId()).fechaUltimoIntento(rq.getFechaUltimoIntento())
				.usuario(rq.getUsuario()).idEmpresa(rq.getIdEmpresa()).build();
		try {
			resolucion = this.resolucionDianService.getResolucion(requestFactura);
			numeroFactura = this.resolucionDianService.actualizarResolucion(requestFactura, resolucion);
			rq.setNumber(numeroFactura);
			rq.setDocumentType(DocumentTypeDianEnum.NOTA_CREDITO.getCodigo());
			HttpEntity<RequestInvoiceDto> entity = new HttpEntity<>(rq, utilsRestemplate.getHeader());
			print("#############REQUEST ################: {} ", entity.getBody());
			response = this.restTemplateConfig.restTemplate().exchange(this.url.concat(this.endPointNotaCredito),
					HttpMethod.POST, entity, ResponseInvoiceDto.class);
			print("################RESPONSE ############## ", response.getBody());
			log.info("Fin metodo crearNotaCredito:{} ", response.getStatusCode());
			String descripcion = obtenerDescripcionNotaCredito(response.getBody());
			this.facturaDianFacade.guardarFactura(requestFactura, PersonaDTO.builder().id(rq.getIdCliente()).build(),
					EmpresaDTO.builder().id(rq.getIdEmpresa()).build(), numeroFactura, descripcion, response.getBody(),
					rq);
			if (response.getStatusCode().equals(HttpStatus.CREATED) && Objects.nonNull(response.getBody())
					&& Objects.nonNull(response.getBody().getCreditNote())
					&& (response.getBody().getCreditNote().getLegalStatus().equals(LegalStatusEnum.ACCEPTED.getCodigo()) || response.getBody().getCreditNote().getLegalStatus().equals(LegalStatusEnum.ACCEPTED_WITH_OBSERVATIONS.getCodigo()))) {
				return new ResponseEntity<>(ResponseDTO.builder().code(HttpStatus.CREATED.value())
						.message(descripcion).response(response.getBody()).build(), HttpStatus.CREATED);
			} else {
				log.error("Reponse crearNotaCredito: {} ", response);
				return new ResponseEntity<>(
						ResponseDTO.builder().code(HttpStatus.CONFLICT.value()).message(descripcion).build(),
						HttpStatus.CONFLICT);
			}
		} catch (HttpClientErrorException.BadRequest ex) {
			log.error(ex.getLocalizedMessage());
			ResponseInvoiceDto error = getResponse(ex.getResponseBodyAsString());
			log.info("################RESPONSE ############## {}", ex.getResponseBodyAsString());
			String descripcion = obtenerDescripcionNotaCredito(error);
			this.facturaDianFacade.guardarFactura(requestFactura, PersonaDTO.builder().id(rq.getIdCliente()).build(),
					EmpresaDTO.builder().id(rq.getIdEmpresa()).build(), numeroFactura, descripcion, error, rq);
			log.info("error: {} ", error);
			return new ResponseEntity<>(
					ResponseDTO.builder().code(HttpStatus.BAD_REQUEST.value()).message(descripcion).build(),
					HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return new ResponseEntity<>(
					ResponseDTO.builder().code(HttpStatus.CONFLICT.value())
							.message(Constantes.ER_CONSUME_SERVICE_DIAN.concat(this.endPointNotaCredito)).build(),
					HttpStatus.CONFLICT);
		}
	}
}
