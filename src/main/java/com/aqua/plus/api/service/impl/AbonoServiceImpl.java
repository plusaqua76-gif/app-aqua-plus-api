package com.aqua.plus.api.service.impl;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IAbonoService;
import com.aqua.plus.api.service.impl.specification.AbonoSpecifications;
import com.aqua.plus.commons.dtos.AbonoDTO;
import com.aqua.plus.commons.dtos.AbonoResponseDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.AbonoEntity;
import com.aqua.plus.commons.entities.DeudaClienteEntity;
import com.aqua.plus.commons.maps.AbonoMapper;
import com.aqua.plus.commons.repositories.AbonoRepository;
import com.aqua.plus.commons.repositories.DeudaClienteRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbonoServiceImpl implements IAbonoService {

	private final AbonoRepository abonoRepository;
	private final AbonoMapper abonoMapper;
	private final DeudaClienteRepository deudaClienteRepository;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(AbonoDTO abonoDTO) {
		log.info("Inicio metodo crear abono");
		try {
			Integer deudaId = abonoDTO.getDeudaCliente().getId();
			DeudaClienteEntity deuda = deudaClienteRepository.findById(deudaId)
					.orElseThrow(() -> new RuntimeException("Deuda no encontrada con ID: " + deudaId));

			if (abonoDTO.getValor() > deuda.getValor()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
						.message(Constantes.MAYOR_VALUE).code(HttpStatus.BAD_REQUEST.value()).build());
			}
			double nuevoValorDeuda = deuda.getValor() - abonoDTO.getValor();
			deuda.setValor(nuevoValorDeuda);
			deuda.setFechaModificacion(new Date());
			deuda.setUsuarioModificacion(abonoDTO.getUsuarioCreacion());
			deudaClienteRepository.save(deuda);
			AbonoEntity entity = abonoMapper.dtoToEntity(abonoDTO);
			entity.setFechaCreacion(new Date());
			entity.setUsuarioCreacion(abonoDTO.getUsuarioCreacion());
			entity.setActivo(true);
			AbonoEntity saved = abonoRepository.save(entity);
			AbonoDTO savedDTO = abonoMapper.entityToDto(saved);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.SAVED_SUCCESSFULLY)
					.code(HttpStatus.CREATED.value()).response(savedDTO).build();
			log.info("Finalizo metodo crear abono");
			return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
		} catch (Exception e) {
			log.error("Error creando el abono ", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> update(AbonoDTO abonoDTO) {
		log.info("inicio metodo Actualizando abono");
		try {
			if (abonoDTO.getId() == null || !abonoRepository.existsById(abonoDTO.getId())) {
				throw new IllegalArgumentException(Constantes.ABONO_NOT_EXIST);
			}

			AbonoEntity entity = abonoRepository.findById(abonoDTO.getId()).orElseThrow();
			abonoMapper.updateEntityFromDto(abonoDTO, entity);
			entity.setFechaModificacion(new Date());
			entity.setUsuarioModificacion(abonoDTO.getUsuarioModificacion());

			AbonoEntity updated = abonoRepository.save(entity);
			AbonoDTO updatedDTO = abonoMapper.entityToDto(updated);

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.UPDATED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(updatedDTO).build();

			return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
		} catch (Exception e) {
			log.error("Error actualizando el abono", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.UPDATE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
		log.info("Buscar abono por id: {}", id);
		try {
			Optional<AbonoEntity> abono = abonoRepository.findById(id);
			if (abono.isPresent()) {
				AbonoDTO dto = abonoMapper.entityToDto(abono.get());
				ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
						.code(HttpStatus.OK.value()).response(dto).build();
				return ResponseEntity.ok(responseDTO);
			} else {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
		} catch (Exception e) {
			log.error("Error al buscar  el abono por id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> listarAbonosPorIdEmpresa(Integer idEmpresa, String clienteLike,
			String codigoFactura, LocalDate fecha, Double valor, Pageable pageable) {

		log.info(
				"Listar abonos (resumen) por empresaId={} filtros: clienteLike={}, codigoFacturaLike={}, fecha={}, valor={}",
				idEmpresa, clienteLike, codigoFactura, fecha, valor);

		try {
			if (idEmpresa == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("idEmpresa es obligatorio").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			Specification<AbonoEntity> spec = Specification.allOf(Stream.of(AbonoSpecifications.porIdEmpresa(idEmpresa),
					AbonoSpecifications.activoIgual(Boolean.TRUE), AbonoSpecifications.fechaIgual(fecha),
					AbonoSpecifications.valorIgual(valor), AbonoSpecifications.clienteLike(clienteLike),
					AbonoSpecifications.codigoFactura(codigoFactura)).filter(Objects::nonNull).toList());

			Pageable pageToUse = (pageable != null) ? pageable
					: PageRequest.of(0, 20, Sort.by("fechaCreacion").descending());

			Page<AbonoEntity> pageResult = abonoRepository.findAll((root, cq, cb) -> {
				cq.distinct(true);
				return (spec == null) ? cb.conjunction() : spec.toPredicate(root, cq, cb);
			}, pageToUse);

			List<AbonoResponseDTO> items = pageResult.getContent().stream().map(a -> {
				var dc = a.getDeudaCliente();
				var ecc = (dc != null ? dc.getEmpresaClienteContador() : null);
				var pxx = (ecc != null ? ecc.getCliente() : null);
				var f = (dc != null ? dc.getFactura() : null);

				String clienteNombre = String
						.join(" ", (pxx != null && pxx.getNombre() != null) ? pxx.getNombre() : "",
								(pxx != null && pxx.getSegundoNombre() != null) ? pxx.getSegundoNombre() : "",
								(pxx != null && pxx.getApellido() != null) ? pxx.getApellido() : "",
								(pxx != null && pxx.getSegundoApellido() != null) ? pxx.getSegundoApellido() : "")
						.replaceAll("\\s+", " ").trim();

				AbonoResponseDTO dto = new AbonoResponseDTO();
				dto.setCliente(clienteNombre);
				dto.setCodigoFactura(f != null ? f.getCodigo() : null);
				dto.setFechaAbono(a.getFechaCreacion());
				dto.setValorAbono(a.getValor());
				return dto;
			}).toList();

			long totalCount = pageResult.getTotalElements();
			int pageSize = pageResult.getSize();
			int currentPage = pageResult.getNumber();
			int totalPages = pageResult.getTotalPages();

			if (items.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron abonos para la empresa con id " + idEmpresa)
								.code(HttpStatus.NOT_FOUND.value()).response(items).totalCount(totalCount)
								.pageSize(pageSize).currentPage(currentPage).totalPages(totalPages).build());
			}

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(items).totalCount(totalCount).pageSize(pageSize)
					.currentPage(currentPage).totalPages(totalPages).build());

		} catch (Exception e) {
			log.error("Error listando abonos (resumen) por empresa {}", idEmpresa, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message(Constantes.CONSULTING_ERROR).code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todos los abonos");
		try {
			var list = abonoRepository.findAll();
			var dtoList = abonoMapper.listEntityToDtoList(list);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar los abonos", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar abono por id: {}", id);
		try {
			if (!abonoRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			abonoRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar abono con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}
}
