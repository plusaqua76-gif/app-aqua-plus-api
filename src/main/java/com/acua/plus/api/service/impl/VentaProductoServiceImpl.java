package com.acua.plus.api.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acua.plus.api.service.IVentaProductoService;
import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.dtos.VentaProductoDTO;
import com.acua.plus.commons.entities.VentaProductoEntity;
import com.acua.plus.commons.maps.VentaProductoMapper;
import com.acua.plus.commons.repositories.VentaProductoRepository;
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
public class VentaProductoServiceImpl implements IVentaProductoService {

	private final VentaProductoRepository ventaProductoRepository;
	private final VentaProductoMapper ventaProductoMapper;
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(VentaProductoDTO ventaProductoDTO) {
	    log.info("Guardar/Actualizar Venta Producto");
	    try {
	        boolean isUpdate = ventaProductoDTO.getId() != null && ventaProductoRepository.existsById(ventaProductoDTO.getId());
	        VentaProductoEntity entity;

	        if (isUpdate) {
	            entity = ventaProductoRepository.findById(ventaProductoDTO.getId()).orElseThrow();
	            ventaProductoMapper.updateEntityFromDto(ventaProductoDTO, entity);
	            entity.setFechaModificacion(new Date());
	            entity.setUsuarioModificacion(ventaProductoDTO.getUsuarioModificacion());
	        } else {
	            entity = ventaProductoMapper.dtoToEntity(ventaProductoDTO);
	            entity.setFechaCreacion(new Date());
	            entity.setUsuarioCreacion(ventaProductoDTO.getUsuarioCreacion());
	            entity.setActivo(true);
	        }

	        VentaProductoEntity saved = ventaProductoRepository.save(entity);
	        VentaProductoDTO savedDTO = ventaProductoMapper.entityToDto(saved);

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
	        log.error("Error guardando el venta producto", e);
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
	public ResponseEntity<ResponseDTO> findBySaleId(Integer idVenta) {
	    log.info("Buscar venta producto por id de venta: {}", idVenta);
	    try {
	        List<VentaProductoEntity> ventaProducto= ventaProductoRepository.findByVenta_Id(idVenta);

	        if (ventaProducto.isEmpty()) {
	            ResponseDTO responseDTO = ResponseDTO.builder()
	                    .success(false)
	                    .message("No se encontraron patrimonios para la empresa con id " + idVenta)
	                    .code(HttpStatus.NOT_FOUND.value())
	                    .build();
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
	        }

	        List<VentaProductoDTO> dtoList = ventaProductoMapper.listEntityToDtoList(ventaProducto);

	        ResponseDTO responseDTO = ResponseDTO.builder()
	                .success(true)
	                .message(Constantes.CONSULTED_SUCCESSFULLY)
	                .code(HttpStatus.OK.value())
	                .response(dtoList)
	                .build();

	        return ResponseEntity.ok(responseDTO);
	    } catch (Exception e) {
	        log.error("Error al buscar venta producto por id de venta: {}", idVenta, e);
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
	    log.info("Buscar venta producto por id: {}", id);
	    try {
	        Optional<VentaProductoEntity> ventaProducto = ventaProductoRepository.findById(id);
	        if (ventaProducto.isPresent()) {
	        	VentaProductoDTO dto = ventaProductoMapper.entityToDto(ventaProducto.get());
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
	        log.error("Error al buscar venta producto por id: {}", id, e);
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
        log.info("Listar todos los venta producto");
        try {
            var list = ventaProductoRepository.findAll();
            var dtoList = ventaProductoMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar los venta producto", e);
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
        log.info("Inicio método para eliminar venta producto por id: {}", id);
        try {
            if (!ventaProductoRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            ventaProductoRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar venta producto con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}
