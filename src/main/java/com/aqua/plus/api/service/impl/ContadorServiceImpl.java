package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IContadorService;
import com.aqua.plus.commons.dtos.ContadorDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ContadorEntity;
import com.aqua.plus.commons.entities.DireccionEntity;
import com.aqua.plus.commons.entities.EmpresaEntity;
import com.aqua.plus.commons.entities.TipoContadorEntity;
import com.aqua.plus.commons.entities.UsuarioEntity;
import com.aqua.plus.commons.maps.ContadorMapper;
import com.aqua.plus.commons.repositories.ContadorRepository;
import com.aqua.plus.commons.repositories.DireccionRepository;
import com.aqua.plus.commons.repositories.EmpresaClienteContadorRepository;
import com.aqua.plus.commons.repositories.EmpresaRepository;
import com.aqua.plus.commons.repositories.TipoContadorRepository;
import com.aqua.plus.commons.repositories.UsuarioRepository;
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
public class ContadorServiceImpl implements IContadorService {

	private final ContadorRepository contadorRepository;
	private final TipoContadorRepository tipoContadorRepository;
	private final DireccionRepository direccionRepository;
	private final EmpresaClienteContadorRepository empresaClienteContadorRepository;
	private final UsuarioRepository usuarioRepository;
	private final EmpresaRepository empresaRepository;
	private final ContadorMapper contadorMapper;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(ContadorDTO contadorDTO) {
		log.info("Guardar/Actualizar contador");
		try {
			boolean isUpdate = contadorDTO.getId() != null && contadorRepository.existsById(contadorDTO.getId());
			ContadorEntity entity;
			log.info("existe id contador:{} ", contadorDTO.getId());

			if (isUpdate) {
				entity = contadorRepository.findById(contadorDTO.getId()).orElseThrow();
				contadorMapper.updateEntityFromDto(contadorDTO, entity);
				entity.setFechaModificacion(new Date());
				entity.setUsuarioModificacion(contadorDTO.getUsuarioModificacion());
			} else {
				entity = contadorMapper.dtoToEntity(contadorDTO);
				entity.setFechaCreacion(new Date());
				entity.setUsuarioCreacion(contadorDTO.getUsuarioCreacion());
				entity.setActivo(true);
			}

			if (entity.getSerial() != null && !entity.getSerial().isBlank()) {
				String serialTrim = entity.getSerial().trim();

				String usuarioCreacion = isUpdate ? contadorDTO.getUsuarioModificacion()
						: contadorDTO.getUsuarioCreacion();
				var usuario = usuarioRepository.findByNombre(usuarioCreacion);

				if (usuario.isEmpty()) {
					throw new RuntimeException("Usuario no encontrado: " + usuarioCreacion);
				}

				UsuarioEntity user = usuario.get();
				var empresaOpt = empresaRepository.findByUsuario_Id(user.getId());

				if (empresaOpt.isEmpty()) {
					throw new RuntimeException("El usuario no tiene empresa asociada");
				}

				Integer empresaId = empresaOpt.get().getId();

				if (!isUpdate) {
					boolean existeSerialEnEmpresa = empresaClienteContadorRepository
							.existsByContador_SerialAndEmpresa_IdAndActivoTrue(serialTrim, empresaId);

					if (existeSerialEnEmpresa) {
						throw new RuntimeException("El serial '" + serialTrim + "' ya está registrado en tu empresa. "
								+ "El serial + empresa deben ser únicos.");
					}
				} else {
					Optional<ContadorEntity> existente = contadorRepository.findBySerial(serialTrim);

					if (existente.isPresent() && !existente.get().getId().equals(entity.getId())) {
						boolean existeSerialEnEmpresa = empresaClienteContadorRepository
								.existsByContador_IdAndEmpresa_IdAndActivoTrue(existente.get().getId(), empresaId);

						if (existeSerialEnEmpresa) {
							throw new RuntimeException("El serial '" + serialTrim
									+ "' ya está registrado en tu empresa. " + "El serial + empresa deben ser únicos.");
						}
					}
				}
			}

			if (contadorDTO.getTipoContador() != null && contadorDTO.getTipoContador().getId() != null) {
				TipoContadorEntity tipoContador = tipoContadorRepository.findById(contadorDTO.getTipoContador().getId())
						.orElseThrow(() -> new RuntimeException(Constantes.COUND_NOT_FOUND));
				entity.setTipoContador(tipoContador);
			}

			if (contadorDTO.getDescripcion() != null && contadorDTO.getDescripcion().getId() != null) {
				DireccionEntity direccion = direccionRepository.findById(contadorDTO.getDescripcion().getId())
						.orElseThrow(() -> new RuntimeException(Constantes.DIREC_NOT_FOUND));
				entity.setDescripcion(direccion);
			}

			ContadorEntity saved = contadorRepository.save(entity);
			ContadorDTO savedDTO = contadorMapper.entityToDto(saved);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(message).code(statusCode)
					.response(savedDTO).build();

			return ResponseEntity.status(statusCode).body(responseDTO);

		} catch (RuntimeException e) {
			log.error("Error de validación guardando contador: {}", e.getMessage());
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(e.getMessage())
					.code(HttpStatus.BAD_REQUEST.value()).build();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

		} catch (Exception e) {
			log.error("Error guardando contador", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
		log.info("Buscar contador por id: {}", id);
		try {
			Optional<ContadorEntity> contador = contadorRepository.findById(id);
			if (contador.isPresent()) {
				ContadorDTO dto = contadorMapper.entityToDto(contador.get());
				ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
						.code(HttpStatus.OK.value()).response(dto).build();
				return ResponseEntity.ok(responseDTO);
			} else {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
		} catch (Exception e) {
			log.error("Error al buscar contador por id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todos los contadores");
		try {
			var list = contadorRepository.findAll();
			var dtoList = contadorMapper.listEntityToDtoList(list);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar los contadores", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar contador por id: {}", id);
		try {
			if (!contadorRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			contadorRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar el contador con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findContadorPorSerial(String serial, Integer empresaId) {
		log.info("Buscar contador por serial (exacto): '{}' para empresa: {}", serial, empresaId);

		try {
			if (serial == null || serial.isBlank()) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("Parámetro requerido: serial").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			String serialTrim = serial.trim();

			List<ContadorEntity> contadores = contadorRepository.findAllBySerial(serialTrim);
			if (contadores.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No existe contador con el serial especificado")
								.code(HttpStatus.NOT_FOUND.value()).build());
			}

			ContadorEntity contador = contadores.get(0);

			boolean perteneceAEmpresa = false;

			Optional<EmpresaEntity> empresaPorNombre = empresaRepository.findByNombre(contador.getUsuarioCreacion());

			if (empresaPorNombre.isPresent() && empresaPorNombre.get().getId().equals(empresaId)) {
				perteneceAEmpresa = true;
			} else {
				perteneceAEmpresa = empresaClienteContadorRepository.existsByEmpresaIdAndContadorId(empresaId,
						contador.getId());
			}

			if (!perteneceAEmpresa) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ResponseDTO.builder().success(false)
						.message("El contador no pertenece a tu empresa").code(HttpStatus.FORBIDDEN.value()).build());
			}

			boolean enUso = empresaClienteContadorRepository.existsByContador_IdAndActivoTrue(contador.getId());
			if (enUso) {
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body(ResponseDTO.builder().success(false)
								.message("El contador ya está en uso, por favor validar")
								.code(HttpStatus.CONFLICT.value()).build());
			}

			Map<String, Object> dto = new LinkedHashMap<>();
			dto.put("id", contador.getId());
			dto.put("serial", contador.getSerial());
			dto.put("activo", contador.getActivo());

			Map<String, Object> tipo = new LinkedHashMap<>();
			if (contador.getTipoContador() != null) {
				tipo.put("id", contador.getTipoContador().getId());
				tipo.put("nombre", contador.getTipoContador().getNombre());
			} else {
				tipo.put("id", null);
				tipo.put("nombre", null);
			}
			dto.put("tipoContador", tipo);

			Map<String, Object> dir = new LinkedHashMap<>();
			if (contador.getDescripcion() != null) {
				var d = contador.getDescripcion();
				dir.put("id", d.getId());
				dir.put("descripcion", d.getDescripcion());
			} else {
				dir.put("id", null);
				dir.put("descripcion", null);
			}
			dto.put("descripcion", dir);

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dto).build());

		} catch (Exception e) {
			log.error("Error al consultar contador por serial: {}", serial, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message(Constantes.CONSULTING_ERROR).code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

}
