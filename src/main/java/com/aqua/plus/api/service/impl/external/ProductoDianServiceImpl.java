package com.aqua.plus.api.service.impl.external;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.aqua.plus.api.service.external.IProductoDianService;
import com.aqua.plus.commons.dtos.ProductDto;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ProductEntity;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import com.aqua.plus.commons.maps.ProductMapper;
import com.aqua.plus.commons.repositories.ProductRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductoDianServiceImpl implements IProductoDianService {

	private final ProductRepository productRepository;
	
	@Override
	public ResponseEntity<ResponseDTO> guardarProducto(ProductDto product) {
		log.info("Inicio metodo guardarProducto:{} ", product.getNombre());
		this.productRepository.save(ProductMapper.INSTANCE.dtoToEntity(product));
		return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.CREATED.value()).message(HttpStatus.CREATED.name()).build(), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<ResponseDTO> consultarProductos() {
		return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.OK.value()).message(HttpStatus.OK.name()).response(ProductMapper.INSTANCE.listDtoToEntity(this.productRepository.findAll())).build(), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<ResponseDTO> consultarProductoPorCodigo(String codigo) {
		log.info("Inicio metodo consultarProductoPorCodigo:{} ", codigo);
		ProductEntity producto =this.productRepository.findByCodigoUnidad(codigo).orElseThrow(() -> new ProcessGenericException(Constantes.PRODUCT_NOT_FOUND));
		log.info("Fin metodo consultarProductoPorCodigo:{},{} ", codigo,producto.getNombre());
		return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.OK.value()).message(HttpStatus.OK.name()).response(ProductMapper.INSTANCE.entityToDto(producto)).build(), HttpStatus.OK);
	}

}
