package com.aqua.plus.api.service.external;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.RequestFacturaDto;
import com.aqua.plus.commons.dtos.external.RequestSetPruebaDto;

public interface IFacturaDianService {
	
	public ResponseEntity<ResponseDTO> crearFacturaElectronica(final RequestFacturaDto request);
	
	public ResponseEntity<ResponseDTO> crearSetPrueba(final RequestSetPruebaDto request);
	
	public ResponseEntity<ResponseDTO> consultarFacturasPorEmpresa(final Integer idEmpresa, final Pageable pageable);
}
