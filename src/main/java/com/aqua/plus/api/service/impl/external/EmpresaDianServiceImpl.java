package com.aqua.plus.api.service.impl.external;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.config.RestTemplateConfig;
import com.aqua.plus.api.service.external.IEmpresaDianService;
import com.aqua.plus.api.utils.UtilsRestemplate;
import com.aqua.plus.commons.dtos.ResolutionDto;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.CompanyDto;
import com.aqua.plus.commons.dtos.external.ResponseConsultaEmpresaDto;
import com.aqua.plus.commons.dtos.external.ResponseEmpresaAltaDto;
import com.aqua.plus.commons.entities.EmpresaEntity;
import com.aqua.plus.commons.entities.ResolutionEntity;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import com.aqua.plus.commons.maps.ResolucionMapper;
import com.aqua.plus.commons.repositories.EmpresaRepository;
import com.aqua.plus.commons.repositories.ResolutionRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmpresaDianServiceImpl implements IEmpresaDianService {

	@Value("${alegra.url}")
	private String url;
	
	@Value("${alegra.end-point.empresa.companies}")
	private String endPointEmpresa;

	private final RestTemplateConfig restTemplateConfig;
	
	private final UtilsRestemplate utilsRestemplate;
	
	private final ResolutionRepository resolutionRepository;
	
	private final EmpresaRepository empresaRepository;
	
	@Override
	public ResponseEntity<ResponseDTO> darAltaEmpresa(CompanyDto request) {
		log.info("Inicio metodo darAltaEmpresa: {} " , request.getIdentification());
        HttpEntity<CompanyDto> entity = new HttpEntity<>(request,utilsRestemplate.getHeader());
        FacturaDianServiceImpl.print("##########REQUEST########## ", entity);
		ResponseEntity<ResponseEmpresaAltaDto> response = this.restTemplateConfig.restTemplate().exchange(
		        this.url.concat(this.endPointEmpresa),
		        HttpMethod.POST,
		        entity,
		        ResponseEmpresaAltaDto.class
		);
	    FacturaDianServiceImpl.print("##########response########## ", response);
		log.info("Fin metodo darAltaEmpresa:{} " , response.getStatusCode());
		if(response.getStatusCode().equals(HttpStatus.CREATED)) {
			actualizarEmpresa(request, response.getBody());
			return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.CREATED.value()).message(HttpStatus.CREATED.name()).response(response.getBody()).build(), HttpStatus.CREATED);
		}else {
			log.error("Reponse darAltaEmpresa: {} ", response);
		}
		return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.CONFLICT.value()).message(Constantes.ER_CONSUME_SERVICE_DIAN.concat(this.endPointEmpresa)).build(), HttpStatus.CONFLICT);
	}

	@Transactional
	public void actualizarEmpresa(CompanyDto request, ResponseEmpresaAltaDto response) {
		log.info("Inicio metodo actualizarEmpresa: {} " , request.getIdentification());
		EmpresaEntity entity= this.empresaRepository.findByNit(request.getIdentification()).orElseThrow(() -> new ProcessGenericException(Constantes.EMP_NOT_FOUND));
		entity.setIdEmpresaDian(response.getCompany().getId());
		this.empresaRepository.save(entity);
		log.info("Fin metodo actualizarEmpresa: {} " , request.getIdentification());
	}
	@Override
	public ResponseEntity<ResponseDTO> consultarEmpresaPorId(String id) {
		log.info("Inicio metodo consultarEmpresaPorId: {} " , id);
        HttpEntity<Void> entity = new HttpEntity<>(utilsRestemplate.getHeader());
		
		ResponseEntity<ResponseConsultaEmpresaDto> response = this.restTemplateConfig.restTemplate().exchange(
		        this.url.concat(endPointEmpresa).concat("/").concat(id),
		        HttpMethod.GET,
		        entity,
		        ResponseConsultaEmpresaDto.class
		);
		log.info("Fin metodo consultarEmpresaPorId:{} " , response.getStatusCode());
		if(response.getStatusCode().equals(HttpStatus.OK)&& Objects.nonNull(response.getBody()) && Objects.nonNull(response.getBody())) {
			return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.OK.value()).message(HttpStatus.OK.name()).response(response.getBody()).build(), HttpStatus.OK);
		}else {
			log.error("Reponse consultarEmpresaPorId: {} ", response);
		}
		return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.CONFLICT.value()).message(Constantes.ER_CONSUME_SERVICE_DIAN.concat(endPointEmpresa).concat("/").concat(id)).build(), HttpStatus.CONFLICT);
	}
}
