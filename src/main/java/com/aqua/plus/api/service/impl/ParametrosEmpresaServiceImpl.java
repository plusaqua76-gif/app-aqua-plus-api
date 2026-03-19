package com.aqua.plus.api.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aqua.plus.commons.enums.ParametroEmpresaEnum;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IParametrosEmpresaService;
import com.aqua.plus.commons.dtos.ParametrosEmpresaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ParametrosEmpresaEntity;
import com.aqua.plus.commons.maps.ParametrosEmpresaMapper;
import com.aqua.plus.commons.repositories.ParametrosEmpresaRepository;
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
public class ParametrosEmpresaServiceImpl implements IParametrosEmpresaService {

	private final ParametrosEmpresaRepository parametrosEmpresaRepository;
	private final ParametrosEmpresaMapper parametrosEmpresaMapper;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(ParametrosEmpresaDTO parametrosEmpresaDTO) {
		log.info("Inicio guardar/actualizar Parametros Empresa");
		try {
			boolean isUpdate = parametrosEmpresaDTO.getId() != null
					&& parametrosEmpresaRepository.existsById(parametrosEmpresaDTO.getId());

			ParametrosEmpresaEntity entity;
			if (isUpdate) {
				entity = parametrosEmpresaRepository.findById(parametrosEmpresaDTO.getId()).orElseThrow();
				parametrosEmpresaMapper.updateEntityFromDto(parametrosEmpresaDTO, entity);
				entity.setFechaModificacion(new Date());
				entity.setUsuarioModificacion(parametrosEmpresaDTO.getUsuarioModificacion());
			} else {
				entity = parametrosEmpresaMapper.dtoToEntity(parametrosEmpresaDTO);
				entity.setFechaCreacion(new Date());
				entity.setUsuarioCreacion(parametrosEmpresaDTO.getUsuarioCreacion());
				entity.setActivo(true);
			}

			parametrosEmpresaRepository.save(entity);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			return ResponseEntity.status(statusCode).body(ResponseDTO.builder().success(true).message(message)
					.code(statusCode).response(Collections.emptyMap()).build());

		} catch (Exception e) {
			log.error("Error al guardar/actualizar Parametros Empresa", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
							.code(HttpStatus.BAD_REQUEST.value()).response(Collections.emptyMap()).build());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByIdEnterprise(Integer idEmpresa) {
		log.info("Buscar Parametros Empresa por empresaId={}", idEmpresa);
		try {
			if (idEmpresa == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("Parámetro requerido: idEmpresa").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			List<ParametrosEmpresaEntity> entities = parametrosEmpresaRepository.findByEmpresaId(idEmpresa);

			if (entities == null || entities.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron parametros empresa para la empresa indicada")
								.code(HttpStatus.NOT_FOUND.value()).totalCount(0L).build());
			}

			List<ParametrosEmpresaDTO> dtos = entities.stream().map(parametrosEmpresaMapper::entityToDto).toList();

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtos).totalCount((long) dtos.size()).build());

		} catch (Exception e) {
			log.error("Error al buscar Parametros Empresa por empresaId={}", idEmpresa, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEmpresaAndLlave(Integer idEmpresa, String llave) {
		log.info("Buscar ParametroEmpresa por empresaId={} y llave={}", idEmpresa, llave);

		try {
			if (idEmpresa == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("Parámetro requerido: idEmpresa").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			if (llave == null || llave.isBlank()) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("Parámetro requerido: llave").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			var opt = parametrosEmpresaRepository.findFirstByEmpresa_IdAndLlaveAndActivoTrue(idEmpresa, llave);

			if (opt.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontró parámetro para la empresa indicada y la llave dada")
								.code(HttpStatus.NOT_FOUND.value()).build());
			}

			ParametrosEmpresaDTO dto = parametrosEmpresaMapper.entityToDto(opt.get());

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dto).build());

		} catch (Exception e) {
			log.error("Error al buscar ParametroEmpresa por empresaId={} y llave={}", idEmpresa, llave, e);

			Throwable root = e;
			while (root.getCause() != null && root.getCause() != root) {
				root = root.getCause();
			}

			Map<String, Object> errorInfo = new LinkedHashMap<>();
			errorInfo.put("exception", e.getClass().getName());
			errorInfo.put("message", e.getMessage());
			errorInfo.put("rootCause", root.getMessage());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false)
							.message("Error al consultar parámetros de empresa: "
									+ (root.getMessage() != null ? root.getMessage() : "ver detalle en 'response'"))
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(errorInfo).build());
		}
	}

	public Integer obtenerMesesPeriodo(Integer idEmpresa) {
		ParametrosEmpresaEntity entity = this.parametrosEmpresaRepository
				.findFirstByEmpresa_IdAndLlaveAndActivoTrue(idEmpresa, ParametroEmpresaEnum.PERIODOS_FACT.getCodigo())
				.orElseThrow(() -> new ProcessGenericException(Constantes.PARAM_NOT_FOUND));
		return Integer.parseInt(entity.getValorParametro());
	}

}
