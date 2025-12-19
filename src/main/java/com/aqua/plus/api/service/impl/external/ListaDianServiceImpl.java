package com.aqua.plus.api.service.impl.external;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.aqua.plus.api.config.RestTemplateConfig;
import com.aqua.plus.api.service.external.IListaDianService;
import com.aqua.plus.api.utils.UtilsRestemplate;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.ResponseListaDianDto;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListaDianServiceImpl implements IListaDianService {
	
	@Value("${alegra.url}")
	private String url;

	private final RestTemplateConfig restTemplateConfig;
	
	private final UtilsRestemplate utilsRestemplate;
	
	
	@Override
	public ResponseEntity<ResponseDTO> getLista(String endPoint,String departamento) {
		log.info("Inicio metodo getTypesIdentification");
		HttpEntity<Void> entity = new HttpEntity<>(utilsRestemplate.getHeader());
		
		ResponseEntity<ResponseListaDianDto> response = this.restTemplateConfig.restTemplate().exchange(
		        this.url.concat("/").concat(endPoint),
		        HttpMethod.GET,
		        entity,
		        ResponseListaDianDto.class
		);
		log.info("Fin metodo getTypesIdentification:{} " , response.getStatusCode());
		if(response.getStatusCode().equals(HttpStatus.OK) && Objects.nonNull(response.getBody())) {
			return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.OK.value()).message(HttpStatus.OK.name()).response(getResponse(response.getBody(), departamento)).build(), HttpStatus.OK);
		}else {
			log.error("Reponse lista: {} ", response);
		}
		return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.CONFLICT.value()).message(Constantes.LIST_ER_DIAN.concat(endPoint)).build(), HttpStatus.CONFLICT);
	}
	
	private Object getResponse(final ResponseListaDianDto response,String departamento) {
		
		if(Objects.nonNull(response.getIdentificationTypes())) {
			return response.getIdentificationTypes();
		}else if(Objects.nonNull(response.getDepartments())) {
			return response.getDepartments();
		}else if(Objects.nonNull(response.getMunicipalities())) {
			return response.getMunicipalities().stream().filter(item -> item.getDepartmentCode().equals(departamento));
		}else if(Objects.nonNull(response.getPaymentMethods())) {
			return response.getPaymentMethods();
		}else if(Objects.nonNull(response.getUnitCodes())) {
			return response.getUnitCodes();
		}else {
			return null;
		}
	}

}
