package com.aqua.plus.api.service.impl.external;

import java.util.List;
import java.util.Objects;

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
import com.aqua.plus.api.utils.UtilsRestemplate;
import com.aqua.plus.commons.dtos.CorreoGeneralDTO;
import com.aqua.plus.commons.dtos.EmpresaDTO;
import com.aqua.plus.commons.dtos.InvoiceDto;
import com.aqua.plus.commons.dtos.PersonaDTO;
import com.aqua.plus.commons.dtos.ResolutionDto;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.CompanyDto;
import com.aqua.plus.commons.dtos.external.RequestFacturaDto;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto;
import com.aqua.plus.commons.dtos.external.RequestSetPruebaDto;
import com.aqua.plus.commons.dtos.external.ResponseConsultaEmpresaDto;
import com.aqua.plus.commons.dtos.external.ResponseInvoiceDto;
import com.aqua.plus.commons.entities.InvoiceEntity;
import com.aqua.plus.commons.entities.ResolutionEntity;
import com.aqua.plus.commons.enums.LegalStatusEnum;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import com.aqua.plus.commons.maps.CorreoGeneralMapper;
import com.aqua.plus.commons.maps.EmpresaMapper;
import com.aqua.plus.commons.maps.InvoiceMapper;
import com.aqua.plus.commons.maps.PersonaMapper;
import com.aqua.plus.commons.maps.ResolucionMapper;
import com.aqua.plus.commons.repositories.CorreoGeneralRepository;
import com.aqua.plus.commons.repositories.EmpresaRepository;
import com.aqua.plus.commons.repositories.InvoiceRepository;
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

	private final RestTemplateConfig restTemplateConfig;
	
	private final UtilsRestemplate utilsRestemplate;
	
	private final EmpresaRepository empresaRepository;
	
	private final PersonaRepository personaRepository;
	
	private final ResolutionRepository resolutionRepository;
	
	private final PersonaMapper personaMapper;
	
	private final CorreoGeneralMapper correoGeneralMapper;
	
	private final CorreoGeneralRepository correoGeneralRepository;
	
	private final InvoiceRepository facturaRepository;
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> crearFacturaElectronica(final RequestFacturaDto request) {
		log.info("Inicio metodo crearFacturaElectronica: {},{},{} " , request.getIdCliente(), request.getIdEmpresa(), request.getProductos().size());
		ResponseEntity<ResponseInvoiceDto> response = null;
		ResolutionDto resolucion =null;
		PersonaDTO persona =null;
		EmpresaDTO empresa =null;
		Long numeroFactura =null;
		try {
			resolucion =getResolucion(request);
			persona =getCliente(request);
			empresa = getEmpresa(request);
			numeroFactura = actualizarResolucion(request, resolucion);
	        HttpEntity<RequestInvoiceDto> entity = new HttpEntity<>(FacturaDianMapper.INSTANCE.mapDataFacturaEletronica(getResolucion(request), empresa, persona, request, getCorreoPersona(request),numeroFactura),utilsRestemplate.getHeader());
			print("#############REQUEST ################3: {} ", entity.getBody());
			response = this.restTemplateConfig.restTemplate().exchange(
			        this.url.concat(this.endPointFactura),
			        HttpMethod.POST,
			        entity,
			        ResponseInvoiceDto.class
			);
			print("################RESPONSE ############## ", response.getBody());
			log.info("Fin metodo crearFacturaElectronica:{} " , response.getStatusCode());
			String descripcion = obtenerDescripcion(response.getBody());
			guardarFactura(request, resolucion, persona, empresa, numeroFactura, descripcion, response.getBody());
			if(response.getStatusCode().equals(HttpStatus.CREATED) && Objects.nonNull(response.getBody()) && Objects.nonNull(response.getBody().getInvoice()) && response.getBody().getInvoice().getLegalStatus().equals(LegalStatusEnum.ACCEPTED.getCodigo())) {
				return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.CREATED.value()).message(descripcion).response(response.getBody()).build(), HttpStatus.CREATED);
			}else {
				log.error("Reponse crearFacturaElectronica: {} ", response);
				return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.CONFLICT.value()).message(descripcion).build(), HttpStatus.CONFLICT);
			}
		}catch (HttpClientErrorException.BadRequest ex) {
			log.error(ex.getLocalizedMessage());
			ResponseInvoiceDto error = getResponse(ex.getResponseBodyAsString());
			log.info("################RESPONSE ############## {}", ex.getResponseBodyAsString());
			String descripcion = obtenerDescripcion(error);
			guardarFactura(request, resolucion, persona, empresa, numeroFactura, descripcion, error);
		    log.info("error: {} ", error);
		    return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.BAD_REQUEST.value()).message(descripcion).build(), HttpStatus.BAD_REQUEST);
		}catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.CONFLICT.value()).message(Constantes.ER_CONSUME_SERVICE_DIAN.concat(this.endPointFactura)).build(), HttpStatus.CONFLICT);
		}
	}
	
	private ResponseInvoiceDto getResponse(String descripcion) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(
					descripcion,
			        ResponseInvoiceDto.class
			);
		}catch (Exception e) {
			log.error(e.getLocalizedMessage());
		}
		return null;
	}
	
	private String obtenerDescripcion(ResponseInvoiceDto response) {
		log.info("Inicio metodo obtenerDescripcion");
		if(Objects.nonNull(response) && Objects.nonNull(response.getInvoice()) && Objects.nonNull(response.getInvoice().getGovernmentResponse())) {
			log.info("Fin metodo obtenerDescripcion:{} ", response.getInvoice().getGovernmentResponse().getMessage());
			return response.getInvoice().getGovernmentResponse().getMessage().concat(" ").concat(Objects.nonNull(response.getInvoice().getGovernmentResponse().getErrorMessages()) && !response.getInvoice().getGovernmentResponse().getErrorMessages().isEmpty() ? response.getInvoice().getGovernmentResponse().getErrorMessages().get(0):"");
		}else if(Objects.nonNull(response) && Objects.nonNull(response.getErrors()) && !response.getErrors().isEmpty()){
			log.info("Fin metodo obtenerDescripcion:{} ", response.getErrors().get(0).getMessage());
			return response.getErrors().get(0).getMessage();
		}else {
			log.info("Fin metodo obtenerDescripcion:{} ",Constantes.ER_CONSUME_SERVICE_DIAN.concat(this.endPointFactura));
			return Constantes.ER_CONSUME_SERVICE_DIAN.concat(this.endPointFactura);
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
		log.info("Fin metodo actualizarResolucion:{},{} ", request.getIdEmpresa(),siguiente);
		
		return siguiente;
	}
	
	@Transactional
	public void guardarFactura(final RequestFacturaDto request, final ResolutionDto resolucion, PersonaDTO persona, EmpresaDTO empresa, Long numeroFactura, String descripcion, ResponseInvoiceDto response) {
		log.info("Inicio metodo guardarFactura:{} ", numeroFactura);

		InvoiceDto invoiceDto = new InvoiceDto();
		if(Objects.nonNull(response) && Objects.nonNull(response.getInvoice())) {
			invoiceDto.setEstado(response.getInvoice().getStatus());
			invoiceDto.setEstadoLegal(response.getInvoice().getLegalStatus());
			invoiceDto.setIdDian(response.getInvoice().getId());
		}
		invoiceDto.setCliente(persona);
		invoiceDto.setEmpresa(empresa);
		invoiceDto.setActivo(Boolean.TRUE);
		invoiceDto.setNumero(numeroFactura);
		invoiceDto.setDescripcion(descripcion);
		invoiceDto.setUsuarioCreacion(request.getUsuario());
		facturaRepository.save(InvoiceMapper.INSTANCE.dtoToEntity(invoiceDto));
		log.info("Fin metodo guardarFactura:{} ", numeroFactura);
	}
	
	@Override
	public ResponseEntity<ResponseDTO> crearSetPrueba(RequestSetPruebaDto request) {
		log.info("Inicio metodo crearSetPrueba: {} " , request.getGovernmentId());
        HttpEntity<RequestSetPruebaDto> entity = new HttpEntity<>(request,utilsRestemplate.getHeader());
        FacturaDianServiceImpl.print("##########REQUEST########## ", entity);
		ResponseEntity<Object> response = this.restTemplateConfig.restTemplate().exchange(
		        this.url.concat(this.endPointTestSets),
		        HttpMethod.POST,
		        entity,
		        Object.class
		);
		FacturaDianServiceImpl.print("##########RESPONSE########## ", response);
		log.info("Fin metodo crearSetPrueba:{} " , response.getStatusCode());
		if(response.getStatusCode().equals(HttpStatus.CREATED)) {
			return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.CREATED.value()).message(HttpStatus.CREATED.name()).response(response.getBody()).build(), HttpStatus.CREATED);
		}else {
			log.error("Reponse crearSetPrueba: {} ", response);
		}
		return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.CONFLICT.value()).message(Constantes.ER_CONSUME_SERVICE_DIAN.concat(this.endPointTestSets)).build(), HttpStatus.CONFLICT);
	}
	
	private EmpresaDTO getEmpresa(final RequestFacturaDto request) {
		return EmpresaMapper.INSTANCE.entityToDto(this.empresaRepository.findById(request.getIdEmpresa()).orElseThrow(() -> new ProcessGenericException(Constantes.EMP_NOT_FOUND)));
	}
	
	private PersonaDTO getCliente(final RequestFacturaDto request) {
		return personaMapper.entityToDto(this.personaRepository.findById(request.getIdCliente()).orElseThrow(() -> new ProcessGenericException(Constantes.CLIENT_NOT_FOUND)));
	}
	
	private ResolutionDto getResolucion(final RequestFacturaDto request) {
		return ResolucionMapper.INSTANCE.entityToDto(this.resolutionRepository.findByEmpresaId(request.getIdEmpresa()).orElseThrow(() -> new ProcessGenericException(Constantes.RESOLUTION_NOT_FOUND)));
	}
	
	private CorreoGeneralDTO getCorreoPersona(final RequestFacturaDto request) {
		return correoGeneralMapper.entityToDto(this.correoGeneralRepository.findByPersonaIdAndActivoTrue(request.getIdCliente()).orElseThrow(() -> new ProcessGenericException(Constantes.EMAIL_NOT_FOUND)));
	}
	
	public static void print(String nombre,Object objeto) {
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
		log.info("Inicio metodo consultarFacturasPorEmpresa:{},{},{} ", idEmpresa,pageable.getPageSize(), pageable.getPageNumber() );
		List<InvoiceEntity> facturas = this.facturaRepository.findByEmpresaId(idEmpresa, pageable);
		log.info("Fin metodo consultarFacturasPorEmpresa:{},{} ", idEmpresa,facturas.size() );
		return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.OK.value()).message(HttpStatus.OK.name()).response(InvoiceMapper.INSTANCE.listEntityToDtoList(facturas)).build(), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<ResponseDTO> consultarDocumentoPorEmpresa(String idEmpresaDian, String tipo) {
		log.info("Inicio metodo consultarDocumentoPorEmpresa: {},{} " , idEmpresaDian, tipo);
        HttpEntity<Void> entity = new HttpEntity<>(utilsRestemplate.getHeader());
		String url =this.url.concat(endPointFactura).concat("/").concat(idEmpresaDian).concat("/").concat("files").concat("/").concat(tipo);
		log.info("URL: {} " , url);
		ResponseEntity<Object> response = this.restTemplateConfig.restTemplate().exchange(
				url,
		        HttpMethod.GET,
		        entity,
		        Object.class
		);
		log.info("Fin metodo consultarDocumentoPorEmpresa:{} " , response.getStatusCode());
		if(response.getStatusCode().equals(HttpStatus.OK) && Objects.nonNull(response.getBody())) {
			return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.OK.value()).message(HttpStatus.OK.name()).response(response.getBody()).build(), HttpStatus.OK);
		}else {
			log.error("Reponse consultarDocumentoPorEmpresa: {} ", response);
		}
		return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.CONFLICT.value()).message(Constantes.ER_CONSUME_SERVICE_DIAN.concat(url)).build(), HttpStatus.CONFLICT);
	}

}
