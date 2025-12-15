package com.aqua.plus.api.service.external;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto;
import com.aqua.plus.commons.dtos.external.RequestSetPruebaDto;

public interface IFacturaDianService {
	
	public ResponseEntity<ResponseDTO> crearFacturaElectronica(final RequestInvoiceDto request);
	
	public ResponseEntity<ResponseDTO> crearSetPrueba(final RequestSetPruebaDto request);
}
