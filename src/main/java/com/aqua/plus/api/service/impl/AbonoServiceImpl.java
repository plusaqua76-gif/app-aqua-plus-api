package com.aqua.plus.api.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IAbonoService;
import com.aqua.plus.api.service.impl.specification.AbonoSpecifications;
import com.aqua.plus.commons.dtos.AbonoDTO;
import com.aqua.plus.commons.dtos.AbonoResponseDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.AbonoEntity;
import com.aqua.plus.commons.entities.DeudaClienteEntity;
import com.aqua.plus.commons.entities.PlazoPagoEntity;
import com.aqua.plus.commons.maps.AbonoMapper;
import com.aqua.plus.commons.repositories.AbonoRepository;
import com.aqua.plus.commons.repositories.DeudaClienteRepository;
import com.aqua.plus.commons.repositories.PlazoPagoRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbonoServiceImpl implements IAbonoService {

	private final AbonoRepository abonoRepository;
	private final AbonoMapper abonoMapper;
	private final DeudaClienteRepository deudaClienteRepository;
	private final PlazoPagoRepository plazoPagoRepository;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(AbonoDTO abonoDTO) {
		log.info("Inicio metodo crear abono (minimal response)");
		try {
			if (abonoDTO == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false).message("Abono requerido")
						.code(HttpStatus.BAD_REQUEST.value()).build());
			}

			// Usuario por defecto (raíz)
			final String usuarioRaiz = (abonoDTO.getUsuarioCreacion() != null
					&& !abonoDTO.getUsuarioCreacion().isBlank()) ? abonoDTO.getUsuarioCreacion() : "system";

			// ====== MODO 2: LOTE ======
			if (abonoDTO.getItems() != null && !abonoDTO.getItems().isEmpty()) {
				for (AbonoDTO it : abonoDTO.getItems()) {
					if (it == null || it.getDeudaCliente() == null || it.getDeudaCliente().getId() == null) {
						return ResponseEntity.badRequest()
								.body(ResponseDTO.builder().success(false)
										.message("Cada item debe indicar deudaCliente.id")
										.code(HttpStatus.BAD_REQUEST.value()).build());
					}
					if (it.getValor() == null || it.getValor() <= 0) {
						return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
								.message("Valor de abono inválido en item deudaId=" + it.getDeudaCliente().getId())
								.code(HttpStatus.BAD_REQUEST.value()).build());
					}
				}

				var ids = abonoDTO.getItems().stream().map(it -> it.getDeudaCliente().getId())
						.collect(Collectors.toSet());

				var deudas = deudaClienteRepository.findAllById(ids);
				var mapDeudas = deudas.stream().collect(Collectors.toMap(DeudaClienteEntity::getId, d -> d));

				List<AbonoEntity> abonosAInsertar = new ArrayList<>(abonoDTO.getItems().size());

				for (AbonoDTO it : abonoDTO.getItems()) {
					Integer deudaId = it.getDeudaCliente().getId();
					DeudaClienteEntity deuda = mapDeudas.get(deudaId);
					if (deuda == null) {
						return ResponseEntity.badRequest()
								.body(ResponseDTO.builder().success(false)
										.message("Deuda no encontrada con ID: " + deudaId)
										.code(HttpStatus.BAD_REQUEST.value()).build());
					}
					if (Boolean.FALSE.equals(deuda.getActivo())) {
						return ResponseEntity.badRequest()
								.body(ResponseDTO.builder().success(false)
										.message("La deuda " + deudaId + " ya está inactiva")
										.code(HttpStatus.BAD_REQUEST.value()).build());
					}

					var saldo = BigDecimal.valueOf(deuda.getValor() == null ? 0.0 : deuda.getValor()).setScale(2,
							RoundingMode.HALF_UP);
					var abono = BigDecimal.valueOf(it.getValor()).setScale(2, RoundingMode.HALF_UP);

					if (abono.compareTo(saldo) > 0) {
						return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
								.message(Constantes.MAYOR_VALUE).code(HttpStatus.BAD_REQUEST.value()).build());
					}

					var nuevoSaldo = saldo.subtract(abono).setScale(2, RoundingMode.HALF_UP);
					deuda.setValor(nuevoSaldo.doubleValue());
					deuda.setFechaModificacion(new Date());
					deuda.setUsuarioModificacion(usuarioRaiz);

					// ↓ Decrementa 1 mes del plazo si el abono > 0
					decrementarPlazoSiAplica(deuda, abono);

					if (nuevoSaldo.compareTo(new BigDecimal("0.00")) <= 0) {
						deuda.setValor(0.0);
						deuda.setActivo(false);
					}

					// Registrar abono (SIEMPRE usa usuarioRaiz)
					AbonoEntity abonoEntity = new AbonoEntity();
					abonoEntity.setDeudaCliente(deuda);
					abonoEntity.setValor(abono.doubleValue());
					abonoEntity.setFechaCreacion(new Date());
					abonoEntity.setUsuarioCreacion(usuarioRaiz);
					abonoEntity.setActivo(true);

					abonosAInsertar.add(abonoEntity);
				}

				deudaClienteRepository.saveAll(mapDeudas.values());
				var guardados = abonoRepository.saveAll(abonosAInsertar);

				var minimal = guardados.stream().map(a -> {
					Map<String, Object> x = new LinkedHashMap<>(3);
					x.put("id", a.getId());
					x.put("deudaId", a.getDeudaCliente() != null ? a.getDeudaCliente().getId() : null);
					x.put("valor", a.getValor());
					return x;
				}).collect(Collectors.toList());

				log.info("Finalizo metodo crear abonos (lote). creados={}", minimal.size());
				return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDTO.builder().success(true)
						.code(HttpStatus.CREATED.value()).totalCount((long) minimal.size()).response(minimal).build());
			}

			// ====== MODO 1: SINGLE ======
			if (abonoDTO.getDeudaCliente() == null || abonoDTO.getDeudaCliente().getId() == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
						.message("Deuda requerida").code(HttpStatus.BAD_REQUEST.value()).build());
			}
			if (abonoDTO.getValor() == null || abonoDTO.getValor() <= 0) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
						.message("Valor de abono inválido").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			Integer deudaId = abonoDTO.getDeudaCliente().getId();
			DeudaClienteEntity deuda = deudaClienteRepository.findById(deudaId)
					.orElseThrow(() -> new RuntimeException("Deuda no encontrada con ID: " + deudaId));

			if (Boolean.FALSE.equals(deuda.getActivo())) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
						.message("La deuda ya está inactiva").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			var saldo = BigDecimal.valueOf(deuda.getValor() == null ? 0.0 : deuda.getValor()).setScale(2,
					RoundingMode.HALF_UP);
			var abono = java.math.BigDecimal.valueOf(abonoDTO.getValor()).setScale(2, RoundingMode.HALF_UP);

			if (abono.compareTo(saldo) > 0) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
						.message(Constantes.MAYOR_VALUE).code(HttpStatus.BAD_REQUEST.value()).build());
			}

			var nuevoSaldo = saldo.subtract(abono).setScale(2, RoundingMode.HALF_UP);
			deuda.setValor(nuevoSaldo.doubleValue());
			deuda.setFechaModificacion(new Date());
			deuda.setUsuarioModificacion(usuarioRaiz);

			// ↓ Decrementa 1 mes del plazo si el abono > 0
			decrementarPlazoSiAplica(deuda, abono);

			if (nuevoSaldo.compareTo(new BigDecimal("0.00")) <= 0) {
				deuda.setValor(0.0);
				deuda.setActivo(false);
			}

			deudaClienteRepository.save(deuda);

			AbonoEntity entity = new AbonoEntity();
			entity.setDeudaCliente(deuda);
			entity.setValor(abono.doubleValue());
			entity.setFechaCreacion(new Date());
			entity.setUsuarioCreacion(usuarioRaiz);
			entity.setActivo(true);

			AbonoEntity saved = abonoRepository.save(entity);

			Map<String, Object> minimal = new LinkedHashMap<>();
			minimal.put("id", saved.getId());
			minimal.put("deudaId", deuda.getId());
			minimal.put("valor", saved.getValor());

			log.info("Finalizo metodo crear abono (single)");
			return ResponseEntity.status(HttpStatus.CREATED).body(
					ResponseDTO.builder().success(true).code(HttpStatus.CREATED.value()).response(minimal).build());

		} catch (Exception e) {
			log.error("Error creando el/los abono(s) ", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
					.message(Constantes.SAVE_ERROR).code(HttpStatus.BAD_REQUEST.value()).build());
		}
	}

	/* ==================== Helpers para plazo ==================== */

	private static boolean isPositive(BigDecimal v) {
		return v != null && v.compareTo(BigDecimal.ZERO) > 0;
	}

	private Integer parseMeses(String nombre) {
		if (nombre == null)
			return null;
		try {
			return Integer.valueOf(nombre.trim());
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Decrementa el plazo en 1 mes si el monto aplicado > 0. - Lee el mes actual
	 * desde deuda.getPlazoPago().getNombre() (ej. "12", "6", "1") - Si es > 1,
	 * busca y asigna el PlazoPago con nombre = (mesActual - 1) - Si es == 1, deja
	 * plazoPago = null (ya no tiene plazo).
	 */
	private void decrementarPlazoSiAplica(DeudaClienteEntity deuda, BigDecimal montoAplicado) {
		if (deuda == null || !isPositive(montoAplicado))
			return;
		var plazo = deuda.getPlazoPago();
		if (plazo == null)
			return;

		Integer mesesActual = parseMeses(plazo.getNombre());
		if (mesesActual == null || mesesActual <= 0)
			return;

		int nuevoMeses = mesesActual - 1;
		if (nuevoMeses <= 0) {
			deuda.setPlazoPago(null);
			return;
		}

		String nombreNuevo = String.valueOf(nuevoMeses);
		var nuevoPlazoOpt = plazoPagoRepository.findFirstByNombreAndActivoTrue(nombreNuevo);
		if (nuevoPlazoOpt.isPresent()) {
			deuda.setPlazoPago(nuevoPlazoOpt.get());
		} else {
			log.warn("No existe PlazoPago activo con nombre={}, se conserva el plazo actual (deudaId={})", nombreNuevo,
					deuda.getId());
		}
	}

	@Transactional
	public ResponseEntity<ResponseDTO> abonarATodasLasDeudas(Map<String, Object> body) {
		try {
			Integer eccId = body.get("eccId") != null ? Integer.valueOf(body.get("eccId").toString()) : null;
			Double valorTotal = body.get("valorTotal") != null ? Double.valueOf(body.get("valorTotal").toString())
					: null;
			String usuario = body.get("usuario") != null ? body.get("usuario").toString() : "system";

			return abonarATodasLasDeudas(eccId, valorTotal, usuario);

		} catch (NumberFormatException nfe) {
			return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
					.message("Formato inválido para eccId o valorTotal").code(HttpStatus.BAD_REQUEST.value()).build());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message(Constantes.SAVE_ERROR).code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Transactional
	public ResponseEntity<ResponseDTO> abonarATodasLasDeudas(Integer eccId, Double valorTotal, String usuario) {
		log.info("Abono masivo a deudas. eccId={}, valorTotal={}", eccId, valorTotal);
		try {
			if (eccId == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("eccId es obligatorio").code(HttpStatus.BAD_REQUEST.value()).build());
			}
			if (valorTotal == null || valorTotal <= 0) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("valorTotal inválido").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			List<DeudaClienteEntity> deudas = deudaClienteRepository.findAllActiveByEccIdOrdenFechaAsc(eccId);
			if (deudas == null || deudas.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false).message("No hay deudas activas para el ECC " + eccId)
								.code(HttpStatus.NOT_FOUND.value()).build());
			}

			double restante = valorTotal;
			var abonosAplicados = new java.util.ArrayList<AbonoDTO>();
			for (DeudaClienteEntity d : deudas) {
				if (restante <= 0.000001)
					break;

				double saldo = (d.getValor() == null ? 0.0 : d.getValor());
				if (saldo <= 0.000001)
					continue;

				double aplicar = Math.min(saldo, restante);

				double nuevoSaldo = saldo - aplicar;
				d.setValor(nuevoSaldo);
				d.setFechaModificacion(new Date());
				d.setUsuarioModificacion(usuario);
				decrementarPlazoSiAplica(d);

				if (nuevoSaldo <= 0.000001) {
					d.setValor(0.0);
					d.setActivo(false);
				}

				deudaClienteRepository.save(d);

				AbonoEntity abono = new AbonoEntity();
				abono.setDeudaCliente(d);
				abono.setValor(aplicar);
				abono.setActivo(true);
				abono.setUsuarioCreacion(usuario);
				abono.setFechaCreacion(new Date());
				AbonoEntity saved = abonoRepository.save(abono);

				AbonoDTO savedDTO = abonoMapper.entityToDto(saved);
				abonosAplicados.add(savedDTO);

				restante -= aplicar;
			}

			var result = new java.util.LinkedHashMap<String, Object>();
			result.put("abonosAplicados", abonosAplicados);
			result.put("valorTotalSolicitado", valorTotal);
			result.put("valorTotalAplicado", valorTotal - restante);
			result.put("valorSobrante", Math.max(restante, 0.0));

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.SAVED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(result).build());

		} catch (Exception e) {
			log.error("Error en abono masivo", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message(Constantes.SAVE_ERROR).code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> update(AbonoDTO abonoDTO) {
		log.info("inicio metodo Actualizando abono");
		try {
			if (abonoDTO.getId() == null || !abonoRepository.existsById(abonoDTO.getId())) {
				throw new IllegalArgumentException(Constantes.ABONO_NOT_EXIST);
			}

			AbonoEntity entity = abonoRepository.findById(abonoDTO.getId()).orElseThrow();
			abonoMapper.updateEntityFromDto(abonoDTO, entity);
			entity.setFechaModificacion(new Date());
			entity.setUsuarioModificacion(abonoDTO.getUsuarioModificacion());

			AbonoEntity updated = abonoRepository.save(entity);
			AbonoDTO updatedDTO = abonoMapper.entityToDto(updated);

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.UPDATED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(updatedDTO).build();

			return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
		} catch (Exception e) {
			log.error("Error actualizando el abono", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.UPDATE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
		log.info("Buscar abono por id: {}", id);
		try {
			Optional<AbonoEntity> abono = abonoRepository.findById(id);
			if (abono.isPresent()) {
				AbonoDTO dto = abonoMapper.entityToDto(abono.get());
				ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
						.code(HttpStatus.OK.value()).response(dto).build();
				return ResponseEntity.ok(responseDTO);
			} else {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
		} catch (Exception e) {
			log.error("Error al buscar  el abono por id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> listarAbonosPorIdEmpresa(Integer idEmpresa, String clienteLike,
			String codigoFactura, LocalDate fecha, Double valor, Pageable pageable) {

		log.info(
				"Listar abonos (resumen) por empresaId={} filtros: clienteLike={}, codigoFacturaLike={}, fecha={}, valor={}",
				idEmpresa, clienteLike, codigoFactura, fecha, valor);

		try {
			if (idEmpresa == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("idEmpresa es obligatorio").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			Objects.requireNonNull(pageable, "El pageable no debe ser null");

			Sort defaultSort = Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.desc("id"));

			Pageable effectivePageable = pageable.getSort().isUnsorted()
					? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort)
					: pageable;

			Specification<AbonoEntity> distinctSpec = (root, cq, cb) -> {
				cq.distinct(true);
				return cb.conjunction();
			};

			List<Specification<AbonoEntity>> specs = new ArrayList<>();
			specs.add(distinctSpec);
			specs.add(AbonoSpecifications.porIdEmpresa(idEmpresa));
			specs.add(AbonoSpecifications.activoIgual(Boolean.TRUE));
			specs.add(AbonoSpecifications.fechaIgual(fecha));
			specs.add(AbonoSpecifications.valorIgual(valor));
			specs.add(AbonoSpecifications.clienteLike(clienteLike));
			specs.add(AbonoSpecifications.codigoFactura(codigoFactura));

			Specification<AbonoEntity> spec = Specification.allOf(specs.stream().filter(Objects::nonNull).toList());

			Page<AbonoEntity> pageResult = abonoRepository.findAll(spec, effectivePageable);

			List<AbonoResponseDTO> items = pageResult.getContent().stream().map(a -> {
				var dc = a.getDeudaCliente();
				var ecc = (dc != null ? dc.getEmpresaClienteContador() : null);
				var pxx = (ecc != null ? ecc.getCliente() : null);
				var f = (dc != null ? dc.getFactura() : null);

				String clienteNombre = String
						.join(" ", (pxx != null && pxx.getNombre() != null) ? pxx.getNombre() : "",
								(pxx != null && pxx.getSegundoNombre() != null) ? pxx.getSegundoNombre() : "",
								(pxx != null && pxx.getApellido() != null) ? pxx.getApellido() : "",
								(pxx != null && pxx.getSegundoApellido() != null) ? pxx.getSegundoApellido() : "")
						.replaceAll("\\s+", " ").trim();

				AbonoResponseDTO dto = new AbonoResponseDTO();
				dto.setCliente(clienteNombre);
				dto.setCodigoFactura(f != null ? f.getCodigo() : null);
				dto.setFechaAbono(a.getFechaCreacion());
				dto.setValorAbono(a.getValor());
				return dto;
			}).toList();

			long totalCount = pageResult.getTotalElements();
			int pageSize = pageResult.getSize();
			int currentPage = pageResult.getNumber();
			int totalPages = pageResult.getTotalPages();

			if (items.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron abonos para la empresa con id " + idEmpresa)
								.code(HttpStatus.NOT_FOUND.value()).response(items).totalCount(totalCount)
								.pageSize(pageSize).currentPage(currentPage).totalPages(totalPages).build());
			}

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(items).totalCount(totalCount).pageSize(pageSize)
					.currentPage(currentPage).totalPages(totalPages).build());

		} catch (Exception e) {
			log.error("Error listando abonos (resumen) por empresa {}", idEmpresa, e);

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
							.message("Error listando abonos: "
									+ (rootCauseMessage != null ? rootCauseMessage : "ver detalle en 'response'"))
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(errorInfo).build());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todos los abonos");
		try {
			var list = abonoRepository.findAll();
			var dtoList = abonoMapper.listEntityToDtoList(list);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar los abonos", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar abono por id: {}", id);
		try {
			if (!abonoRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			abonoRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar abono con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	private Integer extractMesesFromPlazo(PlazoPagoEntity pp) {
		if (pp == null || pp.getNombre() == null)
			return null;
		String nombre = pp.getNombre().toLowerCase(java.util.Locale.ROOT).trim();
		if (nombre.contains("día"))
			return 1;
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,2})").matcher(nombre);
		if (m.find()) {
			try {
				int n = Integer.parseInt(m.group(1));
				if (n >= 1 && n <= 12)
					return n;
			} catch (NumberFormatException ignore) {
			}
		}
		return 1;
	}

	private PlazoPagoEntity findPlazoPorMesesOrNull(Integer meses) {
		if (meses == null)
			return null;
		if (meses < 1)
			return null;
		if (meses == 1) {
		}
		String nombre = (meses == 1) ? "1 mes" : (meses + " meses");
		return plazoPagoRepository.findFirstByNombreIgnoreCase(nombre).orElse(null);
	}

	/** Aplica la regla de bajar 1 mes si hay plazo; si queda 0, deja null. */
	private void decrementarPlazoSiAplica(DeudaClienteEntity deuda) {
		PlazoPagoEntity pp = deuda.getPlazoPago();
		if (pp == null)
			return;
		Integer meses = extractMesesFromPlazo(pp);
		if (meses == null)
			return;

		int nuevo = meses - 1;
		if (nuevo <= 0) {
			deuda.setPlazoPago(null);
		} else {
			PlazoPagoEntity nuevoPlazo = findPlazoPorMesesOrNull(nuevo);
			deuda.setPlazoPago(nuevoPlazo);
		}
	}
}
