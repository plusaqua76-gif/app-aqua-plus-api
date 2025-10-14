package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IProductoService;
import com.aqua.plus.api.service.impl.specification.ProductoSpecifications;
import com.aqua.plus.commons.dtos.ProductoDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ProductoEntity;
import com.aqua.plus.commons.maps.ProductoMapper;
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
public class ProductoServiceImpl implements IProductoService {

	private final ProductoRepository productoRepository;
	private final ProductoMapper productoMapper;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(ProductoDTO productoDTO) {
		log.info("Guardar/Actualizar Producto ");
		try {
			boolean isUpdate = productoDTO.getId() != null && productoRepository.existsById(productoDTO.getId());
			ProductoEntity entity;

			if (isUpdate) {
				entity = productoRepository.findById(productoDTO.getId()).orElseThrow();
				productoMapper.updateEntityFromDto(productoDTO, entity);
				entity.setFechaModificacion(new Date());
				entity.setUsuarioModificacion(productoDTO.getUsuarioModificacion());
			} else {
				entity = productoMapper.dtoToEntity(productoDTO);
				entity.setFechaCreacion(new Date());
				entity.setUsuarioCreacion(productoDTO.getUsuarioCreacion());
				entity.setActivo(true);
			}

			ProductoEntity saved = productoRepository.save(entity);
			ProductoDTO savedDTO = productoMapper.entityToDto(saved);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(message).code(statusCode)
					.response(savedDTO).build();

			return ResponseEntity.status(statusCode).body(responseDTO);

		} catch (Exception e) {
			log.error("Error guardando la cuenta", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEnterpriseId(Integer idEmpresa, String codigo, String nombre,
			String descripcion, String categoriaNombre, Pageable pageable) {

		log.info("Buscar producto por empresa={}, filtros: codigo={}, nombre={}, descripcion={}, categoriaNombre={}",
				idEmpresa, codigo, nombre, descripcion, categoriaNombre);

		try {
			if (idEmpresa == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("Parámetro requerido: idEmpresa").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			Sort defaultSort = Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.desc("id"));
			Pageable pageToUse = (pageable == null) ? PageRequest.of(0, 20, defaultSort)
					: (pageable.getSort().isUnsorted()
							? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort)
							: pageable);

			List<ProductoEntity> baseCheck = productoRepository.findByEmpresa_Id(idEmpresa);
			if (baseCheck.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron productos para la empresa con id " + idEmpresa)
								.code(HttpStatus.NOT_FOUND.value()).response(List.of()).totalCount(0L)
								.pageSize(pageToUse.getPageSize()).currentPage(pageToUse.getPageNumber()).totalPages(0)
								.build());
			}

			boolean hayFiltros = (codigo != null && !codigo.isBlank()) || (nombre != null && !nombre.isBlank())
					|| (descripcion != null && !descripcion.isBlank())
					|| (categoriaNombre != null && !categoriaNombre.isBlank());

			Page<ProductoEntity> page;

			if (!hayFiltros) {
				page = productoRepository.findByEmpresa_Id(idEmpresa, pageToUse);
			} else {
				Specification<ProductoEntity> baseEmpresa = (root, q, cb) -> cb.equal(root.join("empresa").get("id"),
						idEmpresa);

				Specification<ProductoEntity> filtros = Specification.allOf(ProductoSpecifications.codigoLike(codigo),
						ProductoSpecifications.nombreLike(nombre), ProductoSpecifications.descripcionLike(descripcion),
						ProductoSpecifications.categoriaNombreLike(categoriaNombre));

				Specification<ProductoEntity> specFinal = Specification.allOf(baseEmpresa, filtros);
				page = productoRepository.findAll(specFinal, pageToUse);
			}

			if (page.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron productos para la empresa con id " + idEmpresa)
								.code(HttpStatus.NOT_FOUND.value()).response(List.of()).totalCount(0L)
								.pageSize(pageToUse.getPageSize()).currentPage(pageToUse.getPageNumber()).totalPages(0)
								.build());
			}

			List<ProductoDTO> dtoList = productoMapper.listEntityToDtoList(page.getContent());

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).totalCount(page.getTotalElements())
					.pageSize(page.getSize()).currentPage(page.getNumber()).totalPages(page.getTotalPages()).build());

		} catch (Exception e) {
			log.error("Error al buscar productos por id de empresa: {}", idEmpresa, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
		log.info("Buscar producto por id: {}", id);
		try {
			Optional<ProductoEntity> producto = productoRepository.findById(id);
			if (producto.isPresent()) {
				ProductoDTO dto = productoMapper.entityToDto(producto.get());
				ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
						.code(HttpStatus.OK.value()).response(dto).build();
				return ResponseEntity.ok(responseDTO);
			} else {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
		} catch (Exception e) {
			log.error("Error al buscar producto por id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todos los productos");
		try {
			var list = productoRepository.findAll();
			var dtoList = productoMapper.listEntityToDtoList(list);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar los productos", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar producto por id: {}", id);
		try {
			if (!productoRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			productoRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar cuenta con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}
}
