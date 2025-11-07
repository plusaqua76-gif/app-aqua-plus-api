package com.aqua.plus.api.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
		log.info("Guardar/Actualizar Producto");

		try {
			// ===== Validaciones básicas =====
			if (productoDTO == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("Body requerido: producto").code(HttpStatus.BAD_REQUEST.value()).build());
			}
			if (productoDTO.getEmpresa() == null || productoDTO.getEmpresa().getId() == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("El id de la empresa es requerido").code(HttpStatus.BAD_REQUEST.value()).build());
			}
			if (productoDTO.getCodigo() == null || productoDTO.getCodigo().isBlank()) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("El código del producto es requerido").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			final Integer targetEmpresaId = productoDTO.getEmpresa().getId();
			final String targetCodigo = productoDTO.getCodigo().trim();
			final String targetNombre = (productoDTO.getNombre() == null || productoDTO.getNombre().isBlank())
					? "(sin nombre)"
					: productoDTO.getNombre().trim();

			final boolean isUpdate = (productoDTO.getId() != null
					&& productoRepository.existsById(productoDTO.getId()));

			if (!isUpdate) {
				if (productoRepository.existsByEmpresa_IdAndCodigoIgnoreCase(targetEmpresaId, targetCodigo)) {
					String msg = String.format("Producto '%s' ya existente con el código '%s'", targetNombre,
							targetCodigo);
					return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.builder().success(false)
							.message(msg).code(HttpStatus.CONFLICT.value()).build());
				}
			} else {
				ProductoEntity actual = productoRepository.findById(productoDTO.getId()).orElseThrow();

				Integer currentEmpresaId = (actual.getEmpresa() != null ? actual.getEmpresa().getId() : null);
				String currentCodigo = (actual.getCodigo() != null ? actual.getCodigo().trim() : null);

				boolean changedEmpresa = !java.util.Objects.equals(currentEmpresaId, targetEmpresaId);
				boolean changedCodigo = (currentCodigo == null && targetCodigo != null)
						|| (currentCodigo != null && !currentCodigo.equalsIgnoreCase(targetCodigo));

				if (changedEmpresa || changedCodigo) {
					if (productoRepository.existsByEmpresa_IdAndCodigoIgnoreCase(targetEmpresaId, targetCodigo)) {
						String msg = String.format("Producto '%s' ya existente con el código '%s'", targetNombre,
								targetCodigo);
						return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.builder().success(false)
								.message(msg).code(HttpStatus.CONFLICT.value()).build());
					}
				}
			}

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
				if (entity.getActivo() == null)
					entity.setActivo(true);
			}

			entity.setCodigo(targetCodigo);

			ProductoEntity saved = productoRepository.save(entity);
			ProductoDTO savedDTO = productoMapper.entityToDto(saved);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			return ResponseEntity.status(statusCode).body(
					ResponseDTO.builder().success(true).message(message).code(statusCode).response(savedDTO).build());

		} catch (Exception e) {
			log.error("Error guardando el producto", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
					.message(Constantes.SAVE_ERROR).code(HttpStatus.BAD_REQUEST.value()).build());
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

			Objects.requireNonNull(pageable, "El pageable no debe ser null");

			Sort defaultSort = Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.desc("id"));

			Pageable effectivePageable = pageable.getSort().isUnsorted()
					? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort)
					: pageable;

			boolean hayFiltros = (codigo != null && !codigo.isBlank()) || (nombre != null && !nombre.isBlank())
					|| (descripcion != null && !descripcion.isBlank())
					|| (categoriaNombre != null && !categoriaNombre.isBlank());

			Page<ProductoEntity> page;

			if (!hayFiltros) {
				page = productoRepository.findByEmpresa_Id(idEmpresa, effectivePageable);
			} else {

				Specification<ProductoEntity> empresaSpec = (root, q, cb) -> cb.equal(root.join("empresa").get("id"),
						idEmpresa);

				List<Specification<ProductoEntity>> specs = new ArrayList<>();
				specs.add(empresaSpec);
				specs.add(ProductoSpecifications.codigoLike(codigo));
				specs.add(ProductoSpecifications.nombreLike(nombre));
				specs.add(ProductoSpecifications.descripcionLike(descripcion));
				specs.add(ProductoSpecifications.categoriaNombreLike(categoriaNombre));

				Specification<ProductoEntity> specFinal = Specification
						.allOf(specs.stream().filter(Objects::nonNull).toList());

				page = productoRepository.findAll(specFinal, effectivePageable);
			}

			if (page.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.builder().success(false)
						.message("No se encontraron productos para la empresa con id " + idEmpresa)
						.code(HttpStatus.NOT_FOUND.value()).response(List.of()).totalCount(page.getTotalElements()) // 0
						.pageSize(page.getSize()).currentPage(page.getNumber()).totalPages(page.getTotalPages()) // 0
						.build());
			}

			List<ProductoDTO> dtoList = productoMapper.listEntityToDtoList(page.getContent());

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).totalCount(page.getTotalElements())
					.pageSize(page.getSize()).currentPage(page.getNumber()).totalPages(page.getTotalPages()).build());

		} catch (Exception e) {
			log.error("Error al buscar productos por id de empresa: {}", idEmpresa, e);

			// 🔹 Mapeo detallado de error (mismo estándar que los otros métodos)
			Throwable root = e;
			while (root.getCause() != null && root.getCause() != root) {
				root = root.getCause();
			}

			String errorMessage = e.getMessage();
			String rootCauseMessage = root.getMessage();

			Map<String, Object> errorInfo = new LinkedHashMap<>();
			errorInfo.put("exception", e.getClass().getName());
			errorInfo.put("message", errorMessage);
			errorInfo.put("rootCause", rootCauseMessage);

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false)
							.message("Error consultando productos: "
									+ (rootCauseMessage != null ? rootCauseMessage : "ver detalle en 'response'"))
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(errorInfo).build());
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
