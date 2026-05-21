package com.aqua.plus.api.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.ISaldoClienteService;
import com.aqua.plus.api.service.impl.specification.SaldoClienteSpecification;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.SaldoClienteDTO;
import com.aqua.plus.commons.entities.EmpresaClienteContadorEntity;
import com.aqua.plus.commons.entities.PersonaEntity;
import com.aqua.plus.commons.entities.SaldoClienteEntity;
import com.aqua.plus.commons.maps.SaldoClienteMapper;
import com.aqua.plus.commons.repositories.SaldoClienteRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaldoClienteServiceImpl implements ISaldoClienteService {

	private final SaldoClienteRepository saldoClienteRepository;
	private final SaldoClienteMapper saldoClienteMapper;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(SaldoClienteDTO saldoClienteDTO) {
		log.info("Guardar/Actualizar saldo cliente");
		try {
			boolean isUpdate = saldoClienteDTO.getId() != null
					&& saldoClienteRepository.existsById(saldoClienteDTO.getId());
			SaldoClienteEntity entity;

			if (isUpdate) {
				entity = saldoClienteRepository.findById(saldoClienteDTO.getId()).orElseThrow();
				saldoClienteMapper.updateEntityFromDto(saldoClienteDTO, entity);
				entity.setFechaModificacion(new Date());
				entity.setUsuarioModificacion(saldoClienteDTO.getUsuarioModificacion());
			} else {
				entity = saldoClienteMapper.dtoToEntity(saldoClienteDTO);
				entity.setFechaCreacion(new Date());
				entity.setUsuarioCreacion(saldoClienteDTO.getUsuarioCreacion());
				entity.setActivo(true);
			}

			SaldoClienteEntity saved = saldoClienteRepository.save(entity);
			SaldoClienteDTO savedDTO = saldoClienteMapper.entityToDto(saved);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(message).code(statusCode)
					.response(savedDTO).build();

			return ResponseEntity.status(statusCode).body(responseDTO);

		} catch (Exception e) {
			log.error("Error guardando el saldo del cliente", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
		log.info("Buscar saldo Cliente por id: {}", id);
		try {
			Optional<SaldoClienteEntity> saldoCliente = saldoClienteRepository.findById(id);
			if (saldoCliente.isPresent()) {
				SaldoClienteDTO dto = saldoClienteMapper.entityToDto(saldoCliente.get());
				ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
						.code(HttpStatus.OK.value()).response(dto).build();
				return ResponseEntity.ok(responseDTO);
			} else {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
		} catch (Exception e) {
			log.error("Error al buscar el saldo Cliente por id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todos los saldoCliente");
		try {
			var list = saldoClienteRepository.findAll();
			var dtoList = saldoClienteMapper.listEntityToDtoList(list);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar los saldoCliente", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar saldo Cliente por id: {}", id);
		try {
			if (!saldoClienteRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			saldoClienteRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar el saldo Cliente con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAllByEmpresaClienteContadorId(Integer idEmpresaClienteContador,
			Pageable pageable, String nombre, String cedula, String codigo, Boolean estado, Integer nuid,
			Integer saldoTotal, Integer saldoDisponible, Integer cuotas) {

		log.info(
				"Buscar saldoCliente por ECC: {}, filtros: [nombre={}, cedula={}, codigo={}, "
						+ "estado={}, nuid={}, saldoTotal={}, saldoDisponible={}, cuotas={}]",
				idEmpresaClienteContador, nombre, cedula, codigo, estado, nuid, saldoTotal, saldoDisponible, cuotas);

		try {
			if (idEmpresaClienteContador == null) {
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false)
								.message("Parámetro requerido: idEmpresaClienteContador")
								.code(HttpStatus.BAD_REQUEST.value()).build());
			}
			Objects.requireNonNull(pageable, "El pageable no debe ser null");

			Specification<SaldoClienteEntity> spec = Specification.allOf(
					SaldoClienteSpecification.empresaClienteContadorId(idEmpresaClienteContador),
					SaldoClienteSpecification.clienteNombreLike(nombre),
					SaldoClienteSpecification.clienteCedulaIgual(cedula),
					SaldoClienteSpecification.clienteCodigoIgual(codigo),
					SaldoClienteSpecification.clienteEstado(estado), SaldoClienteSpecification.contadorNuid(nuid),
					SaldoClienteSpecification.saldoTotalMin(saldoTotal),
					SaldoClienteSpecification.saldoDisponibleMin(saldoDisponible),
					SaldoClienteSpecification.cuotasIgual(cuotas));

			Page<SaldoClienteEntity> pageSC = saldoClienteRepository.findAll(spec, pageable);

			final int pageSize = pageable.getPageSize();
			final int pageNumber = pageable.getPageNumber();
			final int chunkSize = pageSize * 5;
			final int uniqueOffset = pageNumber * pageSize;
			final int uniqueTarget = uniqueOffset + pageSize;

			Map<String, SaldoClienteEntity> uniques = new LinkedHashMap<>();
			int chunkPage = 0;
			boolean hasMore = true;

			while (hasMore && uniques.size() < uniqueTarget) {
				Pageable chunkPageable = PageRequest.of(chunkPage, chunkSize, pageable.getSort());
				Page<SaldoClienteEntity> chunk = saldoClienteRepository.findAll(spec, chunkPageable);
				List<SaldoClienteEntity> content = chunk.getContent();

				log.info("Chunk={} (size={}): trajo {} filas", chunkPage, chunkSize, content.size());

				for (SaldoClienteEntity sc : content) {
					EmpresaClienteContadorEntity ecc = sc.getEmpresaClienteContador();
					if (ecc == null)
						continue;

					PersonaEntity p = ecc.getCliente();
					if (p == null)
						continue;

					String numero = (p.getNumeroCedula() != null) ? p.getNumeroCedula().trim() : null;
					Integer tipoDocId = (p.getTipoDocumento() != null) ? p.getTipoDocumento().getId() : null;

					String key;
					if (numero != null && !numero.isBlank()) {
						key = "DOC:" + (tipoDocId != null ? tipoDocId : "NULL") + ":" + numero.toLowerCase();
					} else {
						if (p.getId() == null)
							continue;
						key = "PID:" + p.getId();
					}

					if (!uniques.containsKey(key)) {
						uniques.put(key, sc);
						if (uniques.size() == uniqueTarget)
							break;
					}
				}

				hasMore = chunk.hasNext();
				chunkPage++;
				if (content.isEmpty() && !hasMore)
					break;
			}

			log.info("SaldoCliente únicos acumulados: {} (offset={}, pageSize={})", uniques.size(), uniqueOffset,
					pageSize);

			if (uniqueOffset >= uniques.size()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.builder().success(false)
						.message("No se encontraron registros para los filtros dados")
						.code(HttpStatus.NOT_FOUND.value()).response(List.of()).totalCount(pageSC.getTotalElements())
						.pageSize(pageSize).currentPage(pageNumber).totalPages(pageSC.getTotalPages()).build());
			}

			List<SaldoClienteEntity> uniquesList = new ArrayList<>(uniques.values());
			List<SaldoClienteEntity> pageUniques = uniquesList.subList(uniqueOffset,
					Math.min(uniqueOffset + pageSize, uniquesList.size()));

			log.info("Página {} - registros únicos en página: {}", pageNumber, pageUniques.size());

			List<Map<String, Object>> rows = new ArrayList<>(pageUniques.size());

			for (SaldoClienteEntity sc : pageUniques) {
				EmpresaClienteContadorEntity ecc = sc.getEmpresaClienteContador();
				PersonaEntity p = (ecc != null) ? ecc.getCliente() : null;

				Integer personaId = (p != null) ? p.getId() : null;

				Integer tipoDocId = null;
				String tipoDocNombreVal = null;
				if (p != null && p.getTipoDocumento() != null) {
					tipoDocId = p.getTipoDocumento().getId();
					tipoDocNombreVal = p.getTipoDocumento().getNombre();
				}

				String nombreCompleto = null;
				if (p != null) {
					StringBuilder full = new StringBuilder();
					if (p.getNombre() != null)
						full.append(p.getNombre()).append(' ');
					if (p.getSegundoNombre() != null)
						full.append(p.getSegundoNombre()).append(' ');
					if (p.getApellido() != null)
						full.append(p.getApellido()).append(' ');
					if (p.getSegundoApellido() != null)
						full.append(p.getSegundoApellido());
					nombreCompleto = full.toString().trim().replaceAll("\\s+", " ");
				}

				Map<String, Object> row = new LinkedHashMap<>();

				row.put("saldoClienteId", sc.getId());
				row.put("saldoTotal", sc.getSaldoTotal());
				row.put("saldoDisponible", sc.getSaldoDisponible());
				row.put("cuotas", sc.getCuotas());
				row.put("saldoActivo", sc.getActivo());

				row.put("empresaClienteContadorId", (ecc != null) ? ecc.getId() : null);

				row.put("id", personaId);
				row.put("numeroCedula", p != null ? p.getNumeroCedula() : null);
				row.put("nombreCompleto", nombreCompleto);
				row.put("codigo", p != null ? p.getCodigo() : null);
				row.put("activo", p != null ? p.getActivo() : null);
				row.put("discapacidad", p != null ? p.getDiscapacidad() : null);
				row.put("tipoDocumentoId", tipoDocId);
				row.put("tipoDocumentoNombre", tipoDocNombreVal);

				row.put("nuid", (ecc != null && ecc.getContador() != null) ? ecc.getContador().getNuid() : null);

				rows.add(row);
			}

			rows.sort(Comparator.comparing((Map<String, Object> m) -> Boolean.FALSE.equals(m.get("activo")))
					.thenComparing(m -> ((String) m.getOrDefault("nombreCompleto", "")),
							String.CASE_INSENSITIVE_ORDER));

			long totalUnicos = uniques.size();
			int totalPagesUnicos = (int) Math.ceil((double) totalUnicos / pageSize);

			if (rows.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron registros para los filtros dados")
								.code(HttpStatus.NOT_FOUND.value()).response(rows).totalCount(totalUnicos)
								.pageSize(pageSize).currentPage(pageNumber).totalPages(totalPagesUnicos).build());
			}

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(rows).totalCount(totalUnicos).pageSize(pageSize)
					.currentPage(pageNumber).totalPages(totalPagesUnicos).build());

		} catch (Exception e) {
			log.error("Error al consultar saldoCliente por idEmpresaClienteContador: {}", idEmpresaClienteContador, e);
			Throwable root = e;
			while (root.getCause() != null && root.getCause() != root)
				root = root.getCause();

			Map<String, Object> errorInfo = new LinkedHashMap<>();
			errorInfo.put("exception", e.getClass().getName());
			errorInfo.put("message", e.getMessage());
			errorInfo.put("rootCause", root.getMessage());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false)
							.message("Error consultando saldoCliente: "
									+ (root.getMessage() != null ? root.getMessage() : "ver detalle en 'response'"))
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(errorInfo).build());
		}
	}
}
