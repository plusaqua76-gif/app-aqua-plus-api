package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.ITarifasTransversalesService;
import com.aqua.plus.commons.dtos.EmpresaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.TarifasTransversalesDTO;
import com.aqua.plus.commons.dtos.TipoUsoDTO;
import com.aqua.plus.commons.entities.EmpresaEntity;
import com.aqua.plus.commons.entities.TarifasTransversalesEntity;
import com.aqua.plus.commons.maps.TarifasTransversalesMapper;
import com.aqua.plus.commons.repositories.TarifasTransversalesRepository;
import com.aqua.plus.commons.repositories.TipoUsoRepository;
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
public class TarifasTransversalesServiceImpl implements ITarifasTransversalesService {

	private final TarifasTransversalesRepository tarifasTransversalesRepository;
	private final TipoUsoRepository tipoUsoRepository;
	private final TarifasTransversalesMapper tarifasTransversalesMapper;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(TarifasTransversalesDTO dto) {
		log.info("Guardar/Actualizar Tarifa Transversal (por id)");

		try {
			if (dto == null) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false).code(HttpStatus.BAD_REQUEST.value())
								.message("El DTO de TarifasTransversales es obligatorio").response(null).build());
			}

			final Integer id = dto.getId();

			if (id != null) {
				var opt = tarifasTransversalesRepository.findById(id);
				if (opt.isPresent()) {
					TarifasTransversalesEntity target = opt.get();

					if (dto.getNombre() != null)
						target.setNombre(dto.getNombre());
					if (dto.getEstrato() != null)
						target.setEstrato(dto.getEstrato());
					if (dto.getCodigo() != null)
						target.setCodigo(dto.getCodigo());
					if (dto.getValor() != null)
						target.setValor(dto.getValor());
					if (dto.getActivo() != null)
						target.setActivo(dto.getActivo());

					Integer empresaIdDto = (dto.getEmpresa() != null ? dto.getEmpresa().getId() : null);
					if (empresaIdDto != null) {
						EmpresaEntity empresa = new EmpresaEntity();
						empresa.setId(empresaIdDto);
						target.setEmpresa(empresa);
					}

					Integer tipoUsoIdDto = (dto.getTipoUso() != null ? dto.getTipoUso().getId() : null);
					if (tipoUsoIdDto != null) {
						var tipoUso = tipoUsoRepository.findById(tipoUsoIdDto).orElseThrow(
								() -> new RuntimeException("No se encontró TipoUso con id " + tipoUsoIdDto));
						target.setTipoUso(tipoUso);
					}

					target.setFechaModificacion(new Date());
					target.setUsuarioModificacion(dto.getUsuarioModificacion() != null ? dto.getUsuarioModificacion()
							: dto.getUsuarioCreacion());

					TarifasTransversalesEntity saved = tarifasTransversalesRepository.save(target);

					TarifasTransversalesDTO savedDTO = TarifasTransversalesDTO.builder().id(saved.getId())
							.nombre(saved.getNombre()).estrato(saved.getEstrato()).codigo(saved.getCodigo())
							.valor(saved.getValor()).activo(saved.getActivo())
							.usuarioCreacion(saved.getUsuarioCreacion()).fechaCreacion(saved.getFechaCreacion())
							.usuarioModificacion(saved.getUsuarioModificacion())
							.fechaModificacion(saved.getFechaModificacion())
							.empresa(saved.getEmpresa() != null && saved.getEmpresa().getId() != null
									? EmpresaDTO.builder().id(saved.getEmpresa().getId()).build()
									: null)
							.tipoUso(saved.getTipoUso() != null && saved.getTipoUso().getId() != null
									? TipoUsoDTO.builder().id(saved.getTipoUso().getId()).build()
									: null)
							.build();

					return ResponseEntity.status(HttpStatus.OK)
							.body(ResponseDTO.builder().success(true).code(HttpStatus.OK.value())
									.message(Constantes.UPDATED_SUCCESSFULLY).response(savedDTO).build());
				}
			}

			Integer empresaId = (dto.getEmpresa() != null ? dto.getEmpresa().getId() : null);
			if (empresaId == null) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false).code(HttpStatus.BAD_REQUEST.value())
								.message("Debe indicar la empresa (empresa.id)").response(null).build());
			}

			Integer tipoUsoId = (dto.getTipoUso() != null ? dto.getTipoUso().getId() : null);
			if (tipoUsoId == null) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false).code(HttpStatus.BAD_REQUEST.value())
								.message("Debe indicar el tipo de uso (tipoUso.id)").response(null).build());
			}

			if (dto.getNombre() == null || dto.getNombre().isBlank()) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false).code(HttpStatus.BAD_REQUEST.value())
								.message("El nombre es obligatorio").response(null).build());
			}

			if (dto.getEstrato() == null) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false).code(HttpStatus.BAD_REQUEST.value())
								.message("El estrato es obligatorio").response(null).build());
			}

			if (dto.getCodigo() == null || dto.getCodigo().isBlank()) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false).code(HttpStatus.BAD_REQUEST.value())
								.message("El código es obligatorio").response(null).build());
			}

			if (dto.getValor() == null) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false).code(HttpStatus.BAD_REQUEST.value())
								.message("El valor es obligatorio").response(null).build());
			}

			var tipoUso = tipoUsoRepository.findById(tipoUsoId)
					.orElseThrow(() -> new RuntimeException("No se encontró TipoUso con id " + tipoUsoId));

			TarifasTransversalesEntity target = tarifasTransversalesMapper.dtoToEntity(dto);

			EmpresaEntity empresa = new EmpresaEntity();
			empresa.setId(empresaId);
			target.setEmpresa(empresa);

			target.setTipoUso(tipoUso);

			target.setActivo(dto.getActivo() != null ? dto.getActivo() : Boolean.TRUE);
			target.setFechaCreacion(new Date());
			target.setUsuarioCreacion(dto.getUsuarioCreacion());

			TarifasTransversalesEntity saved = tarifasTransversalesRepository.save(target);

			TarifasTransversalesDTO savedDTO = TarifasTransversalesDTO.builder().id(saved.getId())
					.nombre(saved.getNombre()).estrato(saved.getEstrato()).codigo(saved.getCodigo())
					.valor(saved.getValor()).activo(saved.getActivo()).usuarioCreacion(saved.getUsuarioCreacion())
					.fechaCreacion(saved.getFechaCreacion()).usuarioModificacion(saved.getUsuarioModificacion())
					.fechaModificacion(saved.getFechaModificacion())
					.empresa(saved.getEmpresa() != null && saved.getEmpresa().getId() != null
							? EmpresaDTO.builder().id(saved.getEmpresa().getId()).build()
							: null)
					.tipoUso(saved.getTipoUso() != null && saved.getTipoUso().getId() != null
							? TipoUsoDTO.builder().id(saved.getTipoUso().getId()).build()
							: null)
					.build();

			return ResponseEntity.status(HttpStatus.CREATED)
					.body(ResponseDTO.builder().success(true).code(HttpStatus.CREATED.value())
							.message(Constantes.SAVED_SUCCESSFULLY).response(savedDTO).build());

		} catch (Exception e) {
			log.error("Error guardando tarifa transversal", e);

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
							.code(HttpStatus.BAD_REQUEST.value()).response(e.getMessage()).build());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEnterpriseId(Integer idEmpresa) {
		log.info("Buscar tarifas transversales por empresaId={}", idEmpresa);
		try {
			if (idEmpresa == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("Parámetro requerido: idEmpresa").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			List<TarifasTransversalesEntity> entities = tarifasTransversalesRepository.findByEmpresa_Id(idEmpresa);

			if (entities == null || entities.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron tarifas transversales para la empresa indicada")
								.code(HttpStatus.NOT_FOUND.value()).totalCount(0L).build());
			}

			List<TarifasTransversalesDTO> dtos = entities.stream().map(tarifasTransversalesMapper::entityToDto)
					.toList();

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtos).totalCount((long) dtos.size()).build());

		} catch (Exception e) {
			log.error("Error al buscar tarifas transversales por empresaId={}", idEmpresa, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(e.getMessage()).build());
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar tarifa transversal por id: {}", id);
		try {
			if (id == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("Parámetro requerido: id").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			if (!tarifasTransversalesRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}

			tarifasTransversalesRepository.deleteById(id);

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();

			return ResponseEntity.ok(responseDTO);

		} catch (Exception e) {
			log.error("Error al eliminar la tarifa transversal con id: {}", id, e);

			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(e.getMessage()).build();

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

}
