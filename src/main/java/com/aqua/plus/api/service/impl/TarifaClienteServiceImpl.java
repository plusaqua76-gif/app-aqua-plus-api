package com.aqua.plus.api.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.ITarifaClienteService;
import com.aqua.plus.commons.dtos.PersonaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.TarifaClienteDTO;
import com.aqua.plus.commons.dtos.TipoTarifaDTO;
import com.aqua.plus.commons.entities.PersonaEntity;
import com.aqua.plus.commons.entities.TarifaClienteEntity;
import com.aqua.plus.commons.entities.TipoTarifaEntity;
import com.aqua.plus.commons.repositories.PersonaRepository;
import com.aqua.plus.commons.repositories.TarifaClienteRepository;
import com.aqua.plus.commons.repositories.TipoTarifaRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TarifaClienteServiceImpl implements ITarifaClienteService {

	private final TarifaClienteRepository tarifaClienteRepository;
	private final PersonaRepository personaRepository;
	private final TipoTarifaRepository tipoTarifaRepository;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(List<TarifaClienteDTO> dtos) {
		log.info("Guardar/actualizar tarifas de cliente. numTarifas={}", (dtos != null ? dtos.size() : 0));

		try {
			if (dtos == null || dtos.isEmpty()) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false).message("Debe enviar al menos una tarifa de cliente")
								.code(HttpStatus.BAD_REQUEST.value()).build());
			}

			TarifaClienteDTO first = dtos.stream().filter(Objects::nonNull).findFirst().orElse(null);

			if (first == null || first.getCliente() == null || first.getCliente().getId() == null) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false)
								.message("El cliente y su id son requeridos en al menos un registro")
								.code(HttpStatus.BAD_REQUEST.value()).build());
			}

			Integer clienteId = first.getCliente().getId();

			boolean allSameCliente = dtos.stream().filter(Objects::nonNull)
					.allMatch(dto -> dto.getCliente() != null && Objects.equals(clienteId, dto.getCliente().getId()));

			if (!allSameCliente) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false)
								.message("Todas las tarifas deben pertenecer al mismo cliente")
								.code(HttpStatus.BAD_REQUEST.value()).build());
			}

			PersonaEntity cliente = personaRepository.findById(clienteId)
					.orElseThrow(() -> new IllegalArgumentException("No existe el cliente con id " + clienteId));

			List<Integer> tipoTarifaIds = dtos.stream().filter(Objects::nonNull).map(TarifaClienteDTO::getTipoTarifa)
					.filter(Objects::nonNull).map(TipoTarifaDTO::getId).filter(Objects::nonNull).distinct().toList();

			if (tipoTarifaIds.isEmpty()) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false)
								.message("Cada tarifa debe tener un tipo de tarifa con id")
								.code(HttpStatus.BAD_REQUEST.value()).build());
			}

			Map<Integer, TipoTarifaEntity> tipoTarifaMap = tipoTarifaRepository.findAllById(tipoTarifaIds).stream()
					.collect(Collectors.toMap(TipoTarifaEntity::getId, tt -> tt));

			List<Integer> faltantes = tipoTarifaIds.stream().filter(id -> !tipoTarifaMap.containsKey(id)).toList();

			if (!faltantes.isEmpty()) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false)
								.message("Existen idTipoTarifa inexistentes: " + faltantes)
								.code(HttpStatus.BAD_REQUEST.value()).build());
			}

			List<TarifaClienteEntity> existentes = tarifaClienteRepository.findByCliente_Id(clienteId);

			Map<Integer, TarifaClienteEntity> existentesPorId = existentes.stream().filter(tc -> tc.getId() != null)
					.collect(Collectors.toMap(TarifaClienteEntity::getId, tc -> tc, (a, b) -> a));

			Map<Integer, TarifaClienteEntity> existentesPorTipoTarifa = existentes.stream()
					.filter(tc -> tc.getTipoTarifa() != null && tc.getTipoTarifa().getId() != null)
					.collect(Collectors.toMap(tc -> tc.getTipoTarifa().getId(), tc -> tc, (a, b) -> a));

			Date ahora = new Date();
			String usuario = first.getUsuarioModificacion() != null ? first.getUsuarioModificacion()
					: (first.getUsuarioCreacion() != null ? first.getUsuarioCreacion() : "SYSTEM");

			List<TarifaClienteEntity> paraGuardar = new ArrayList<>();

			for (TarifaClienteDTO dto : dtos) {
				if (dto == null)
					continue;

				Integer dtoId = dto.getId();
				Integer tipoTarifaId = (dto.getTipoTarifa() != null ? dto.getTipoTarifa().getId() : null);
				if (tipoTarifaId == null) {
					log.warn("Se encontró TarifaClienteDTO sin tipoTarifaId, se omite. dto={}", dto);
					continue;
				}

				Boolean aplica = dto.getAplica() != null ? dto.getAplica() : Boolean.FALSE;

				TarifaClienteEntity entity = null;

				if (dtoId != null) {
					entity = existentesPorId.get(dtoId);
					if (entity != null && entity.getCliente() != null
							&& !Objects.equals(entity.getCliente().getId(), clienteId)) {
						log.warn("Se intentó actualizar una tarifa de otro cliente. dtoId={}, clienteId={}", dtoId,
								clienteId);
						entity = null;
					}
				}

				if (entity == null) {
					entity = existentesPorTipoTarifa.get(tipoTarifaId);
				}

				if (entity != null) {
					entity.setAplica(aplica);
					if (dto.getActivo() != null) {
						entity.setActivo(dto.getActivo());
					}
					entity.setUsuarioModificacion(usuario);
					entity.setFechaModificacion(ahora);
				} else {
					entity = new TarifaClienteEntity();
					entity.setCliente(cliente);
					entity.setTipoTarifa(tipoTarifaMap.get(tipoTarifaId));
					entity.setAplica(aplica);
					entity.setActivo(dto.getActivo() != null ? dto.getActivo() : Boolean.TRUE);
					entity.setUsuarioCreacion(usuario);
					entity.setFechaCreacion(ahora);
				}

				paraGuardar.add(entity);
			}

			if (paraGuardar.isEmpty()) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false)
								.message("No se encontró ninguna tarifa válida para guardar")
								.code(HttpStatus.BAD_REQUEST.value()).build());
			}

			List<TarifaClienteEntity> guardados = tarifaClienteRepository.saveAll(paraGuardar);

			List<TarifaClienteDTO> respuesta = guardados
					.stream().map(
							tc -> TarifaClienteDTO
									.builder().id(
											tc.getId())
									.cliente(tc.getCliente() != null
											? PersonaDTO.builder().id(tc.getCliente().getId()).build()
											: null)
									.tipoTarifa(
											tc.getTipoTarifa() != null
													? TipoTarifaDTO.builder().id(tc.getTipoTarifa().getId())
															.nombre(tc.getTipoTarifa().getNombre()).build()
													: null)
									.aplica(tc.getAplica()).activo(tc.getActivo())
									.usuarioCreacion(tc.getUsuarioCreacion()).fechaCreacion(tc.getFechaCreacion())
									.usuarioModificacion(tc.getUsuarioModificacion())
									.fechaModificacion(tc.getFechaModificacion()).build())
					.toList();

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.SAVED_SUCCESSFULLY)
					.code(HttpStatus.CREATED.value()).response(respuesta).build();

			return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);

		} catch (Exception e) {
			log.error("Error al guardar tarifas de cliente", e);

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

			ResponseDTO errorResponse = ResponseDTO.builder().success(false)
					.message("Error guardando tarifas de cliente: "
							+ (rootCauseMessage != null ? rootCauseMessage : "ver detalle en 'response'"))
					.code(HttpStatus.BAD_REQUEST.value()).response(errorInfo).build();

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio eliminar tarifa cliente por id: {}", id);
		try {
			if (!tarifaClienteRepository.existsById(id)) {
				String notFoundMsg = String.format(Constantes.ROL_NOT_FOUND, id);
				log.warn(notFoundMsg);
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).code(HttpStatus.NOT_FOUND.value())
						.message(notFoundMsg).response(null).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}

			tarifaClienteRepository.deleteById(id);
			log.info("Tarifa cliente eliminado correctamente para el Id: {}", id);

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(null).build();

			return ResponseEntity.ok(responseDTO);

		} catch (Exception e) {
			log.error("Error al eliminar rol", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(e).build();

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}

}
