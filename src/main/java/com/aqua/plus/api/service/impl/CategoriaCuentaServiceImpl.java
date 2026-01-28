package com.aqua.plus.api.service.impl;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.ICategoriaCuentaService;
import com.aqua.plus.commons.dtos.CategoriaCuentaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.CategoriaCuentaEntity;
import com.aqua.plus.commons.maps.CategoriaCuentaMapper;
import com.aqua.plus.commons.repositories.CategoriaCuentaRepository;
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
public class CategoriaCuentaServiceImpl implements ICategoriaCuentaService {

	private final CategoriaCuentaRepository categoriaCuentaRepository;
	private final CategoriaCuentaMapper categoriaCuentaMapper;
	private final CuentaRepository cuentaRepository;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(CategoriaCuentaDTO categoriaCuentaDTO) {
		log.info("Guardar/Actualizar la categoria cuenta");
		try {
			boolean isUpdate = categoriaCuentaDTO.getId() != null
					&& categoriaCuentaRepository.existsById(categoriaCuentaDTO.getId());

			CategoriaCuentaEntity entity;

			if (isUpdate) {

				boolean enUso = cuentaRepository.existsByCategoriaCuenta_Id(categoriaCuentaDTO.getId());
				if (enUso) {
					ResponseDTO responseDTO = ResponseDTO.builder().success(false)
							.message(Constantes.CATEGORIA_CUENTA_EN_USO).code(HttpStatus.CONFLICT.value()).build();
					return ResponseEntity.status(HttpStatus.CONFLICT).body(responseDTO);
				}

				entity = categoriaCuentaRepository.findById(categoriaCuentaDTO.getId()).orElseThrow();
				categoriaCuentaMapper.updateEntityFromDto(categoriaCuentaDTO, entity);
				entity.setFechaModificacion(new Date());
				entity.setUsuarioModificacion(categoriaCuentaDTO.getUsuarioModificacion());

			} else {
				entity = categoriaCuentaMapper.dtoToEntity(categoriaCuentaDTO);
				entity.setFechaCreacion(new Date());
				entity.setUsuarioCreacion(categoriaCuentaDTO.getUsuarioCreacion());
				entity.setActivo(true);
			}

			CategoriaCuentaEntity saved = categoriaCuentaRepository.save(entity);
			CategoriaCuentaDTO savedDTO = categoriaCuentaMapper.entityToDto(saved);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(message).code(statusCode)
					.response(savedDTO).build();

			return ResponseEntity.status(statusCode).body(responseDTO);

		} catch (Exception e) {
			log.error("Error guardando la categoria cuenta", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todas la categorias cuentas");
		try {
			var list = categoriaCuentaRepository.findAll();
			var dtoList = categoriaCuentaMapper.listEntityToDtoList(list);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar la categoria cuenta", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar la categoria cuenta por id: {}", id);

		try {
			if (id == null) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.REQUIRED_ID)
						.code(HttpStatus.BAD_REQUEST.value()).build();
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseDTO);
			}

			if (!categoriaCuentaRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}

			boolean enUso = cuentaRepository.existsByCategoriaCuenta_Id(id);
			if (enUso) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false)
						.message(Constantes.CATEGORIA_CUENTA_EN_USO_ELIMINAR).code(HttpStatus.CONFLICT.value()).build();
				return ResponseEntity.status(HttpStatus.CONFLICT).body(responseDTO);
			}

			categoriaCuentaRepository.deleteById(id);

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();

			return ResponseEntity.ok(responseDTO);

		} catch (Exception e) {
			log.error("Error al eliminar la categoria cuenta con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

}
