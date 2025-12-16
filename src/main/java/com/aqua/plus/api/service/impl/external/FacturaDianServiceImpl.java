package com.aqua.plus.api.service.impl.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.aqua.plus.api.config.RestTemplateConfig;
import com.aqua.plus.api.maps.FacturaDianMapper;
import com.aqua.plus.api.service.external.IFacturaDianService;
import com.aqua.plus.api.utils.UtilsRestemplate;
import com.aqua.plus.commons.dtos.CorreoGeneralDTO;
import com.aqua.plus.commons.dtos.EmpresaDTO;
import com.aqua.plus.commons.dtos.PersonaDTO;
import com.aqua.plus.commons.dtos.ResolutionDto;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.CompanyDto;
import com.aqua.plus.commons.dtos.external.RequestFacturaDto;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto;
import com.aqua.plus.commons.dtos.external.RequestSetPruebaDto;
import com.aqua.plus.commons.dtos.external.ResponseInvoiceDto;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import com.aqua.plus.commons.maps.CorreoGeneralMapper;
import com.aqua.plus.commons.maps.EmpresaMapper;
import com.aqua.plus.commons.maps.PersonaMapper;
import com.aqua.plus.commons.maps.ResolucionMapper;
import com.aqua.plus.commons.repositories.CorreoGeneralRepository;
import com.aqua.plus.commons.repositories.EmpresaRepository;
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
	
	@Override
	public ResponseEntity<ResponseDTO> crearFacturaElectronica(final RequestFacturaDto request) {
		log.info("Inicio metodo crearFacturaElectronica: {},{},{} " , request.getIdCliente(), request.getIdEmpresa(), request.getProductos().size());
        HttpEntity<RequestInvoiceDto> entity = new HttpEntity<>(FacturaDianMapper.INSTANCE.mapDataFacturaEletronica(getResolucion(request), getEmpresa(request), getCliente(request), request, getCorreoPersona(request)),utilsRestemplate.getHeader());
		print("#############REQUEST ################3: {} ", entity.getBody());
		ResponseEntity<ResponseInvoiceDto> response = this.restTemplateConfig.restTemplate().exchange(
		        this.url.concat(this.endPointFactura),
		        HttpMethod.POST,
		        entity,
		        ResponseInvoiceDto.class
		);
		print("################RESPONSE ############## ", response.getBody());
		log.info("Fin metodo crearFacturaElectronica:{} " , response.getStatusCode());
		if(response.getStatusCode().equals(HttpStatus.CREATED)) {
			return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.CREATED.value()).message(HttpStatus.CREATED.name()).response(response.getBody()).build(), HttpStatus.CREATED);
		}else {
			log.error("Reponse crearFacturaElectronica: {} ", response);
		}
		return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.CONFLICT.value()).message(Constantes.ER_CONSUME_SERVICE_DIAN.concat(this.endPointFactura)).build(), HttpStatus.CONFLICT);
	}
	
	@Override
	public ResponseEntity<ResponseDTO> crearSetPrueba(RequestSetPruebaDto request) {
		log.info("Inicio metodo crearSetPrueba: {} " , request.getGovernmentId());
        HttpEntity<CompanyDto> entity = new HttpEntity<>(utilsRestemplate.getHeader());
		
		ResponseEntity<Object> response = this.restTemplateConfig.restTemplate().exchange(
		        this.url.concat(this.endPointTestSets),
		        HttpMethod.POST,
		        entity,
		        Object.class
		);
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
	
	private void print(String nombre,Object objeto) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			log.info(nombre, mapper.writeValueAsString(objeto));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
