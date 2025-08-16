package com.acua.plus.api.service.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acua.plus.api.service.ICategoriaProductoService;
import com.acua.plus.commons.dtos.CategoriaProductoDTO;
import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.entities.CategoriaProductoEntity;
import com.acua.plus.commons.maps.CategoriaProductoMapper;
import com.acua.plus.commons.repositories.CategoriaProductoRepository;
import com.acua.plus.commons.utils.Constantes;

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
public class CategoriaProductoServiceImpl implements ICategoriaProductoService {

	private final CategoriaProductoRepository categoriaRepository;
	private final CategoriaProductoMapper categoriaMapper;
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(CategoriaProductoDTO categoriaDTO) {
	    log.info("Guardar/Actualizar Tipo Cuenta Contable");
	    try {
	        boolean isUpdate = categoriaDTO.getId() != null && categoriaRepository.existsById(categoriaDTO.getId());
	        CategoriaProductoEntity entity;

	        if (isUpdate) {
	            entity = categoriaRepository.findById(categoriaDTO.getId()).orElseThrow();
	            categoriaMapper.updateEntityFromDto(categoriaDTO, entity);
	            entity.setFechaModificacion(new Date());
	            entity.setUsuarioModificacion(categoriaDTO.getUsuarioModificacion());
	        } else {
	            entity = categoriaMapper.dtoToEntity(categoriaDTO);
	            entity.setFechaCreacion(new Date());
	            entity.setUsuarioCreacion(categoriaDTO.getUsuarioCreacion());
	            entity.setActivo(true);
	        }

	        CategoriaProductoEntity saved = categoriaRepository.save(entity);
	        CategoriaProductoDTO savedDTO = categoriaMapper.entityToDto(saved);

	        String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
	        int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

	        ResponseDTO responseDTO = ResponseDTO.builder()
	                .success(true)
	                .message(message)
	                .code(statusCode)
	                .response(savedDTO)
	                .build();

	        return ResponseEntity.status(statusCode).body(responseDTO);

	    } catch (Exception e) {
	        log.error("Error guardando la categoria producto", e);
	        ResponseDTO errorResponse = ResponseDTO.builder()
	                .success(false)
	                .message(Constantes.SAVE_ERROR)
	                .code(HttpStatus.BAD_REQUEST.value())
	                .build();

	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	    }
	}
	
	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
	    log.info("Buscar categoria producto por id: {}", id);
	    try {
	        Optional<CategoriaProductoEntity> categoria = categoriaRepository.findById(id);
	        if (categoria.isPresent()) {
	        	CategoriaProductoDTO dto = categoriaMapper.entityToDto(categoria.get());
	            ResponseDTO responseDTO = ResponseDTO.builder()
	                    .success(true)
	                    .message(Constantes.CONSULTED_SUCCESSFULLY)
	                    .code(HttpStatus.OK.value())
	                    .response(dto)
	                    .build();
	            return ResponseEntity.ok(responseDTO);
	        } else {
	            ResponseDTO responseDTO = ResponseDTO.builder()
	                    .success(false)
	                    .message(Constantes.CONSULTING_ERROR)
	                    .code(HttpStatus.NOT_FOUND.value())
	                    .build();
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
	        }
	    } catch (Exception e) {
	        log.error("Error al buscar la categoria producto por id: {}", id, e);
	        ResponseDTO responseDTO = ResponseDTO.builder()
	                .success(false)
	                .message(Constantes.ERROR_QUERY_RECORD_BY_ID)
	                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
	                .build();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
	    }
	}
	
	@Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findAll() {
        log.info("Listar todas las categortias");
        try {
            var list = categoriaRepository.findAll();
            var dtoList = categoriaMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar las categorias", e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.CONSULTING_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .response(null)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
	
	@Override
    @Transactional
    public ResponseEntity<ResponseDTO> deleteById(Integer id) {
        log.info("Inicio método para eliminar categoria por id: {}", id);
        try {
            if (!categoriaRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            categoriaRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar la categoria con el id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}
