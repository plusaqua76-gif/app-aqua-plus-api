package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IClienteNovedadService;
import com.aqua.plus.commons.dtos.ClienteNovedadDTO;
import com.aqua.plus.commons.dtos.ClienteNovedadRequestDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ClienteNovedadEntity;
import com.aqua.plus.commons.maps.ClienteNovedadMapper;
import com.aqua.plus.commons.repositories.ClienteNovedadRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteNovedadServiceImpl implements IClienteNovedadService {

	private final ClienteNovedadRepository clienteNovedadRepository;
	private final ClienteNovedadMapper clienteNovedadMapper;
	private final DocumentoServiceImpl documentoServiceImpl;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(ClienteNovedadRequestDTO req) {
		try {
			ClienteNovedadDTO in = req.getNovedad();
			ClienteNovedadEntity novedad = clienteNovedadMapper.dtoToEntity(in);
			novedad = clienteNovedadRepository.save(novedad);

			String base64 = req.getBase64File();
			if (base64 != null && !base64.isBlank()) {

				Integer idPersona = req.getIdPersona();
				if (idPersona == null) {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(ResponseDTO.builder().success(false)
									.message("idPersona es requerido cuando se envía base64File")
									.code(HttpStatus.BAD_REQUEST.value()).build());
				}

				String candidato;
				if (req.getNombreArchivo() != null && !req.getNombreArchivo().isBlank()) {
					candidato = req.getNombreArchivo().trim();
				} else if (in != null && in.getDescripcion() != null && !in.getDescripcion().isBlank()) {
					candidato = in.getDescripcion().trim();
				} else {
					candidato = "pqr";
				}

				String slug = java.text.Normalizer.normalize(candidato, java.text.Normalizer.Form.NFD)
						.replaceAll("\\p{M}+", "").replaceAll("[^A-Za-z0-9\\s_-]", "").replaceAll("\\s+", "-")
						.toLowerCase();
				if (slug.length() > 20)
					slug = slug.substring(0, 20).replaceAll("-+$", "");
				if (slug.isBlank())
					slug = "pqr";

				String yymmdd = java.time.LocalDate.now()
						.format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));
				String rnd6 = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 6);
				String nombreArchivoFinal = slug + "-" + yymmdd + "-" + rnd6;

				String extension = req.getExtension();
				if (extension != null && !extension.isBlank()) {
					extension = extension.trim().toLowerCase();
				}

				String usuarioCreacion = (in != null ? in.getUsuarioCreacion() : null);
				String usuario = (usuarioCreacion != null && !usuarioCreacion.isBlank()) ? usuarioCreacion : "system";

				ResponseEntity<ResponseDTO> respDoc = documentoServiceImpl.saveDocumentoBase64(base64, null, idPersona,
						nombreArchivoFinal, extension, usuario, req.getCategoriaCodigo(), novedad.getId());

				if (!respDoc.getStatusCode().is2xxSuccessful()) {
					return respDoc;
				}

				ResponseDTO body = respDoc.getBody();
				if (body == null || !Boolean.TRUE.equals(body.getSuccess())) {
					String msg = (body != null && body.getMessage() != null) ? body.getMessage()
							: "Fallo subiendo documento a Azure";
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder()
							.success(false).message(msg).code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
				}
			}

			ClienteNovedadDTO out = clienteNovedadMapper.entityToDto(novedad);
			return ResponseEntity.ok(ResponseDTO.builder().success(true).message("Novedad creada correctamente")
					.code(HttpStatus.OK.value()).response(out).build());

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message("Error creando novedad").code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> update(ClienteNovedadDTO clienteNovedadDTO) {
		log.info("inicio metodo Actualizando Cliente Novedad");
		try {
			if (clienteNovedadDTO.getId() == null || !clienteNovedadRepository.existsById(clienteNovedadDTO.getId())) {
				throw new IllegalArgumentException(Constantes.CLIENT_NOT_EXIST);
			}

			ClienteNovedadEntity entity = clienteNovedadRepository.findById(clienteNovedadDTO.getId()).orElseThrow();
			clienteNovedadMapper.updateEntityFromDto(clienteNovedadDTO, entity);
			entity.setFechaModificacion(new Date());
			entity.setUsuarioModificacion(clienteNovedadDTO.getUsuarioModificacion());

			ClienteNovedadEntity updated = clienteNovedadRepository.save(entity);
			ClienteNovedadDTO updatedDTO = clienteNovedadMapper.entityToDto(updated);

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.UPDATED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(updatedDTO).build();

			return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
		} catch (Exception e) {
			log.error("Error actualizando el Cliente Novedad", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.UPDATE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
		log.info("Buscar Cliente Novedad por id: {}", id);
		try {
			Optional<ClienteNovedadEntity> clienteNovedad = clienteNovedadRepository.findById(id);
			if (clienteNovedad.isPresent()) {
				ClienteNovedadDTO dto = clienteNovedadMapper.entityToDto(clienteNovedad.get());
				ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
						.code(HttpStatus.OK.value()).response(dto).build();
				return ResponseEntity.ok(responseDTO);
			} else {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
		} catch (Exception e) {
			log.error("Error al buscar  el Cliente Novedad por id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todos los Cliente Novedad");
		try {
			var list = clienteNovedadRepository.findAll();
			var dtoList = clienteNovedadMapper.listEntityToDtoList(list);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar los Cliente Novedad", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar Cliente Novedad por id: {}", id);
		try {
			if (!clienteNovedadRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			clienteNovedadRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar Cliente Novedad con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEmpresa(Integer idEmpresa, Integer page, Integer size) {
		try {
			if (idEmpresa == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
						.message("idEmpresa es requerido").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			if (page == null || size == null) {
				List<ClienteNovedadEntity> list = clienteNovedadRepository
						.findByEmpresaClienteContador_Empresa_Id(idEmpresa);

				if (list.isEmpty()) {
					return ResponseEntity.status(HttpStatus.NOT_FOUND)
							.body(ResponseDTO.builder().success(false)
									.message("No se encontraron novedades para la empresa indicada")
									.code(HttpStatus.NOT_FOUND.value()).response(List.of()).totalCount(0L).pageSize(0)
									.currentPage(0).totalPages(0).build());
				}

				List<ClienteNovedadDTO> content = list.stream().map(clienteNovedadMapper::entityToDtoLight).toList();

				return ResponseEntity.ok(ResponseDTO.builder().success(true).message("Consulta exitosa")
						.code(HttpStatus.OK.value()).response(content).totalCount((long) content.size())
						.pageSize(content.size()).currentPage(0).totalPages(1).build());
			}

			Pageable pageable = PageRequest.of(page, size);
			Page<ClienteNovedadEntity> p = clienteNovedadRepository.findByEmpresaClienteContador_Empresa_Id(idEmpresa,
					pageable);

			if (p.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.builder().success(false)
						.message("No se encontraron novedades para la empresa indicada")
						.code(HttpStatus.NOT_FOUND.value()).response(List.of()).totalCount(0L)
						.pageSize(pageable.getPageSize()).currentPage(pageable.getPageNumber()).totalPages(0).build());
			}

			List<ClienteNovedadDTO> content = p.getContent().stream().map(clienteNovedadMapper::entityToDtoLight)
					.toList();

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message("Consulta exitosa")
					.code(HttpStatus.OK.value()).response(content).totalCount(p.getTotalElements())
					.pageSize(p.getSize()).currentPage(p.getNumber()).totalPages(p.getTotalPages()).build());

		} catch (Exception e) {
			log.error("Error consultando ClienteNovedad por empresa {}", idEmpresa, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message("Error consultando").code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

}
