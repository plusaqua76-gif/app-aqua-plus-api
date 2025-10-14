package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.ICategoriaProductoService;
import com.aqua.plus.commons.dtos.CategoriaProductoDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.CategoriaProductoEntity;
import com.aqua.plus.commons.maps.CategoriaProductoMapper;
import com.aqua.plus.commons.repositories.CategoriaProductoRepository;
import com.aqua.plus.commons.repositories.ProductoRepository;
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
public class CategoriaProductoServiceImpl implements ICategoriaProductoService {

	private final CategoriaProductoRepository categoriaRepository;
	private final ProductoRepository productoRepository;
	private final CategoriaProductoMapper categoriaMapper;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(CategoriaProductoDTO categoriaDTO) {
		log.info("Guardar/Actualizar Categoria producto");
		try {
			Integer rawId = categoriaDTO.getId();
			Integer id = (rawId != null && rawId > 0) ? rawId : null;

			boolean isUpdate = id != null && categoriaRepository.existsById(id);
			CategoriaProductoEntity entity;

			if (isUpdate) {
				boolean usada = productoRepository.existsByCategoria_Id(id);
				if (usada) {
					return ResponseEntity.status(HttpStatus.CONFLICT)
							.body(ResponseDTO.builder().success(false).code(HttpStatus.CONFLICT.value())
									.message(
											"No se puede actualizar: la categoría está asociada a uno o más productos.")
									.build());
				}

				entity = categoriaRepository.findById(id).orElseThrow();
				categoriaMapper.updateEntityFromDto(categoriaDTO, entity);
				entity.setFechaModificacion(new Date());
				entity.setUsuarioModificacion(categoriaDTO.getUsuarioModificacion());

			} else {
				categoriaDTO.setId(null);
				entity = categoriaMapper.dtoToEntity(categoriaDTO);
				entity.setId(null);
				entity.setFechaCreacion(new Date());
				entity.setUsuarioCreacion(categoriaDTO.getUsuarioCreacion());
				if (entity.getActivo() == null)
					entity.setActivo(true);
			}

			CategoriaProductoEntity saved = categoriaRepository.save(entity);
			CategoriaProductoDTO savedDTO = categoriaMapper.entityToDto(saved);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			return ResponseEntity.status(statusCode).body(
					ResponseDTO.builder().success(true).message(message).code(statusCode).response(savedDTO).build());

		} catch (org.springframework.dao.DataIntegrityViolationException e) {
			log.error("Violación de integridad guardando/actualizando la categoría", e);
			return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.builder().success(false)
					.message("Conflicto de datos: " + rootCauseMessage(e)).code(HttpStatus.CONFLICT.value()).build());
		} catch (Exception e) {
			log.error("Error guardando la categoria producto", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
					.message(Constantes.SAVE_ERROR).code(HttpStatus.BAD_REQUEST.value()).build());
		}
	}

	private String rootCauseMessage(Throwable t) {
		Throwable c = t;
		while (c.getCause() != null && c.getCause() != c)
			c = c.getCause();
		return c.getMessage() != null ? c.getMessage() : c.toString();
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
		log.info("Buscar categoria producto por id: {}", id);
		try {
			Optional<CategoriaProductoEntity> categoria = categoriaRepository.findById(id);
			if (categoria.isPresent()) {
				CategoriaProductoDTO dto = categoriaMapper.entityToDto(categoria.get());
				ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
						.code(HttpStatus.OK.value()).response(dto).build();
				return ResponseEntity.ok(responseDTO);
			} else {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
		} catch (Exception e) {
			log.error("Error al buscar la categoria producto por id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
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
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar las categorias", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar categoria por id: {}", id);
		try {
			if (id == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("El id es requerido").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			// 1) Existe la categoría?
			if (!categoriaRepository.existsById(id)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.builder().success(false)
						.message(Constantes.RECORD_NOT_FOUND).code(HttpStatus.NOT_FOUND.value()).build());
			}

			// 2) ¿Está siendo usada por algún producto?
			boolean usadaEnProductos = productoRepository.existsByCategoria_Id(id);
			if (usadaEnProductos) {
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body(ResponseDTO.builder().success(false)
								.message("No se puede eliminar: la categoría está asociada a uno o más productos.")
								.code(HttpStatus.CONFLICT.value()).build());
			}

			// 3) Eliminar
			categoriaRepository.deleteById(id);

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build());

		} catch (org.springframework.dao.DataIntegrityViolationException e) {
			// Respaldo por si la FK en BD impide borrar (carrera de datos, etc.)
			log.error("FK impide eliminar la categoría {}", id, e);
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(ResponseDTO.builder().success(false)
							.message("No se puede eliminar: existen referencias en otras tablas.")
							.code(HttpStatus.CONFLICT.value()).build());
		} catch (Exception e) {
			log.error("Error al eliminar la categoria con el id: {}", id, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message(Constantes.DELETE_ERROR).code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

}
