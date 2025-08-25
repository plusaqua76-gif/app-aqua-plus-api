package com.aqua.plus.api.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IImagenEmpresaService;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ImagenEmpresaEntity;
import com.aqua.plus.commons.maps.ImagenEmpresaMapper;
import com.aqua.plus.commons.repositories.ImagenEmpresaRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author nicope
 * @version 1.0
 * 
 *          Clase que implementa la interfaz de la lógica de negocio.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class ImagenEmpresaServiceImpl implements IImagenEmpresaService {

	private final ImagenEmpresaRepository imagenEmpresaRepository;
	private final ImagenEmpresaMapper imagenEmpresaMapper;
	
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll(Integer page, Integer size) {
	    log.info("Listar imágenes de empresas con paginado opcional");
	    try {
	        List<ImagenEmpresaEntity> list;
	        
	        if (page != null && size != null) {
	            Pageable pageable = PageRequest.of(page, size);
	            Page<ImagenEmpresaEntity> pagedResult = imagenEmpresaRepository.findAll(pageable);
	            list = pagedResult.getContent();

	            var dtoList = imagenEmpresaMapper.listEntityToDtoList(list);
	            return ResponseEntity.ok(
	                ResponseDTO.builder()
	                    .success(true)
	                    .message(Constantes.CONSULTED_SUCCESSFULLY)
	                    .code(HttpStatus.OK.value())
	                    .response(dtoList)
	                    .build()
	            );
	        } else {
	            list = imagenEmpresaRepository.findAll();
	            var dtoList = imagenEmpresaMapper.listEntityToDtoList(list);
	            return ResponseEntity.ok(
	                ResponseDTO.builder()
	                    .success(true)
	                    .message(Constantes.CONSULTED_SUCCESSFULLY)
	                    .code(HttpStatus.OK.value())
	                    .response(dtoList)
	                    .build()
	            );
	        }
	    } catch (Exception e) {
	        log.error("Error al listar las imágenes de empresas", e);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
	            ResponseDTO.builder()
	                .success(false)
	                .message(Constantes.CONSULTING_ERROR)
	                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
	                .response(null)
	                .build()
	        );
	    }
	}

}
