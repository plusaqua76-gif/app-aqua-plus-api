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

import com.aqua.plus.api.service.ICuentaService;
import com.aqua.plus.api.service.impl.specification.CuentaSpecifications;
import com.aqua.plus.commons.dtos.CategoriaCuentaDTO;
import com.aqua.plus.commons.dtos.CuentaDTO;
import com.aqua.plus.commons.dtos.EmpresaDTO;
import com.aqua.plus.commons.dtos.ParametrosGeneralesDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.TipoCuentaContableDTO;
import com.aqua.plus.commons.entities.CuentaEntity;
import com.aqua.plus.commons.maps.CuentaMapper;
import com.aqua.plus.commons.repositories.CuentaRepository;
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
public class CuentaServiceImpl implements ICuentaService {

	private final CuentaRepository cuentaRepository;
	private final CuentaMapper cuentaMapper;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(CuentaDTO cuentaDTO) {
		log.info("Guardar/Actualizar Cuenta ");
		try {
			boolean isUpdate = cuentaDTO.getId() != null && cuentaRepository.existsById(cuentaDTO.getId());
			CuentaEntity entity;

			if (isUpdate) {
				entity = cuentaRepository.findById(cuentaDTO.getId()).orElseThrow();
				cuentaMapper.updateEntityFromDto(cuentaDTO, entity);
				entity.setFechaModificacion(new Date());
				entity.setUsuarioModificacion(cuentaDTO.getUsuarioModificacion());
			} else {
				entity = cuentaMapper.dtoToEntity(cuentaDTO);
				entity.setFechaCreacion(new Date());
				entity.setUsuarioCreacion(cuentaDTO.getUsuarioCreacion());
				entity.setActivo(true);
			}

			CuentaEntity saved = cuentaRepository.save(entity);
			CuentaDTO savedDTO = cuentaMapper.entityToDto(saved);

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
	public ResponseEntity<ResponseDTO> findById(Integer id) {
		log.info("Buscar cuenta por id: {}", id);
		try {
			Optional<CuentaEntity> cuenta = cuentaRepository.findById(id);
			if (cuenta.isPresent()) {
				CuentaDTO dto = cuentaMapper.entityToDto(cuenta.get());
				ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
						.code(HttpStatus.OK.value()).response(dto).build();
				return ResponseEntity.ok(responseDTO);
			} else {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
		} catch (Exception e) {
			log.error("Error al buscar cuenta por id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todas las cuentas");
		try {
			var list = cuentaRepository.findAll();
			var dtoList = cuentaMapper.listEntityToDtoList(list);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar las cuentas", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEmpresa(Integer idEmpresa, String cuentaCodigo, String cuentaNombre,
			Double cuentaValor, String tipoNombre, String tipoNaturaleza, Pageable pageable) {
		log.info("Buscar cuentas por empresa={}, filtros: codigo={}, nombre={}, valor={}, tipoNombre={}, naturaleza={}",
				idEmpresa, cuentaCodigo, cuentaNombre, cuentaValor, tipoNombre, tipoNaturaleza);

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

			Specification<CuentaEntity> spec = Specification.allOf(CuentaSpecifications.empresaIdEquals(idEmpresa),
					CuentaSpecifications.codigoLike(cuentaCodigo), CuentaSpecifications.nombreLike(cuentaNombre),
					CuentaSpecifications.valorEquals(cuentaValor), CuentaSpecifications.tipoNombreLike(tipoNombre),
					CuentaSpecifications.tipoNaturalezaLike(tipoNaturaleza));

			Page<CuentaEntity> page = cuentaRepository.findAll(spec, pageToUse);

			if (page.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron cuentas para la empresa indicada")
								.code(HttpStatus.NOT_FOUND.value()).response(List.of()).totalCount(0L)
								.pageSize(pageToUse.getPageSize()).currentPage(pageToUse.getPageNumber()).totalPages(0)
								.build());
			}

			List<CuentaDTO> content = page.getContent().stream().map(cuentaMapper::entityToDto).toList();

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(content).totalCount(page.getTotalElements())
					.pageSize(page.getSize()).currentPage(page.getNumber()).totalPages(page.getTotalPages()).build());

		} catch (Exception e) {
			log.error("Error al buscar cuentas por empresa {}", idEmpresa, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar cuenta por id: {}", id);
		try {
			if (!cuentaRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			cuentaRepository.deleteById(id);
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
	
	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findCuentas(Integer idEmpresa, Date fechaInicio, Date fechaFin,
			Integer page, Integer size) {

		log.info("Consultar cuentas: idEmpresa={}, fechaInicio={}, fechaFin={}, page={}, size={}",
				idEmpresa, fechaInicio, fechaFin, page, size);

		try {
			if (idEmpresa == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder()
						.success(false)
						.message("Parámetro requerido: idEmpresa")
						.code(HttpStatus.BAD_REQUEST.value())
						.build());
			}

			int pageNumber = (page == null || page < 0) ? 0 : page;
			int pageSize = (size == null || size <= 0) ? 10 : size;
			Pageable pageable = PageRequest.of(pageNumber, pageSize);

			Page<CuentaEntity> resultPage;

			if (fechaInicio != null && fechaFin != null) {
				if (fechaInicio.after(fechaFin)) {
					return ResponseEntity.badRequest().body(ResponseDTO.builder()
							.success(false)
							.message("fechaInicio no puede ser mayor que fechaFin")
							.code(HttpStatus.BAD_REQUEST.value())
							.build());
				}

				resultPage = cuentaRepository
						.findByEmpresa_IdAndFechaCreacionBetweenOrderByFechaCreacionDesc(idEmpresa, fechaInicio, fechaFin, pageable);

			} else {
				resultPage = cuentaRepository
						.findByEmpresa_IdOrderByFechaCreacionDesc(idEmpresa, pageable);
			}

			if (resultPage == null || resultPage.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.builder()
						.success(false)
						.message("No se encontraron cuentas para los filtros indicados")
						.code(HttpStatus.NOT_FOUND.value())
						.totalCount(0L)
						.response(List.of())
						.build());
			}

			List<CuentaDTO> dtos = resultPage.getContent().stream().map(c -> {

				EmpresaDTO empresaDTO = null;
				if (c.getEmpresa() != null) {
					var e = c.getEmpresa();
					empresaDTO = EmpresaDTO.builder()
							.id(e.getId())
							.build();
				}

				TipoCuentaContableDTO tipoCuentaDTO = null;
				if (c.getTipoCuenta() != null) {
					var t = c.getTipoCuenta();
					tipoCuentaDTO = TipoCuentaContableDTO.builder()
							.id(t.getId())
							.build();
				}

				CategoriaCuentaDTO categoriaCuentaDTO = null;
				if (c.getCategoriaCuenta() != null) {
					var cat = c.getCategoriaCuenta();
					categoriaCuentaDTO = CategoriaCuentaDTO.builder()
							.id(cat.getId())
							.nombre(cat.getNombre())
							.build();
				}

				ParametrosGeneralesDTO naturalezaDTO = null;
				if (c.getNaturaleza() != null) {
					var p = c.getNaturaleza();
					naturalezaDTO = ParametrosGeneralesDTO.builder()
							.id(p.getId())
							.build();
				}

				return CuentaDTO.builder()
						.id(c.getId())
						.empresa(empresaDTO)
						.tipoCuenta(tipoCuentaDTO)
						.categoriaCuenta(categoriaCuentaDTO)
						.naturaleza(naturalezaDTO)
						.codigo(c.getCodigo())
						.nombre(c.getNombre())
						.valor(c.getValor())
						.corriente(c.getCorriente())
						.activo(c.getActivo())
						.usuarioCreacion(c.getUsuarioCreacion())
						.fechaCreacion(c.getFechaCreacion())
						.usuarioModificacion(c.getUsuarioModificacion())
						.fechaModificacion(c.getFechaModificacion())
						.build();

			}).toList();

			return ResponseEntity.ok(ResponseDTO.builder()
					.success(true)
					.message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value())
					.response(dtos)
					.totalCount(resultPage.getTotalElements())
					.build());

		} catch (Exception e) {
			log.error("Error consultando cuentas", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder()
					.success(false)
					.message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.build());
		}
	}
}
