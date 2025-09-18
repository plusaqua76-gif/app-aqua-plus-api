package com.aqua.plus.api.service.impl;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IDeudaClienteService;
import com.aqua.plus.api.service.impl.specification.DeudaClienteSpecifications;
import com.aqua.plus.commons.dtos.DeudaClienteDTO;
import com.aqua.plus.commons.dtos.DeudaClienteResponseDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.DeudaClienteEntity;
import com.aqua.plus.commons.maps.DeudaClienteMapper;
import com.aqua.plus.commons.maps.DeudaClienteResponseMapper;
import com.aqua.plus.commons.repositories.AbonoRepository;
import com.aqua.plus.commons.repositories.DeudaClienteRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeudaClienteServiceImpl implements IDeudaClienteService {

	private final DeudaClienteRepository deudaClienteRepository;
	private final DeudaClienteMapper deudaClienteMapper;
	private final DeudaClienteResponseMapper deudaClienteResponseMapper;
	private final AbonoRepository abonoRepository;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(DeudaClienteDTO deudaClienteDTO) {
		log.info("Inicio metodo de Guardar deuda de Cliente");
		try {
			boolean isUpdate = deudaClienteDTO.getId() != null
					&& deudaClienteRepository.existsById(deudaClienteDTO.getId());
			DeudaClienteEntity entity;

			if (isUpdate) {
				entity = deudaClienteRepository.findById(deudaClienteDTO.getId()).orElseThrow();
				deudaClienteMapper.updateEntityFromDto(deudaClienteDTO, entity);
				entity.setFechaModificacion(new Date());
				entity.setUsuarioModificacion(deudaClienteDTO.getUsuarioModificacion());
			} else {
				entity = deudaClienteMapper.dtoToEntity(deudaClienteDTO);
				entity.setFechaCreacion(new Date());
				entity.setUsuarioCreacion(deudaClienteDTO.getUsuarioCreacion());
				entity.setActivo(true);
			}

			DeudaClienteEntity saved = deudaClienteRepository.save(entity);
			DeudaClienteDTO savedDTO = deudaClienteMapper.entityToDto(saved);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(message).code(statusCode)
					.response(savedDTO).build();
			log.info("Fin del metodo guardar deuda de cliente");
			return ResponseEntity.status(statusCode).body(responseDTO);
		} catch (Exception e) {
			log.error("Error guardando Deuda de Cliente", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
		log.info("Buscar Deuda de Cliente por id: {}", id);
		try {
			Optional<DeudaClienteEntity> deudaCliente = deudaClienteRepository.findById(id);
			if (deudaCliente.isPresent()) {
				DeudaClienteDTO dto = deudaClienteMapper.entityToDto(deudaCliente.get());
				ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
						.code(HttpStatus.OK.value()).response(dto).build();
				return ResponseEntity.ok(responseDTO);
			} else {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
		} catch (Exception e) {
			log.error("Error al buscar Deuda de Cliente por id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByIdEnterprise(Integer idEmpresa, String clienteNombreLike,
			String facturaCodigoLike, String descripcionLike, LocalDate fechaDeuda, Double valor,
			String tipoDeudaNombre, String plazoPagoNombre, Pageable pageable) {
		log.info(
				"Listar DeudaCliente por empresaId={} con filtros: clienteNombreLike={}, facturaCodigoLike={}, descripcionLike={}, fechaDeuda={}, valor={}, tipoDeudaNombre={}, plazoPagoNombre={}",
				idEmpresa, clienteNombreLike, facturaCodigoLike, descripcionLike, fechaDeuda, valor, tipoDeudaNombre,
				plazoPagoNombre);

		try {
			if (idEmpresa == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("idEmpresa es obligatorio").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			// Construcción de Specifications (empresaId es obligatorio)
			Specification<DeudaClienteEntity> spec = DeudaClienteSpecifications.allOfNonNull(
					DeudaClienteSpecifications.perteneceAEmpresa(idEmpresa),
					DeudaClienteSpecifications.fechaDeudaIgual(fechaDeuda),
					DeudaClienteSpecifications.valorIgual(valor),
					DeudaClienteSpecifications.descripcionLike(descripcionLike),
					DeudaClienteSpecifications.facturaCodigoLike(facturaCodigoLike),
					DeudaClienteSpecifications.clienteNombreLike(clienteNombreLike),
					DeudaClienteSpecifications.tipoDeudaNombreLike(tipoDeudaNombre),
					DeudaClienteSpecifications.plazoPagoNombreLike(plazoPagoNombre));

			Pageable pageToUse = (pageable != null) ? pageable : Pageable.unpaged();

			// DISTINCT para evitar duplicados por los joins
			Page<DeudaClienteEntity> page = deudaClienteRepository.findAll((root, cq, cb) -> {
				cq.distinct(true);
				return (spec == null) ? cb.conjunction() : spec.toPredicate(root, cq, cb);
			}, pageToUse);

			List<DeudaClienteEntity> entities = page.getContent();
			List<DeudaClienteResponseDTO> dtos = deudaClienteResponseMapper.toDtoList(entities);

			long totalCount = page.getTotalElements();
			int pageSize = page.getSize();
			int currentPage = page.getNumber();
			int totalPages = page.getTotalPages();

			if (entities.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false).message("No se encontraron deudas para la empresa")
								.code(HttpStatus.NOT_FOUND.value()).response(List.of()).totalCount(totalCount)
								.pageSize(pageSize).currentPage(currentPage).totalPages(totalPages).build());
			}

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtos).totalCount(totalCount).pageSize(pageSize)
					.currentPage(currentPage).totalPages(totalPages).build());

		} catch (Exception e) {
			log.error("Error al buscar DeudaCliente por empresaId={}", idEmpresa, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todos las Deudas de Cliente");
		try {
			var list = deudaClienteRepository.findAll();
			var filteredList = list.stream().filter(deuda -> deuda.getValor() != null && deuda.getValor() > 0).toList();
			var dtoList = deudaClienteMapper.listEntityToDtoList(filteredList);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar las Deudas de Cliente", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar Deuda de Cliente por id: {}", id);
		try {
			if (!deudaClienteRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			abonoRepository.deleteByDeudaClienteId(id);
			deudaClienteRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar Deuda de Cliente con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> updateDeuda(DeudaClienteDTO deudaClienteDTO) {
		log.info("Inicio del método para actualizar Deuda de Cliente");

		try {
			if (deudaClienteDTO.getId() == null) {
				log.warn("ID de la deuda es nulo. No se puede actualizar.");
				ResponseDTO responseDTO = ResponseDTO.builder().success(false)
						.message("El ID de la deuda es requerido para actualizar.").code(HttpStatus.BAD_REQUEST.value())
						.build();
				return ResponseEntity.badRequest().body(responseDTO);
			}

			Optional<DeudaClienteEntity> optionalEntity = deudaClienteRepository.findById(deudaClienteDTO.getId());

			if (optionalEntity.isEmpty()) {
				log.warn("No se encontró la Deuda de Cliente con ID: {}", deudaClienteDTO.getId());
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}

			DeudaClienteEntity entity = optionalEntity.get();

			deudaClienteMapper.updateEntityFromDto(deudaClienteDTO, entity);

			entity.setFechaModificacion(new Date());
			entity.setUsuarioModificacion(deudaClienteDTO.getUsuarioModificacion());

			DeudaClienteEntity updatedEntity = deudaClienteRepository.save(entity);
			DeudaClienteDTO updatedDTO = deudaClienteMapper.entityToDto(updatedEntity);

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.UPDATED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(updatedDTO).build();

			log.info("Deuda de Cliente actualizada exitosamente");
			return ResponseEntity.ok(responseDTO);

		} catch (Exception e) {
			log.error("Error actualizando Deuda de Cliente", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.UPDATE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

}
