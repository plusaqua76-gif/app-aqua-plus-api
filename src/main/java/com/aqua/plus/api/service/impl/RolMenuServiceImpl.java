package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IRolMenuService;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.RolMenuDTO;
import com.aqua.plus.commons.entities.MenuEntity;
import com.aqua.plus.commons.entities.RolEntity;
import com.aqua.plus.commons.entities.RolMenuEntity;
import com.aqua.plus.commons.maps.RolMenuMapper;
import com.aqua.plus.commons.repositories.MenuRepository;
import com.aqua.plus.commons.repositories.RolMenuRepository;
import com.aqua.plus.commons.repositories.RolRepository;
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
public class RolMenuServiceImpl implements IRolMenuService {

	private final RolMenuRepository rolMenuRepository;
	private final RolRepository rolRepository;
	private final MenuRepository menuRepository;
	private final RolMenuMapper rolMenuMapper;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(RolMenuDTO rolMenuDTO) {
		log.info("Guardar/Actualizar rol menu");
		try {
			boolean isUpdate = rolMenuDTO.getId() != null && rolMenuRepository.existsById(rolMenuDTO.getId());
			if (!isUpdate && rolMenuDTO.getRol() != null && rolMenuDTO.getRol().getId() != null
					&& rolMenuDTO.getMenu() != null && rolMenuDTO.getMenu().getId() != null && rolMenuRepository
							.existsByRolIdAndMenuId(rolMenuDTO.getRol().getId(), rolMenuDTO.getMenu().getId())) {
				ResponseDTO errorResponse = ResponseDTO.builder().success(false)
						.message(Constantes.RATE_TYPE_ALREADY_EXISTS).code(HttpStatus.CONFLICT.value()).build();
				return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
			}

			RolMenuEntity entity;

			if (isUpdate) {
				entity = rolMenuRepository.findById(rolMenuDTO.getId()).orElseThrow();
				rolMenuMapper.updateEntityFromDto(rolMenuDTO, entity);
				entity.setFechaModificacion(new Date());
				entity.setUsuarioModificacion(rolMenuDTO.getUsuarioModificacion());
			} else {
				entity = rolMenuMapper.dtoToEntity(rolMenuDTO);
				entity.setFechaCreacion(new Date());
				entity.setUsuarioCreacion(rolMenuDTO.getUsuarioCreacion());
				entity.setActivo(true);
			}

			if (rolMenuDTO.getRol() != null && rolMenuDTO.getRol().getId() != null) {
				RolEntity rol = rolRepository.findById(rolMenuDTO.getRol().getId())
						.orElseThrow(() -> new RuntimeException(Constantes.EMP_NOT_FOUND));
				entity.setRol(rol);
			}
			if (rolMenuDTO.getMenu() != null && rolMenuDTO.getMenu().getId() != null) {
				MenuEntity menu = menuRepository.findById(rolMenuDTO.getMenu().getId())
						.orElseThrow(() -> new RuntimeException(Constantes.TIP_NOT_FOUND));
				entity.setMenu(menu);
			}

			RolMenuEntity saved = rolMenuRepository.save(entity);
			RolMenuDTO savedDTO = rolMenuMapper.entityToDto(saved);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(message).code(statusCode)
					.response(savedDTO).build();

			return ResponseEntity.status(statusCode).body(responseDTO);

		} catch (Exception e) {
			log.error("Error guardando rol menu", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar rol-menu con activo = true");
		try {
			var list = rolMenuRepository.findByActivoTrue();
			var dtoList = rolMenuMapper.listEntityToDtoList(list);

			var responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();

			return ResponseEntity.ok(responseDTO);

		} catch (Exception e) {
			log.error("Error al listar rol-menu (activo = true)", e);
			var responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByRolAndEmpresa(Integer idRol, Integer idEmpresa) {
		log.info("Listar rol-menu por rolId={}, empresaId={}, activo=true", idRol, idEmpresa);

		if (idRol == null || idEmpresa == null) {
			return ResponseEntity.badRequest()
					.body(ResponseDTO.builder().success(false)
							.message("Los parámetros idRol e idEmpresa son requeridos.")
							.code(HttpStatus.BAD_REQUEST.value()).build());
		}

		try {
			List<RolMenuEntity> filas = rolMenuRepository.findByActivoTrueAndEmpresa_IdAndRol_Id(idEmpresa, idRol);

			List<RolMenuDTO> dtos = rolMenuMapper.listEntityToDtoList(filas);

			if (dtos.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron menús para el rol y empresa indicados.")
								.code(HttpStatus.NOT_FOUND.value()).response(dtos).build());
			}

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtos).build());

		} catch (Exception e) {
			log.error("Error al listar rol-menu por rolId={}, empresaId={}", idRol, idEmpresa, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message(Constantes.CONSULTING_ERROR).code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}
	
	@Override
    @Transactional
    public ResponseEntity<ResponseDTO> deleteById(Integer id) {
        log.info("Inicio método para eliminar tipo de tarifa por id: {}", id);
        try {
            if (!rolMenuRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            rolMenuRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar el tipo de tarifa con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }

}
