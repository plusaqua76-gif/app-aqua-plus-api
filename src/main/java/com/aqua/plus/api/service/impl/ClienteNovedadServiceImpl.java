package com.aqua.plus.api.service.impl;

import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IClienteNovedadService;
import com.aqua.plus.api.service.impl.specification.ClienteNovedadSpecifications;
import com.aqua.plus.commons.dtos.ClienteNovedadDTO;
import com.aqua.plus.commons.dtos.ClienteNovedadRequestDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ClienteNovedadEntity;
import com.aqua.plus.commons.entities.ParametrosGeneralesEntity;
import com.aqua.plus.commons.maps.ClienteNovedadMapper;
import com.aqua.plus.commons.repositories.ClienteNovedadRepository;
import com.aqua.plus.commons.repositories.ParametrosGeneralesRepository;
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
	private final ParametrosGeneralesRepository parametrosGeneralesRepository;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(ClienteNovedadRequestDTO req) {
		try {
			ClienteNovedadDTO in = req.getNovedad();
			if (in == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
						.message("El objeto 'novedad' es requerido").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			ParametrosGeneralesEntity estadoEntity = null;
			if (in.getEstado() != null) {
				String codigo = (in.getEstado().getCodigo() != null ? in.getEstado().getCodigo().trim() : null);
				String codigoPadre = (in.getEstado().getCodigoPadre() != null ? in.getEstado().getCodigoPadre().trim()
						: null);

				if (codigo != null && !codigo.isBlank()) {

					estadoEntity = parametrosGeneralesRepository.findByCodigoIgnoreCaseAndActivoTrue(codigo)
							.orElse(null);

					if (estadoEntity == null) {
						return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
								.message("No existe estado activo con código"
										+ (codigoPadre != null ? " padre '" + codigoPadre + "'" : "") + " y código '"
										+ codigo + "'.")
								.code(HttpStatus.BAD_REQUEST.value()).build());
					}

					in.getEstado().setId(estadoEntity.getId());
				}
			}

			ClienteNovedadEntity novedad = clienteNovedadMapper.dtoToEntity(in);

			if (estadoEntity != null) {
				novedad.setEstado(estadoEntity);
			}

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
				} else if (in.getDescripcion() != null && !in.getDescripcion().isBlank()) {
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

				String usuarioCreacion = in.getUsuarioCreacion();
				String usuario = (usuarioCreacion != null && !usuarioCreacion.isBlank()) ? usuarioCreacion : "system";

				ResponseEntity<ResponseDTO> respDoc = documentoServiceImpl.saveDocumentoBase64(base64, null, idPersona,
						nombreArchivoFinal, extension, usuario, req.getCategoriaCodigo(), novedad.getId(), null);

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
			log.error("Error creando novedad", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message("Error creando novedad").code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> update(ClienteNovedadDTO dto) {
		log.info("inicio metodo Actualizando Cliente Novedad");

		try {
			if (dto == null || dto.getId() == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("El id de la novedad es obligatorio.").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			var entity = clienteNovedadRepository.findById(dto.getId()).orElse(null);
			if (entity == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.builder().success(false)
						.message(Constantes.CLIENT_NOT_EXIST).code(HttpStatus.NOT_FOUND.value()).build());
			}

			if (dto.getEstado() != null) {
				String codigo = dto.getEstado().getCodigo();
				if (codigo != null && !codigo.isBlank()) {
					var estadoPG = parametrosGeneralesRepository.findByCodigoIgnoreCaseAndActivoTrue(codigo.trim())
							.orElse(null);

					if (estadoPG == null) {
						return ResponseEntity.badRequest()
								.body(ResponseDTO.builder().success(false)
										.message("El código de estado no existe o está inactivo: " + codigo)
										.code(HttpStatus.BAD_REQUEST.value()).build());
					}
					entity.setEstado(estadoPG);
				} else {
					log.debug("DTO.estado presente pero sin 'codigo'; se mantiene el estado actual.");
				}
			}

			clienteNovedadMapper.updateEntityFromDto(dto, entity);

			entity.setFechaModificacion(new Date());
			entity.setUsuarioModificacion(dto.getUsuarioModificacion());

			var updated = clienteNovedadRepository.save(entity);
			var out = clienteNovedadMapper.entityToDto(updated);

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.UPDATED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(out).build());

		} catch (Exception e) {
			log.error("Error actualizando el Cliente Novedad", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
					.message(Constantes.UPDATE_ERROR).code(HttpStatus.BAD_REQUEST.value()).build());
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
	public ResponseEntity<ResponseDTO> findByEmpresa(Integer idEmpresa, String novedad, String clienteNombre,
			String contadorSerial, String estadoDescripcion, String codigo, String descripcion, Boolean activo,
			String fechaCreacion, Pageable pageable) {

		try {
			if (idEmpresa == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("idEmpresa es requerido").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			Objects.requireNonNull(pageable, "El pageable no debe ser null");

			Sort defaultSort = Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.desc("id"));

			Pageable effectivePageable = pageable.getSort().isUnsorted()
					? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort)
					: pageable;

			LocalDate fecha = (fechaCreacion == null || fechaCreacion.isBlank()) ? null
					: LocalDate.parse(fechaCreacion);

			boolean hayFiltros = (novedad != null && !novedad.isBlank())
					|| (clienteNombre != null && !clienteNombre.isBlank())
					|| (contadorSerial != null && !contadorSerial.isBlank())
					|| (estadoDescripcion != null && !estadoDescripcion.isBlank())
					|| (codigo != null && !codigo.isBlank()) || (descripcion != null && !descripcion.isBlank())
					|| (activo != null) || (fecha != null);

			Page<ClienteNovedadEntity> page;

			if (!hayFiltros) {
				page = clienteNovedadRepository.findByEmpresaClienteContador_Empresa_Id(idEmpresa, effectivePageable);
			} else {
				Specification<ClienteNovedadEntity> spec = Specification.allOf(
						ClienteNovedadSpecifications.empresaId(idEmpresa),
						ClienteNovedadSpecifications.tipoNovedadNombreLike(novedad),
						ClienteNovedadSpecifications.clienteNombreLike(clienteNombre),
						ClienteNovedadSpecifications.contadorSerialLike(contadorSerial),
						ClienteNovedadSpecifications.estadoDescripcionLike(estadoDescripcion),
						ClienteNovedadSpecifications.codigoLike(codigo),
						ClienteNovedadSpecifications.descripcionLike(descripcion),
						ClienteNovedadSpecifications.activoEquals(activo),
						ClienteNovedadSpecifications.fechaCreacionEquals(fecha));

				page = clienteNovedadRepository.findAll(spec, effectivePageable);
			}

			if (page.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron novedades para la empresa indicada")
								.code(HttpStatus.NOT_FOUND.value()).response(List.of())
								.totalCount(page.getTotalElements()).pageSize(page.getSize())
								.currentPage(page.getNumber()).totalPages(page.getTotalPages()).build());
			}

			List<ClienteNovedadDTO> content = page.getContent().stream().map(clienteNovedadMapper::entityToDtoLight)
					.toList();

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message("Consulta exitosa")
					.code(HttpStatus.OK.value()).response(content).totalCount(page.getTotalElements())
					.pageSize(page.getSize()).currentPage(page.getNumber()).totalPages(page.getTotalPages()).build());

		} catch (java.time.format.DateTimeParseException ex) {
			return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
					.message("Formato de fecha inválido. Usa yyyy-MM-dd").code(HttpStatus.BAD_REQUEST.value()).build());
		} catch (Exception e) {
			log.error("Error consultando ClienteNovedad por empresa {}", idEmpresa, e);

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

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false)
							.message("Error consultando ClienteNovedad: "
									+ (rootCauseMessage != null ? rootCauseMessage : "ver detalle en 'response'"))
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(errorInfo).build());
		}
	}

}
