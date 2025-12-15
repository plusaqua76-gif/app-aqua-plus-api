package com.aqua.plus.api.service.impl.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.aqua.plus.api.config.RestTemplateConfig;
import com.aqua.plus.api.service.external.IFacturaDianService;
import com.aqua.plus.api.utils.UtilsRestemplate;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.CompanyDto;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto;
import com.aqua.plus.commons.dtos.external.RequestSetPruebaDto;
import com.aqua.plus.commons.dtos.external.ResponseInvoiceDto;
import com.aqua.plus.commons.utils.Constantes;

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
	
	@Override
	public ResponseEntity<ResponseDTO> crearFacturaElectronica(RequestInvoiceDto request) {
		log.info("Inicio metodo crearFacturaElectronica: {} " , request.getNumber());
        HttpEntity<RequestInvoiceDto> entity = new HttpEntity<>(utilsRestemplate.getHeader());
		
		ResponseEntity<ResponseInvoiceDto> response = this.restTemplateConfig.restTemplate().exchange(
		        this.url.concat(this.endPointFactura),
		        HttpMethod.POST,
		        entity,
		        ResponseInvoiceDto.class
		);
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

}
