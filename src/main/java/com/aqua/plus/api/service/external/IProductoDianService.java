package com.aqua.plus.api.service.external;

import org.springframework.http.ResponseEntity;

import com.aqua.plus.commons.dtos.ProductDto;
import com.aqua.plus.commons.dtos.ResponseDTO;

public interface IProductoDianService {
	
	public ResponseEntity<ResponseDTO> guardarProducto(final ProductDto product);
	
	public ResponseEntity<ResponseDTO> consultarProductos();
}
