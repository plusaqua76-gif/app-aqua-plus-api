package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IInventarioService;
import com.aqua.plus.commons.dtos.InventarioDTO;
import com.aqua.plus.commons.dtos.InventarioResponseDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.InventarioEntity;
import com.aqua.plus.commons.maps.InventarioMapper;
import com.aqua.plus.commons.repositories.InventarioRepository;
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
public class InventarioServiceImpl implements IInventarioService{

	private final InventarioRepository inventarioRepository;
	private final InventarioMapper inventarioMapper;
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(InventarioDTO inventarioDTO) {
	    log.info("Guardar/Actualizar Inventario ");
	    try {
	        boolean isUpdate = inventarioDTO.getId() != null && inventarioRepository.existsById(inventarioDTO.getId());
	        InventarioEntity entity;

	        if (isUpdate) {
	            entity = inventarioRepository.findById(inventarioDTO.getId()).orElseThrow();
	            inventarioMapper.updateEntityFromDto(inventarioDTO, entity);
	            entity.setFechaModificacion(new Date());
	            entity.setUsuarioModificacion(inventarioDTO.getUsuarioModificacion());
	        } else {
	            entity = inventarioMapper.dtoToEntity(inventarioDTO);
	            entity.setFechaCreacion(new Date());
	            entity.setUsuarioCreacion(inventarioDTO.getUsuarioCreacion());
	            entity.setActivo(true);
	        }

	        InventarioEntity saved = inventarioRepository.save(entity);
	        InventarioDTO savedDTO = inventarioMapper.entityToDto(saved);

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
	        log.error("Error guardando el inventario", e);
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
	public ResponseEntity<ResponseDTO> findByEnterpriseId(Integer idEmpresa, Pageable pageable) {
	    log.info("Buscar inventario por id de empresa: {}", idEmpresa);
	    try {
	        Page<InventarioEntity> inventario = inventarioRepository.findByProducto_Empresa_Id(idEmpresa, pageable);

	        if (inventario.isEmpty()) {
	            ResponseDTO responseDTO = ResponseDTO.builder()
	                    .success(false)
	                    .message("No se encontraron inventarios para la empresa con id " + idEmpresa)
	                    .code(HttpStatus.NOT_FOUND.value())
	                    .build();
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
	        }

	        List<InventarioResponseDTO> dtoList = inventario.stream()
	                .map(entity -> InventarioResponseDTO.builder()
	                        .id(entity.getId())
	                        .productoId(entity.getProducto().getId())
	                        .codigo(entity.getProducto().getCodigo())
	                        .nombre(entity.getProducto().getNombre())
	                        .cantidad(entity.getCantidad())
	                        .precioUnitario(entity.getPrecioUnitario())
	                        .precioVenta(entity.getPrecioVenta())
	                        .porcentaje(entity.getPorcentaje())
	                        .build()
	                ).toList();

	        ResponseDTO responseDTO = ResponseDTO.builder()
	                .success(true)
	                .message(Constantes.CONSULTED_SUCCESSFULLY)
	                .code(HttpStatus.OK.value())
	                .response(dtoList)
	                .build();

	        return ResponseEntity.ok(responseDTO);
	    } catch (Exception e) {
	        log.error("Error al buscar inventario por id de empresa: {}", idEmpresa, e);
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
	public ResponseEntity<ResponseDTO> findById(Integer id) {
	    log.info("Buscar inventario por id: {}", id);
	    try {
	        Optional<InventarioEntity> inventario = inventarioRepository.findById(id);
	        if (inventario.isPresent()) {
	        	InventarioDTO dto = inventarioMapper.entityToDto(inventario.get());
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
	        log.error("Error al buscar inventario por id: {}", id, e);
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
        log.info("Listar todos los inventarios");
        try {
            var list = inventarioRepository.findAll();
            var dtoList = inventarioMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar los inventarios", e);
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
        log.info("Inicio método para eliminar inventario por id: {}", id);
        try {
            if (!inventarioRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            inventarioRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar inventario con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}
