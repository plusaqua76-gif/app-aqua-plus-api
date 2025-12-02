package com.aqua.plus.api.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.ITarifaContadorService;
import com.aqua.plus.commons.dtos.EmpresaClienteContadorDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.TarifaContadorDTO;
import com.aqua.plus.commons.dtos.TipoTarifaDTO;
import com.aqua.plus.commons.entities.EmpresaClienteContadorEntity;
import com.aqua.plus.commons.entities.TarifaContadorEntity;
import com.aqua.plus.commons.entities.TipoTarifaEntity;
import com.aqua.plus.commons.maps.TarifaContadorMapper;
import com.aqua.plus.commons.repositories.EmpresaClienteContadorRepository;
import com.aqua.plus.commons.repositories.TarifaContadorRepository;
import com.aqua.plus.commons.repositories.TipoTarifaRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TarifaContadorServiceImpl implements ITarifaContadorService {

	private final TarifaContadorRepository tarifaContadorRepository;
	private final TarifaContadorMapper tarifaContadorMapper;
	private final EmpresaClienteContadorRepository empresaClienteContadorRepository;
	private final TipoTarifaRepository tipoTarifaRepository;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(List<TarifaContadorDTO> dtos) {
		log.info("Guardar/actualizar tarifas del contador. numTarifas={}", (dtos != null ? dtos.size() : 0));

		try {
			if (dtos == null || dtos.isEmpty()) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false)
								.message("Debe enviar al menos una tarifa del contador")
								.code(HttpStatus.BAD_REQUEST.value()).build());
			}

			TarifaContadorDTO first = dtos.stream().filter(Objects::nonNull).findFirst().orElse(null);

			if (first == null || first.getEmpresaClienteContador() == null
					|| first.getEmpresaClienteContador().getId() == null) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false)
								.message("El contador y su id son requeridos en al menos un registro")
								.code(HttpStatus.BAD_REQUEST.value()).build());
			}

			Integer contadorId = first.getEmpresaClienteContador().getId();

			boolean allSameCliente = dtos.stream().filter(Objects::nonNull)
					.allMatch(dto -> dto.getEmpresaClienteContador() != null
							&& Objects.equals(contadorId, dto.getEmpresaClienteContador().getId()));

			if (!allSameCliente) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false)
								.message("Todas las tarifas deben pertenecer al mismo contador")
								.code(HttpStatus.BAD_REQUEST.value()).build());
			}

			EmpresaClienteContadorEntity contador = empresaClienteContadorRepository.findById(contadorId)
					.orElseThrow(() -> new IllegalArgumentException("No existe el contador con id " + contadorId));

			List<Integer> tipoTarifaIds = dtos.stream().filter(Objects::nonNull).map(TarifaContadorDTO::getTipoTarifa)
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

			List<TarifaContadorEntity> existentes = tarifaContadorRepository
					.findByEmpresaClienteContador_Id(contadorId);

			Map<Integer, TarifaContadorEntity> existentesPorId = existentes.stream().filter(tc -> tc.getId() != null)
					.collect(Collectors.toMap(TarifaContadorEntity::getId, tc -> tc, (a, b) -> a));

			Map<Integer, TarifaContadorEntity> existentesPorTipoTarifa = existentes.stream()
					.filter(tc -> tc.getTipoTarifa() != null && tc.getTipoTarifa().getId() != null)
					.collect(Collectors.toMap(tc -> tc.getTipoTarifa().getId(), tc -> tc, (a, b) -> a));

			Date ahora = new Date();
			String usuario = first.getUsuarioModificacion() != null ? first.getUsuarioModificacion()
					: (first.getUsuarioCreacion() != null ? first.getUsuarioCreacion() : "SYSTEM");

			List<TarifaContadorEntity> paraGuardar = new ArrayList<>();

			for (TarifaContadorDTO dto : dtos) {
				if (dto == null)
					continue;

				Integer dtoId = dto.getId();
				Integer tipoTarifaId = (dto.getTipoTarifa() != null ? dto.getTipoTarifa().getId() : null);
				if (tipoTarifaId == null) {
					log.warn("Se encontró TarifaContadorDTO sin tipoTarifaId, se omite. dto={}", dto);
					continue;
				}

				Boolean aplica = dto.getAplica() != null ? dto.getAplica() : Boolean.FALSE;

				TarifaContadorEntity entity = null;

				if (dtoId != null) {
					entity = existentesPorId.get(dtoId);
					if (entity != null && entity.getEmpresaClienteContador() != null
							&& !Objects.equals(entity.getEmpresaClienteContador().getId(), contadorId)) {
						log.warn("Se intentó actualizar una tarifa de otro contador. dtoId={}, contadorId={}", dtoId,
								contadorId);
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
					entity = new TarifaContadorEntity();
					entity.setEmpresaClienteContador(contador);
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

			List<TarifaContadorEntity> guardados = tarifaContadorRepository.saveAll(paraGuardar);

			List<TarifaContadorDTO> respuesta = guardados
					.stream().map(
							tc -> TarifaContadorDTO
									.builder().id(
											tc.getId())
									.empresaClienteContador(tc.getEmpresaClienteContador() != null
											? EmpresaClienteContadorDTO.builder()
													.id(tc.getEmpresaClienteContador().getId()).build()
											: null)
									.tipoTarifa(tc.getTipoTarifa() != null
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
			log.error("Error al guardar tarifas del contador", e);

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
			if (!tarifaContadorRepository.existsById(id)) {
				String notFoundMsg = String.format(Constantes.ROL_NOT_FOUND, id);
				log.warn(notFoundMsg);
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).code(HttpStatus.NOT_FOUND.value())
						.message(notFoundMsg).response(null).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}

			tarifaContadorRepository.deleteById(id);
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

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByIdEmpresaClienteContador(Integer idEmpresaClienteContador) {
		log.info("Buscar TarifaContador (lista) por idEmpresaClienteContador: {}", idEmpresaClienteContador);

		try {
			if (idEmpresaClienteContador == null) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false).message("El idEmpresaClienteContador es obligatorio")
								.code(HttpStatus.BAD_REQUEST.value()).response(null).build());
			}

			var list = tarifaContadorRepository.findByEmpresaClienteContador_Id(idEmpresaClienteContador);

			if (list == null || list.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.builder().success(false)
						.message("No se encontraron tarifas para EmpresaClienteContador id " + idEmpresaClienteContador)
						.code(HttpStatus.NOT_FOUND.value()).response(List.of()).build());
			}

			List<TarifaContadorDTO> dtos = list.stream().map(tarifaContadorMapper::entityToDto).toList();

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtos).build());

		} catch (Exception e) {
			log.error("Error al buscar TarifaContador por idEmpresaClienteContador: {}", idEmpresaClienteContador, e);

			Map<String, Object> errorPayload = new HashMap<>();
			errorPayload.put("errorMessage", e.getMessage());
			errorPayload.put("exception", e.getClass().getSimpleName());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(errorPayload).build());
		}
	}

}
