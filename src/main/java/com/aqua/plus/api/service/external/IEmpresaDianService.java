package com.aqua.plus.api.service.external;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ResolutionDto;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.CompanyDto;

public interface IEmpresaDianService {
	
	public ResponseEntity<ResponseDTO> darAltaEmpresa(final CompanyDto request);
	
	public ResponseEntity<ResponseDTO> consultarEmpresaPorId(final String id);
	
	public ResponseEntity<ResponseDTO> guardarResolution(final ResolutionDto resolution);
	
	public ResponseEntity<ResponseDTO> consultarResolucionPorId(final Integer id);
}
