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

import com.aqua.plus.api.service.IInventarioService;
import com.aqua.plus.api.service.impl.specification.InventarioSpecifications;
import com.aqua.plus.commons.dtos.InventarioDTO;
import com.aqua.plus.commons.dtos.InventarioResponseDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.InventarioEntity;
import com.aqua.plus.commons.entities.ProductoEntity;
import com.aqua.plus.commons.maps.InventarioMapper;
import com.aqua.plus.commons.repositories.InventarioRepository;
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
public class InventarioServiceImpl implements IInventarioService {

	private final InventarioRepository inventarioRepository;
	private final ProductoRepository productoRepository;
	private final InventarioMapper inventarioMapper;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(InventarioDTO inventarioDTO) {
		log.info("Guardar/Actualizar Inventario (upsert por id/producto, sumando cantidad)");
		try {
			if (inventarioDTO == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("Body requerido: inventario").code(HttpStatus.BAD_REQUEST.value()).build());
			}
			if (inventarioDTO.getProducto() == null || inventarioDTO.getProducto().getId() == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("El id del producto es requerido").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			final Integer targetProductoId = inventarioDTO.getProducto().getId();

			InventarioEntity target = null;
			boolean isUpdate = false;

			if (inventarioDTO.getId() != null && inventarioRepository.existsById(inventarioDTO.getId())) {
				target = inventarioRepository.findById(inventarioDTO.getId()).orElseThrow();
				isUpdate = true;

				Optional<Integer> invIdByProductOpt = inventarioRepository.findIdByProductoId(targetProductoId);
				if (invIdByProductOpt.isPresent() && !invIdByProductOpt.get().equals(target.getId())) {
					Integer otherId = invIdByProductOpt.get();
					log.info(
							"[INV] DTO trae id={}, pero el producto {} ya tiene inventario id={}. Se actualizará id={}.",
							target.getId(), targetProductoId, otherId, otherId);
					target = inventarioRepository.findById(otherId).orElseThrow();
				} else {
					if (target.getProducto() == null || !targetProductoId.equals(target.getProducto().getId())) {
						ProductoEntity prod = productoRepository.findById(targetProductoId).orElseThrow();
						target.setProducto(prod);
					}
				}

			} else {
				Optional<Integer> invIdByProductOpt = inventarioRepository.findIdByProductoId(targetProductoId);
				if (invIdByProductOpt.isPresent()) {
					Integer existingId = invIdByProductOpt.get();
					target = inventarioRepository.findById(existingId).orElseThrow();
					isUpdate = true;
					log.info("[INV] Upsert por producto: {} → update id={}", targetProductoId, existingId);
				} else {
					target = inventarioMapper.dtoToEntity(inventarioDTO);
					isUpdate = false;
					target.setFechaCreacion(new Date());
					target.setUsuarioCreacion(inventarioDTO.getUsuarioCreacion());
					if (target.getActivo() == null)
						target.setActivo(true);

					if (target.getProducto() == null || !targetProductoId.equals(target.getProducto().getId())) {
						ProductoEntity prod = productoRepository.findById(targetProductoId).orElseThrow();
						target.setProducto(prod);
					}
					log.info("[INV] Crear inventario nuevo para producto {}", targetProductoId);
				}
			}

			if (inventarioDTO.getCantidad() != null && inventarioDTO.getCantidad() < 0) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false)
								.message("La cantidad no puede ser negativa. Solo se permiten incrementos.")
								.code(HttpStatus.BAD_REQUEST.value()).build());
			}

			applyPartialUpdateSoloSumas(target, inventarioDTO, isUpdate);

			if (isUpdate) {
				target.setFechaModificacion(new Date());
				target.setUsuarioModificacion(inventarioDTO.getUsuarioModificacion());
			}

			InventarioEntity saved = inventarioRepository.save(target);
			InventarioDTO savedDTO = inventarioMapper.entityToDto(saved);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			return ResponseEntity.status(statusCode).body(
					ResponseDTO.builder().success(true).message(message).code(statusCode).response(savedDTO).build());

		} catch (Exception e) {
			log.error("Error guardando el inventario", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
					.message(Constantes.SAVE_ERROR).code(HttpStatus.BAD_REQUEST.value()).build());
		}
	}

	private void applyPartialUpdateSoloSumas(InventarioEntity entity, InventarioDTO dto, boolean isUpdate) {
		if (dto.getCantidad() != null) {
			if (isUpdate) {
				int base = (entity.getCantidad() != null ? entity.getCantidad() : 0);
				int delta = dto.getCantidad();
				entity.setCantidad(base + delta);
			} else {
				entity.setCantidad(dto.getCantidad());
			}
		}

		if (dto.getPrecioUnitario() != null)
			entity.setPrecioUnitario(dto.getPrecioUnitario());
		if (dto.getPrecioVenta() != null)
			entity.setPrecioVenta(dto.getPrecioVenta());
		if (dto.getPorcentaje() != null)
			entity.setPorcentaje(dto.getPorcentaje());
		if (dto.getDescripcion() != null)
			entity.setDescripcion(dto.getDescripcion());
		if (dto.getActivo() != null)
			entity.setActivo(dto.getActivo());
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEnterpriseId(Integer idEmpresa, Integer cantidad, Double precioUnitario,
			Double precioVenta, Integer porcentaje, String codigo, String nombre, String descripcion,
			String descripcionProducto, String categoriaNombre, Pageable pageable) {

		log.info(
				"Buscar inventario empresa={}, filtros: cantidad={}, pUnit={}, pVenta={}, %={}, codigo={}, nombre={}, descInv={}, descProd={}, categoria={}",
				idEmpresa, cantidad, precioUnitario, precioVenta, porcentaje, codigo, nombre, descripcion,
				descripcionProducto, categoriaNombre);

		try {
			if (idEmpresa == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("Parámetro requerido: idEmpresa").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			Sort defaultSort = Sort.by(Sort.Order.asc("producto.nombre"), Sort.Order.desc("fechaCreacion"),
					Sort.Order.desc("id"));

			Pageable pageToUse = (pageable == null) ? PageRequest.of(0, 20, defaultSort)
					: (pageable.getSort().isUnsorted()
							? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort)
							: pageable);

			List<InventarioEntity> baseCheck = inventarioRepository.findByProducto_Empresa_Id(idEmpresa);
			if (baseCheck.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron inventarios para la empresa con id " + idEmpresa)
								.code(HttpStatus.NOT_FOUND.value()).response(List.of()).totalCount(0L)
								.pageSize(pageToUse.getPageSize()).currentPage(pageToUse.getPageNumber()).totalPages(0)
								.build());
			}

			Specification<InventarioEntity> spec = Specification.allOf(
					InventarioSpecifications.perteneceAEmpresa(idEmpresa),
					InventarioSpecifications.cantidadEquals(cantidad),
					InventarioSpecifications.precioUnitarioEquals(precioUnitario),
					InventarioSpecifications.precioVentaEquals(precioVenta),
					InventarioSpecifications.porcentajeEquals(porcentaje),
					InventarioSpecifications.codigoProductoLike(codigo),
					InventarioSpecifications.nombreProductoLike(nombre),
					InventarioSpecifications.descripcionLike(descripcion),
					InventarioSpecifications.descripcionProductoLike(descripcionProducto),
					InventarioSpecifications.categoriaNombreLike(categoriaNombre));

			Page<InventarioEntity> page = inventarioRepository.findAll(spec, pageToUse);

			if (page.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron inventarios para la empresa con id " + idEmpresa)
								.code(HttpStatus.NOT_FOUND.value()).response(List.of()).totalCount(0L)
								.pageSize(pageToUse.getPageSize()).currentPage(pageToUse.getPageNumber()).totalPages(0)
								.build());
			}

			// ===== Mapeo a DTO de respuesta (incluye datos del producto) =====
			List<InventarioResponseDTO> dtoList = page.getContent().stream().map(entity -> {
				var prod = entity.getProducto();
				String categoria = null;
				if (prod != null && prod.getCategoria() != null) {
					categoria = prod.getCategoria().getNombre();
				}
				return InventarioResponseDTO.builder().id(entity.getId()).productoId(prod != null ? prod.getId() : null)
						.codigo(prod != null ? prod.getCodigo() : null).nombre(prod != null ? prod.getNombre() : null)
						.descripcionProducto(prod != null ? prod.getDescripcion() : null).categoriaNombre(categoria)
						.cantidad(entity.getCantidad()).precioUnitario(entity.getPrecioUnitario())
						.precioVenta(entity.getPrecioVenta()).porcentaje(entity.getPorcentaje())
						.descripcion(entity.getDescripcion()).activo(entity.getActivo())
						.fechaCreacion(entity.getFechaCreacion()).build();
			}).toList();

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).totalCount(page.getTotalElements())
					.pageSize(page.getSize()).currentPage(page.getNumber()).totalPages(page.getTotalPages()).build());

		} catch (Exception e) {
			log.error("Error al buscar inventario por id de empresa: {}", idEmpresa, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
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
				ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
						.code(HttpStatus.OK.value()).response(dto).build();
				return ResponseEntity.ok(responseDTO);
			} else {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
		} catch (Exception e) {
			log.error("Error al buscar inventario por id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
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
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar los inventarios", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar inventario por id: {}", id);
		try {
			if (!inventarioRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			inventarioRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar inventario con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}
}
