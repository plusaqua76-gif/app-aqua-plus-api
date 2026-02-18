package com.aqua.plus.api.service.impl.external;

import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import com.aqua.plus.api.config.RestTemplateConfig;
import com.aqua.plus.api.maps.FacturaDianMapper;
import com.aqua.plus.api.service.external.IFacturaDianService;
import com.aqua.plus.api.utils.EncriptarDesencriptar;
import com.aqua.plus.api.utils.Utils;
import com.aqua.plus.api.utils.UtilsRestemplate;
import com.aqua.plus.commons.dtos.CorreoGeneralDTO;
import com.aqua.plus.commons.dtos.EmpresaDTO;
import com.aqua.plus.commons.dtos.InvoiceDto;
import com.aqua.plus.commons.dtos.PersonaDTO;
import com.aqua.plus.commons.dtos.ResolutionDto;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.RequestFacturaDto;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto;
import com.aqua.plus.commons.dtos.external.RequestSetPruebaDto;
import com.aqua.plus.commons.dtos.external.ResponseInvoiceDto;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto.TaxCode;
import com.aqua.plus.commons.entities.InvoiceEntity;
import com.aqua.plus.commons.entities.ParametrosEmpresaEntity;
import com.aqua.plus.commons.entities.ResolutionEntity;
import com.aqua.plus.commons.enums.DocumentTypeDianEnum;
import com.aqua.plus.commons.enums.LegalStatusEnum;
import com.aqua.plus.commons.enums.ParametroEmpresaEnum;
import com.aqua.plus.commons.enums.TaxTypeEnum;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import com.aqua.plus.commons.maps.CorreoGeneralMapper;
import com.aqua.plus.commons.maps.EmpresaMapper;
import com.aqua.plus.commons.maps.InvoiceMapper;
import com.aqua.plus.commons.maps.PersonaMapper;
import com.aqua.plus.commons.maps.ResolucionMapper;
import com.aqua.plus.commons.repositories.CorreoGeneralRepository;
import com.aqua.plus.commons.repositories.EmpresaRepository;
import com.aqua.plus.commons.repositories.InvoiceRepository;
import com.aqua.plus.commons.repositories.ParametrosEmpresaRepository;
import com.aqua.plus.commons.repositories.PersonaRepository;
import com.aqua.plus.commons.repositories.ResolutionRepository;
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

	private final EmpresaRepository empresaRepository;

	private final PersonaRepository personaRepository;

	private final ResolutionRepository resolutionRepository;

	private final PersonaMapper personaMapper;

	private final CorreoGeneralMapper correoGeneralMapper;

	private final CorreoGeneralRepository correoGeneralRepository;

	private final InvoiceRepository facturaRepository;

	private final EncriptarDesencriptar encriptarDesencriptar;

	private final ParametrosEmpresaRepository parametrosEmpresaRepository;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> crearFacturaElectronica(final RequestFacturaDto request) {
		log.info("Inicio metodo crearFacturaElectronica: {},{},{} ", request.getIdCliente(), request.getIdEmpresa(),
				request.getProductos().size());
		ResponseEntity<ResponseInvoiceDto> response = null;
		ResolutionDto resolucion = null;
		PersonaDTO persona = null;
		EmpresaDTO empresa = null;
		Long numeroFactura = null;
		RequestInvoiceDto rq = null;
		try {
			resolucion = getResolucion(request);
			persona = getCliente(request);
			empresa = getEmpresa(request);
			numeroFactura = actualizarResolucion(request, resolucion);
			rq = FacturaDianMapper.INSTANCE.mapDataFacturaEletronica(getResolucion(request), empresa, persona, request,
					getCorreoPersona(request), numeroFactura, obtenerMesesPeriodo(request.getIdEmpresa()));
			HttpEntity<RequestInvoiceDto> entity = new HttpEntity<>(rq, utilsRestemplate.getHeader());
			print("#############REQUEST ################3: {} ", entity.getBody());
			response = this.restTemplateConfig.restTemplate().exchange(this.url.concat(this.endPointFactura),
					HttpMethod.POST, entity, ResponseInvoiceDto.class);
			print("################RESPONSE ############## ", response.getBody());
			log.info("Fin metodo crearFacturaElectronica:{} ", response.getStatusCode());
			String descripcion = obtenerDescripcion(response.getBody());
			guardarFactura(request, resolucion, persona, empresa, numeroFactura, descripcion, response.getBody(), rq);
			if (response.getStatusCode().equals(HttpStatus.CREATED) && Objects.nonNull(response.getBody())
					&& Objects.nonNull(response.getBody().getInvoice())
					&& response.getBody().getInvoice().getLegalStatus().equals(LegalStatusEnum.ACCEPTED.getCodigo())) {
				return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.CREATED.value())
						.message(descripcion).response(response.getBody()).build(), HttpStatus.CREATED);
			} else {
				log.error("Reponse crearFacturaElectronica: {} ", response);
				return new ResponseEntity<ResponseDTO>(
						ResponseDTO.builder().code(HttpStatus.CONFLICT.value()).message(descripcion).build(),
						HttpStatus.CONFLICT);
			}
		} catch (HttpClientErrorException.BadRequest ex) {
			log.error(ex.getLocalizedMessage());
			ResponseInvoiceDto error = getResponse(ex.getResponseBodyAsString());
			log.info("################RESPONSE ############## {}", ex.getResponseBodyAsString());
			String descripcion = obtenerDescripcion(error);
			guardarFactura(request, resolucion, persona, empresa, numeroFactura, descripcion, error, rq);
			log.info("error: {} ", error);
			return new ResponseEntity<ResponseDTO>(
					ResponseDTO.builder().code(HttpStatus.BAD_REQUEST.value()).message(descripcion).build(),
					HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return new ResponseEntity<ResponseDTO>(
					ResponseDTO.builder().code(HttpStatus.CONFLICT.value())
							.message(Constantes.ER_CONSUME_SERVICE_DIAN.concat(this.endPointFactura)).build(),
					HttpStatus.CONFLICT);
		}
	}

	private Integer obtenerMesesPeriodo(Integer idEmpresa) {
		ParametrosEmpresaEntity entity = this.parametrosEmpresaRepository
				.findFirstByEmpresa_IdAndLlaveAndActivoTrue(idEmpresa, ParametroEmpresaEnum.PERIODOS_FACT.getCodigo())
				.orElseThrow(() -> new ProcessGenericException(Constantes.PARAM_NOT_FOUND));
		return Integer.parseInt(entity.getValorParametro());
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

	@Transactional
	public ResolutionEntity obtenerConBloqueo(Integer id) {
		return this.resolutionRepository.findByIdForUpdate(id);
	}

	@Transactional
	public Long actualizarResolucion(final RequestFacturaDto request, final ResolutionDto resolucion) {
		log.info("Inicio metodo actualizarResolucion:{} ", request.getIdEmpresa());
		ResolutionEntity entityResolucion = obtenerConBloqueo(resolucion.getId());

		if (entityResolucion.getNumeroActual() >= entityResolucion.getNumeroMaximo()) {
			throw new ProcessGenericException(Constantes.RANGE_DIAN_EXHAUSTED);
		}

		Long siguiente = entityResolucion.getNumeroActual() + 1;
		entityResolucion.setNumeroActual(siguiente);
		resolutionRepository.save(entityResolucion);
		log.info("Fin metodo actualizarResolucion:{},{} ", request.getIdEmpresa(), siguiente);

		return siguiente;
	}

	@Transactional
	public void guardarFactura(final RequestFacturaDto request, final ResolutionDto resolucion, PersonaDTO persona,
			EmpresaDTO empresa, Long numeroFactura, String descripcion, ResponseInvoiceDto response,
			RequestInvoiceDto rq) {
		log.info("Inicio metodo guardarFactura:{} ", numeroFactura);

		InvoiceDto invoiceDto = new InvoiceDto();
		invoiceDto.setId(request.getId());
		if (Objects.nonNull(response) && Objects.nonNull(response.getInvoice())) {
			invoiceDto.setEstado(response.getInvoice().getStatus());
			invoiceDto.setEstadoLegal(response.getInvoice().getLegalStatus());
			invoiceDto.setIdDian(response.getInvoice().getId());
			invoiceDto.setCufe(response.getInvoice().getCufe());
		}
		if (Objects.nonNull(response) && Objects.nonNull(response.getCreditNote())) {
			invoiceDto.setEstado(response.getCreditNote().getStatus());
			invoiceDto.setEstadoLegal(response.getCreditNote().getLegalStatus());
			invoiceDto.setIdDian(response.getCreditNote().getId());
			invoiceDto.setCufe(response.getCreditNote().getCufe());
		}
		invoiceDto.setCliente(persona);
		invoiceDto.setEmpresa(empresa);
		invoiceDto.setActivo(Boolean.TRUE);
		invoiceDto.setNumero(numeroFactura);
		invoiceDto.setDescripcion(descripcion);
		if(Objects.nonNull(request.getId())) {
			invoiceDto.setUsuarioModificacion(request.getUsuario());
		}else {
			invoiceDto.setUsuarioCreacion(request.getUsuario());
		}
		invoiceDto.setFechaUltimoIntento(request.getFechaUltimoIntento());
		rq.setResolution(null);
		invoiceDto.setData(this.encriptarDesencriptar.encriptar(Utils.objectToBase64(rq)));
		facturaRepository.save(InvoiceMapper.INSTANCE.dtoToEntity(invoiceDto));
		log.info("Fin metodo guardarFactura:{} ", numeroFactura);
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
			return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.CREATED.value())
					.message(HttpStatus.CREATED.name()).response(response.getBody()).build(), HttpStatus.CREATED);
		} else {
			log.error("Reponse crearSetPrueba: {} ", response);
		}
		return new ResponseEntity<ResponseDTO>(
				ResponseDTO.builder().code(HttpStatus.CONFLICT.value())
						.message(Constantes.ER_CONSUME_SERVICE_DIAN.concat(this.endPointTestSets)).build(),
				HttpStatus.CONFLICT);
	}

	private EmpresaDTO getEmpresa(final RequestFacturaDto request) {
		return EmpresaMapper.INSTANCE.entityToDto(this.empresaRepository.findById(request.getIdEmpresa())
				.orElseThrow(() -> new ProcessGenericException(Constantes.EMP_NOT_FOUND)));
	}

	private PersonaDTO getCliente(final RequestFacturaDto request) {
		return personaMapper.entityToDto(this.personaRepository.findById(request.getIdCliente())
				.orElseThrow(() -> new ProcessGenericException(Constantes.CLIENT_NOT_FOUND)));
	}

	private ResolutionDto getResolucion(final RequestFacturaDto request) {
		return ResolucionMapper.INSTANCE.entityToDto(this.resolutionRepository.findByEmpresaId(request.getIdEmpresa())
				.orElseThrow(() -> new ProcessGenericException(Constantes.RESOLUTION_NOT_FOUND)));
	}

	private CorreoGeneralDTO getCorreoPersona(final RequestFacturaDto request) {
		return correoGeneralMapper
				.entityToDto(this.correoGeneralRepository.findByPersonaIdAndActivoTrue(request.getIdCliente())
						.orElseThrow(() -> new ProcessGenericException(Constantes.EMAIL_NOT_FOUND)));
	}

	public static void print(String nombre, Object objeto) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			log.info(nombre, mapper.writeValueAsString(objeto));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public ResponseEntity<ResponseDTO> consultarFacturasPorEmpresa(Integer idEmpresa, Pageable pageable) {
		log.info("Inicio metodo consultarFacturasPorEmpresa:{},{},{}", idEmpresa, pageable.getPageSize(),
				pageable.getPageNumber());

		Page<InvoiceEntity> page = this.facturaRepository.findByEmpresaId(idEmpresa, pageable);

		log.info("Fin metodo consultarFacturasPorEmpresa:{}, totalElements:{}, totalPages:{}", idEmpresa,
				page.getTotalElements(), page.getTotalPages());

		if (page.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ResponseDTO.builder().success(false).code(HttpStatus.NOT_FOUND.value())
							.message("No se encontraron facturas DIAN para la empresa con id " + idEmpresa)
							.response(List.of()).totalCount(page.getTotalElements()).pageSize(page.getSize())
							.currentPage(page.getNumber()).totalPages(page.getTotalPages()).build());
		}

		return ResponseEntity.ok(ResponseDTO.builder().success(true).code(HttpStatus.OK.value())
				.message(Constantes.CONSULTED_SUCCESSFULLY)
				.response(InvoiceMapper.INSTANCE.listEntityToDtoList(page.getContent()))
				.totalCount(page.getTotalElements()).pageSize(page.getSize()).currentPage(page.getNumber())
				.totalPages(page.getTotalPages()).build());
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
			return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.OK.value())
					.message(HttpStatus.OK.name()).response(response.getBody()).build(), HttpStatus.OK);
		} else {
			log.error("Reponse consultarDocumentoPorEmpresa: {} ", response);
		}
		return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.CONFLICT.value())
				.message(Constantes.ER_CONSUME_SERVICE_DIAN.concat(url)).build(), HttpStatus.CONFLICT);
	}

	@Transactional
	public ResponseEntity<ResponseDTO> actualizarEstadoFactura(Long idEmpresa, String estadoActual, String nuevoEstado,
			String usuario) {
		log.info("Inicio metodo actualizarEstadoFactura: {},{},{} ", idEmpresa, estadoActual, nuevoEstado);
		int canditadAfectado = this.facturaRepository.actualizarEstadoPorEmpresa(idEmpresa, estadoActual, nuevoEstado,
				usuario);
		if (canditadAfectado > 0) {
			log.info("Fin metodo actualizarEstadoFactura: {},{},{},{} ", idEmpresa, estadoActual, nuevoEstado,
					Constantes.UPDATED_SUCCESSFULLY);
			return new ResponseEntity<ResponseDTO>(
					ResponseDTO.builder().code(HttpStatus.OK.value()).message(Constantes.UPDATED_SUCCESSFULLY).build(),
					HttpStatus.OK);
		} else {
			log.info("Fin metodo actualizarEstadoFactura: {},{},{},{} ", idEmpresa, estadoActual, nuevoEstado,
					Constantes.NOT_UPDATED_INVOICE);
			return new ResponseEntity<ResponseDTO>(
					ResponseDTO.builder().code(HttpStatus.OK.value()).message(Constantes.NOT_UPDATED_INVOICE).build(),
					HttpStatus.OK);
		}
	}

	@Transactional
	public ResponseEntity<ResponseDTO> crearNotaCredito(final RequestInvoiceDto rq) {
		log.info("Inicio metodo crearNotaCredito: {},{},{} ", rq.getIdCliente(), rq.getIdEmpresa(),
				rq.getItems().size());
		ResponseEntity<ResponseInvoiceDto> response = null;
		ResolutionDto resolucion = null;
		Long numeroFactura = null;
		RequestFacturaDto requestFactura = RequestFacturaDto.builder().id(rq.getId()).fechaUltimoIntento(rq.getFechaUltimoIntento())
				.usuario(rq.getUsuario()).idEmpresa(rq.getIdEmpresa()).build();
		try {
			resolucion = getResolucion(requestFactura);
			numeroFactura = actualizarResolucion(requestFactura, resolucion);
			rq.setNumber(numeroFactura);
			rq.setDocumentType(DocumentTypeDianEnum.NOTA_CREDITO.getCodigo());
			rq.getCompany().setTaxCode(TaxCode.builder().id(TaxTypeEnum.IVA.getCodigo()).build());
			HttpEntity<RequestInvoiceDto> entity = new HttpEntity<>(rq, utilsRestemplate.getHeader());
			print("#############REQUEST ################: {} ", entity.getBody());
			response = this.restTemplateConfig.restTemplate().exchange(this.url.concat(this.endPointNotaCredito),
					HttpMethod.POST, entity, ResponseInvoiceDto.class);
			print("################RESPONSE ############## ", response.getBody());
			log.info("Fin metodo crearNotaCredito:{} ", response.getStatusCode());
			String descripcion = obtenerDescripcionNotaCredito(response.getBody());
			guardarFactura(requestFactura, resolucion, PersonaDTO.builder().id(rq.getIdCliente()).build(),
					EmpresaDTO.builder().id(rq.getIdEmpresa()).build(), numeroFactura, descripcion, response.getBody(),
					rq);
			if (response.getStatusCode().equals(HttpStatus.CREATED) && Objects.nonNull(response.getBody())
					&& Objects.nonNull(response.getBody().getCreditNote())
					&& response.getBody().getCreditNote().getLegalStatus().equals(LegalStatusEnum.ACCEPTED.getCodigo())) {
				return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.CREATED.value())
						.message(descripcion).response(response.getBody()).build(), HttpStatus.CREATED);
			} else {
				log.error("Reponse crearNotaCredito: {} ", response);
				return new ResponseEntity<ResponseDTO>(
						ResponseDTO.builder().code(HttpStatus.CONFLICT.value()).message(descripcion).build(),
						HttpStatus.CONFLICT);
			}
		} catch (HttpClientErrorException.BadRequest ex) {
			log.error(ex.getLocalizedMessage());
			ResponseInvoiceDto error = getResponse(ex.getResponseBodyAsString());
			log.info("################RESPONSE ############## {}", ex.getResponseBodyAsString());
			String descripcion = obtenerDescripcionNotaCredito(error);
			guardarFactura(requestFactura, resolucion, PersonaDTO.builder().id(rq.getIdCliente()).build(),
					EmpresaDTO.builder().id(rq.getIdEmpresa()).build(), numeroFactura, descripcion, error, rq);
			log.info("error: {} ", error);
			return new ResponseEntity<ResponseDTO>(
					ResponseDTO.builder().code(HttpStatus.BAD_REQUEST.value()).message(descripcion).build(),
					HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return new ResponseEntity<ResponseDTO>(
					ResponseDTO.builder().code(HttpStatus.CONFLICT.value())
							.message(Constantes.ER_CONSUME_SERVICE_DIAN.concat(this.endPointNotaCredito)).build(),
					HttpStatus.CONFLICT);
		}
	}
	
	public ResponseEntity<ResponseDTO> consultarDataEnviaDian(final Integer id){
		InvoiceEntity invoice = this.facturaRepository.findById(id).orElseThrow(() -> new ProcessGenericException(Constantes.FAC_NOT_FOUND));
		
		return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.OK.value())
				.message(HttpStatus.OK.name()).response(Utils.base64ToObject(encriptarDesencriptar.desencriptar(invoice.getData()))).build(), HttpStatus.OK);
	}
}
