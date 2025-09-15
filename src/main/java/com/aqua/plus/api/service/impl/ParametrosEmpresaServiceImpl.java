package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.List;

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
			ParametrosEmpresaEntity parametrosEmpresaEntity;

			if (isUpdate) {
				parametrosEmpresaEntity = parametrosEmpresaRepository.findById(parametrosEmpresaDTO.getId())
						.orElseThrow();
				parametrosEmpresaMapper.updateEntityFromDto(parametrosEmpresaDTO, parametrosEmpresaEntity);
				parametrosEmpresaEntity.setFechaModificacion(new Date());
				parametrosEmpresaEntity.setUsuarioModificacion(parametrosEmpresaDTO.getUsuarioModificacion());
			} else {
				parametrosEmpresaEntity = parametrosEmpresaMapper.dtoToEntity(parametrosEmpresaDTO);
				parametrosEmpresaEntity.setFechaCreacion(new Date());
				parametrosEmpresaEntity.setUsuarioCreacion(parametrosEmpresaDTO.getUsuarioCreacion());
				parametrosEmpresaEntity.setActivo(true);
			}

			ParametrosEmpresaEntity savedEntity = parametrosEmpresaRepository.save(parametrosEmpresaEntity);
			ParametrosEmpresaDTO savedDTO = parametrosEmpresaMapper.entityToDto(savedEntity);

			log.info("Fin guardar/actualizar rol");

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(message).code(statusCode)
					.response(savedDTO).build();

			return ResponseEntity.status(statusCode).body(responseDTO);

		} catch (Exception e) {
			log.error("Error al guardar/actualizar Parametros Empresa", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
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
}
