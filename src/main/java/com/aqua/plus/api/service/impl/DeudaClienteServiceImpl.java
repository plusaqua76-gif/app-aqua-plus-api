package com.aqua.plus.api.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IDeudaClienteService;
import com.aqua.plus.api.service.impl.specification.DeudaClienteSpecifications;
import com.aqua.plus.commons.dtos.DeudaClienteDTO;
import com.aqua.plus.commons.dtos.DeudaClienteResponseDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.DeudaClienteEntity;
import com.aqua.plus.commons.entities.ParametrosEmpresaEntity;
import com.aqua.plus.commons.entities.TipoDeudaEntity;
import com.aqua.plus.commons.maps.DeudaClienteMapper;
import com.aqua.plus.commons.maps.DeudaClienteResponseMapper;
import com.aqua.plus.commons.repositories.AbonoRepository;
import com.aqua.plus.commons.repositories.DeudaClienteRepository;
import com.aqua.plus.commons.repositories.EmpresaClienteContadorRepository;
import com.aqua.plus.commons.repositories.FacturaRepository;
import com.aqua.plus.commons.repositories.ParametrosEmpresaRepository;
import com.aqua.plus.commons.repositories.TipoDeudaRepository;
import com.aqua.plus.commons.utils.Constantes;

import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeudaClienteServiceImpl implements IDeudaClienteService {

	private final DeudaClienteRepository deudaClienteRepository;
	private final DeudaClienteMapper deudaClienteMapper;
	private final DeudaClienteResponseMapper deudaClienteResponseMapper;
	private final AbonoRepository abonoRepository;
	private final FacturaRepository facturaRepository;
	private final TipoDeudaRepository tipoDeudaRepository;
	private final EmpresaClienteContadorRepository empresaClienteContadorRepository;
	private final ParametrosEmpresaRepository parametrosEmpresaRepository;

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(DeudaClienteDTO deudaClienteDTO) {
		log.info("Inicio metodo de Guardar deuda de Cliente");
		try {
			boolean isUpdate = deudaClienteDTO.getId() != null
					&& deudaClienteRepository.existsById(deudaClienteDTO.getId());
			DeudaClienteEntity entity;

			if (isUpdate) {
				entity = deudaClienteRepository.findById(deudaClienteDTO.getId()).orElseThrow();
				deudaClienteMapper.updateEntityFromDto(deudaClienteDTO, entity);
				entity.setFechaModificacion(new Date());
				entity.setUsuarioModificacion(deudaClienteDTO.getUsuarioModificacion());
			} else {
				entity = deudaClienteMapper.dtoToEntity(deudaClienteDTO);
				entity.setFechaCreacion(new Date());
				entity.setUsuarioCreacion(deudaClienteDTO.getUsuarioCreacion());
				entity.setActivo(true);
			}

			DeudaClienteEntity saved = deudaClienteRepository.save(entity);
			DeudaClienteDTO savedDTO = deudaClienteMapper.entityToDto(saved);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(message).code(statusCode)
					.response(savedDTO).build();
			log.info("Fin del metodo guardar deuda de cliente");
			return ResponseEntity.status(statusCode).body(responseDTO);
		} catch (Exception e) {
			log.error("Error guardando Deuda de Cliente", e);
			ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.SAVE_ERROR)
					.code(HttpStatus.BAD_REQUEST.value()).build();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
		log.info("Buscar Deuda de Cliente por id: {}", id);
		try {
			Optional<DeudaClienteEntity> deudaCliente = deudaClienteRepository.findById(id);
			if (deudaCliente.isPresent()) {
				DeudaClienteDTO dto = deudaClienteMapper.entityToDto(deudaCliente.get());
				ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
						.code(HttpStatus.OK.value()).response(dto).build();
				return ResponseEntity.ok(responseDTO);
			} else {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
		} catch (Exception e) {
			log.error("Error al buscar Deuda de Cliente por id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEmpresaClienteContadorId(Integer eccId) {
	    log.info("Listar TODAS las DeudaCliente activas por eccId: {}", eccId);

	    try {
	        if (eccId == null) {
	            return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
	                    .message("eccId es obligatorio").code(HttpStatus.BAD_REQUEST.value()).build());
	        }

	        List<DeudaClienteEntity> deudas = deudaClienteRepository.findAllActiveByEccIdConFiltroEstado(eccId, "PAFA");
	        
	        if (deudas.isEmpty()) {
	            return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
	                    .code(HttpStatus.OK.value()).totalCount(0L).response(new ArrayList<>()).build());
	        }

	        Integer empresaId = deudas.get(0).getEmpresaClienteContador().getEmpresa().getId();
	        double tasaInteres = 0.0;
	        
	        Optional<ParametrosEmpresaEntity> paramInteres = parametrosEmpresaRepository
	                .findByEmpresaIdAndLlaveAndActivoTrue(empresaId, "INTERES_DEUDA");
	        
	        if (paramInteres.isPresent()) {
	            try {
	                tasaInteres = Double.parseDouble(paramInteres.get().getValorParametro());
	            } catch (NumberFormatException e) {
	                log.error("Error al parsear INTERES_DEUDA: {}", paramInteres.get().getValorParametro());
	            }
	        }

	        List<Integer> deudaIds = deudas.stream().map(DeudaClienteEntity::getId).toList();

	        Map<Integer, Double> abonosPorDeuda = abonoRepository.findAllActiveByDeudaIds(deudaIds).stream()
	                .collect(Collectors.groupingBy(a -> a.getDeudaCliente().getId(),
	                        Collectors.summingDouble(a -> a.getValor() == null ? 0.0 : a.getValor())));

	        var items = new ArrayList<Map<String, Object>>();

	        for (DeudaClienteEntity d : deudas) {
	            Double valorTotal = d.getValor() == null ? 0.0 : d.getValor();
	            double totalAbonado = abonosPorDeuda.getOrDefault(d.getId(), 0.0);
	            double saldoPendiente = Math.max(valorTotal - totalAbonado, 0.0);

	            if (saldoPendiente <= 0) continue;

	            var row = new LinkedHashMap<String, Object>();
	            row.put("id", d.getId());
	            row.put("fechaDeuda", d.getFechaCreacion());
	            row.put("descripcion", d.getDescripcion());
	            row.put("tipoDeudaNombre", (d.getTipoDeuda() != null) ? d.getTipoDeuda().getNombre() : null);
	            row.put("facturaCodigo", (d.getFactura() != null) ? d.getFactura().getCodigo() : null);
	            
	            row.put("valorTotal", valorTotal);
	            row.put("totalAbonado", totalAbonado);
	            row.put("saldoPendiente", saldoPendiente);
	            row.put("tasaInteres", tasaInteres);

	            Integer meses = d.getPlazoPago() != null && d.getPlazoPago() > 0 ? d.getPlazoPago() : 1;
	            row.put("meses", meses);
	            row.put("plazoPagoNombre", meses + " meses");

	            
	            BigDecimal capitalCuota = BigDecimal.valueOf(saldoPendiente)
	                .divide(BigDecimal.valueOf(meses), 2, RoundingMode.HALF_UP);
	            
	            BigDecimal interesCuota = BigDecimal.valueOf(saldoPendiente)
	                .multiply(BigDecimal.valueOf(tasaInteres / 100.0))
	                .setScale(2, RoundingMode.HALF_UP);

	            BigDecimal valorCuotaTotal = capitalCuota.add(interesCuota);

	            row.put("interesCuota", interesCuota.doubleValue());
	            row.put("valorMes", valorCuotaTotal.doubleValue());

	            items.add(row);
	        }

	        return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
	                .code(HttpStatus.OK.value()).totalCount((long) items.size()).response(items).build());

	    } catch (Exception ex) {
	        log.error("Error al listar deudas por eccId: {}", eccId, ex);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); 
	    }
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findConsolidadoByEmpresaClienteContadorId(Integer eccId) {
		log.info("Listar deudas CONSOLIDADAS por tipoDeuda para eccId: {}", eccId);

		try {
			if (eccId == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("eccId es obligatorio").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			List<DeudaClienteEntity> deudas = deudaClienteRepository.findAllActiveByEccIdConFiltroEstado(eccId, "PAFA");

			if (deudas == null || deudas.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron deudas activas para el ECC con id " + eccId)
								.code(HttpStatus.NOT_FOUND.value()).build());
			}

			// NUEVO: Filtrar por estado - solo mostrar si está NULL o tiene código 'PAFA'
			List<DeudaClienteEntity> deudasFiltradas = deudas.stream()
					.filter(d -> d.getEstado() == null || "PAFA".equals(d.getEstado().getCodigo()))
					.collect(Collectors.toList());

			if (deudasFiltradas.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron deudas activas para el ECC con id " + eccId)
								.code(HttpStatus.NOT_FOUND.value()).build());
			}

			List<Integer> deudaIds = deudasFiltradas.stream().map(DeudaClienteEntity::getId)
					.collect(Collectors.toList());

			Map<Integer, Double> abonosPorDeuda = abonoRepository.findAllActiveByDeudaIds(deudaIds).stream()
					.collect(Collectors.groupingBy(a -> a.getDeudaCliente().getId(),
							Collectors.summingDouble(a -> a.getValor() == null ? 0.0 : a.getValor())));

			List<DeudaClienteEntity> deudasConSaldo = deudasFiltradas.stream().filter(d -> {
				double valorDeuda = d.getValor() == null ? 0.0 : d.getValor();
				double totalAbonado = abonosPorDeuda.getOrDefault(d.getId(), 0.0);
				return (valorDeuda - totalAbonado) > 0;
			}).collect(Collectors.toList());

			if (deudasConSaldo.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron deudas pendientes para el ECC con id " + eccId)
								.code(HttpStatus.NOT_FOUND.value()).build());
			}

			Map<TipoDeudaEntity, List<DeudaClienteEntity>> agrupadoPorTipo = deudasConSaldo.stream()
					.filter(d -> d.getTipoDeuda() != null)
					.collect(Collectors.groupingBy(DeudaClienteEntity::getTipoDeuda));

			var items = new ArrayList<Map<String, Object>>(agrupadoPorTipo.size());

			for (Map.Entry<TipoDeudaEntity, List<DeudaClienteEntity>> entry : agrupadoPorTipo.entrySet()) {
				TipoDeudaEntity tipo = entry.getKey();
				List<DeudaClienteEntity> lista = entry.getValue();

				BigDecimal totalSaldo = lista.stream().map(d -> {
					double valorDeuda = d.getValor() == null ? 0.0 : d.getValor();
					double totalAbonado = abonosPorDeuda.getOrDefault(d.getId(), 0.0);
					return BigDecimal.valueOf(Math.max(valorDeuda - totalAbonado, 0));
				}).reduce(BigDecimal.ZERO, BigDecimal::add);

				BigDecimal totalOriginal = lista.stream()
						.map(d -> BigDecimal.valueOf(d.getValor() == null ? 0.0 : d.getValor()))
						.reduce(BigDecimal.ZERO, BigDecimal::add);

				BigDecimal abonosRealizados = lista.stream()
						.map(d -> BigDecimal.valueOf(abonosPorDeuda.getOrDefault(d.getId(), 0.0)))
						.reduce(BigDecimal.ZERO, BigDecimal::add);

				int numeroCuotas = lista.stream().mapToInt(d -> d.getPlazoPago() == null ? 1 : d.getPlazoPago()).sum();

				int cuotasPendientes = lista.stream().filter(d -> {
					double valorDeuda = d.getValor() == null ? 0.0 : d.getValor();
					double totalAbonado = abonosPorDeuda.getOrDefault(d.getId(), 0.0);
					return (valorDeuda - totalAbonado) > 0;
				}).mapToInt(d -> d.getPlazoPago() == null ? 1 : d.getPlazoPago()).sum();

				int cuotasCanceladas = numeroCuotas - cuotasPendientes;

				BigDecimal valorCuota = numeroCuotas > 0
						? totalOriginal.divide(BigDecimal.valueOf(numeroCuotas), 2, RoundingMode.HALF_UP)
						: BigDecimal.ZERO;

				Map<String, Object> row = new LinkedHashMap<>();
				row.put("idTipoDeuda", tipo.getId());
				row.put("nombreTipoDeuda", tipo.getNombre());
				row.put("codigoTipoDeuda", tipo.getCodigo());
				row.put("numeroCuotas", numeroCuotas);
				row.put("valorCuota", valorCuota.doubleValue());
				row.put("abonosRealizados", abonosRealizados.doubleValue());
				row.put("cuotasCanceladas", cuotasCanceladas);
				row.put("cuotasPendientes", cuotasPendientes);
				row.put("nuevoSaldo", totalSaldo.doubleValue());

				items.add(row);
			}

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).totalCount((long) items.size()).response(items).build());

		} catch (Exception ex) {
			log.error("Error al listar deudas consolidadas por eccId: {}", eccId, ex);
			Throwable root = ex;
			while (root.getCause() != null && root.getCause() != root)
				root = root.getCause();
			Map<String, Object> errorInfo = new LinkedHashMap<>();
			errorInfo.put("exception", ex.getClass().getName());
			errorInfo.put("message", ex.getMessage());
			errorInfo.put("rootCause", root.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false)
							.message("Error al consultar deudas consolidadas: "
									+ (root.getMessage() != null ? root.getMessage() : "ver detalle en 'response'"))
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(errorInfo).build());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByIdEnterprise(Integer idEmpresa, String clienteNombreLike,
	        String facturaCodigoLike, String descripcionLike, LocalDate fechaDeuda, Double valor,
	        String tipoDeudaNombre, Integer plazoPago, Pageable pageable) {
	    
	    log.info("Listar DeudaCliente por empresaId={} con filtros activos", idEmpresa);

	    try {
	        if (idEmpresa == null) {
	            return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
	                    .message("idEmpresa es obligatorio").code(HttpStatus.BAD_REQUEST.value()).build());
	        }

	        double tasaInteres = 0.0;
	        var paramInteres = parametrosEmpresaRepository
	                .findByEmpresaIdAndLlaveAndActivoTrue(idEmpresa, "INTERES_DEUDA");
	        
	        if (paramInteres.isPresent()) {
	            try {
	                tasaInteres = Double.parseDouble(paramInteres.get().getValorParametro());
	            } catch (NumberFormatException e) {
	                log.error("Error al parsear INTERES_DEUDA: {}", paramInteres.get().getValorParametro());
	            }
	        }
	        final double tasaFinal = tasaInteres;

	        Specification<DeudaClienteEntity> specConSaldo = DeudaClienteSpecifications.allOfNonNull(
	                DeudaClienteSpecifications.activoTrue(), 
	                DeudaClienteSpecifications.perteneceAEmpresa(idEmpresa),
	                DeudaClienteSpecifications.fechaDeudaIgual(fechaDeuda),
	                DeudaClienteSpecifications.valorIgual(valor),
	                DeudaClienteSpecifications.descripcionLike(descripcionLike),
	                DeudaClienteSpecifications.facturaCodigoLike(facturaCodigoLike),
	                DeudaClienteSpecifications.clienteNombreLike(clienteNombreLike),
	                DeudaClienteSpecifications.tipoDeudaNombreLike(tipoDeudaNombre),
	                DeudaClienteSpecifications.plazoPagoIgual(plazoPago),
	                DeudaClienteSpecifications.conSaldoPendiente());

	        Pageable pageToUse = (pageable != null) ? pageable : Pageable.unpaged();

	        Page<DeudaClienteEntity> page = deudaClienteRepository.findAll((root, cq, cb) -> {
	            if (Long.class != cq.getResultType()) {
	                root.fetch("empresaClienteContador", JoinType.LEFT).fetch("cliente", JoinType.LEFT);
	            }
	            return specConSaldo.toPredicate(root, cq, cb);
	        }, pageToUse);

	        List<DeudaClienteEntity> entities = page.getContent();

	        if (entities.isEmpty()) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                    .body(ResponseDTO.builder().success(false).message("No se encontraron deudas")
	                            .code(HttpStatus.NOT_FOUND.value()).response(List.of())
	                            .totalCount(page.getTotalElements()).build());
	        }

	        List<Integer> deudaIds = entities.stream().map(DeudaClienteEntity::getId).toList();
	        Map<Integer, Double> abonosPorDeuda = abonoRepository.findAllActiveByDeudaIds(deudaIds).stream()
	                .collect(Collectors.groupingBy(a -> a.getDeudaCliente().getId(),
	                        Collectors.summingDouble(a -> a.getValor() == null ? 0.0 : a.getValor())));

	        List<Map<String, Object>> items = entities.stream().map(d -> {
	            double valorTotal = d.getValor() == null ? 0.0 : d.getValor();
	            double totalAbonado = abonosPorDeuda.getOrDefault(d.getId(), 0.0);
	            double saldoPendiente = Math.max(valorTotal - totalAbonado, 0.0);

	            DeudaClienteResponseDTO dto = deudaClienteResponseMapper.toDto(d);

	            Map<String, Object> row = new LinkedHashMap<>();
	            row.put("id", dto.getId());
	            row.put("fechaDeuda", dto.getFechaDeuda());
	            row.put("descripcion", dto.getDescripcion());
	            row.put("valorTotal", valorTotal);
	            row.put("totalAbonado", totalAbonado);
	            row.put("saldoPendiente", saldoPendiente);
	            row.put("tipoDeudaNombre", d.getTipoDeuda() != null ? d.getTipoDeuda().getNombre() : null);

	            Integer meses = d.getPlazoPago();
	            row.put("meses", meses);
	            row.put("plazoPagoNombre", meses != null ? (meses + " meses") : null);

	            if (meses != null && meses > 0) {
	                BigDecimal saldoBD = BigDecimal.valueOf(saldoPendiente);
	                BigDecimal capitalMensual = saldoBD.divide(BigDecimal.valueOf(meses), 2, RoundingMode.HALF_UP);
	                BigDecimal interesCuota = saldoBD.multiply(BigDecimal.valueOf(tasaFinal / 100.0))
	                        .setScale(2, RoundingMode.HALF_UP);

	                row.put("valorMes", capitalMensual.add(interesCuota).doubleValue());
	            } else {
	                row.put("valorMes", null);
	            }

	            row.put("facturaCodigo", d.getFactura() != null ? d.getFactura().getCodigo() : null);
	            row.put("clienteNombre", dto.getClienteNombre());

	            return row;
	        }).toList();

	        return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
	                .code(HttpStatus.OK.value()).response(items).totalCount(page.getTotalElements())
	                .pageSize(page.getSize()).currentPage(page.getNumber()).totalPages(page.getTotalPages()).build());

	    } catch (Exception e) {
	        log.error("Error al buscar DeudaCliente por empresaId={}", idEmpresa, e);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
	                        .code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
	    }
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findAll() {
		log.info("Listar todos las Deudas de Cliente");
		try {
			var list = deudaClienteRepository.findAll();
			var filteredList = list.stream().filter(deuda -> deuda.getValor() != null && deuda.getValor() > 0).toList();
			var dtoList = deudaClienteMapper.listEntityToDtoList(filteredList);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtoList).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al listar las Deudas de Cliente", e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar Deuda de Cliente por id: {}", id);
		try {
			if (!deudaClienteRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			abonoRepository.deleteByDeudaClienteId(id);
			deudaClienteRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar Deuda de Cliente con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> updateDeuda(DeudaClienteDTO deudaClienteDTO) {
		log.info("Inicio del método para actualizar Deuda de Cliente");

		try {
			if (deudaClienteDTO.getId() == null) {
				log.warn("ID de la deuda es nulo. No se puede actualizar.");
				return ResponseEntity.badRequest()
						.body(ResponseDTO.builder().success(false)
								.message("El ID de la deuda es requerido para actualizar.")
								.code(HttpStatus.BAD_REQUEST.value()).build());
			}

			Optional<DeudaClienteEntity> optionalEntity = deudaClienteRepository.findById(deudaClienteDTO.getId());

			if (optionalEntity.isEmpty()) {
				log.warn("No se encontró la Deuda de Cliente con ID: {}", deudaClienteDTO.getId());
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.builder().success(false)
						.message(Constantes.RECORD_NOT_FOUND).code(HttpStatus.NOT_FOUND.value()).build());
			}

			DeudaClienteEntity entity = optionalEntity.get();

			deudaClienteMapper.updateEntityFromDto(deudaClienteDTO, entity);

			entity.setFechaModificacion(new Date());
			entity.setUsuarioModificacion(deudaClienteDTO.getUsuarioModificacion());

			if (deudaClienteDTO.getFactura() != null && deudaClienteDTO.getFactura().getId() != null) {
				facturaRepository.findById(deudaClienteDTO.getFactura().getId()).ifPresentOrElse(entity::setFactura,
						() -> log.warn("No se encontró factura con id: {}", deudaClienteDTO.getFactura().getId()));
			} else {
				entity.setFactura(null);
			}

			if (deudaClienteDTO.getTipoDeuda() != null && deudaClienteDTO.getTipoDeuda().getId() != null) {
				tipoDeudaRepository.findById(deudaClienteDTO.getTipoDeuda().getId()).ifPresent(entity::setTipoDeuda);
			}

			if (deudaClienteDTO.getEmpresaClienteContador() != null
					&& deudaClienteDTO.getEmpresaClienteContador().getId() != null) {
				empresaClienteContadorRepository.findById(deudaClienteDTO.getEmpresaClienteContador().getId())
						.ifPresent(entity::setEmpresaClienteContador);
			}

			DeudaClienteEntity updatedEntity = deudaClienteRepository.save(entity);
			DeudaClienteDTO updatedDTO = deudaClienteMapper.entityToDto(updatedEntity);

			log.info("Deuda de Cliente actualizada exitosamente");
			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.UPDATED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(updatedDTO).build());

		} catch (Exception e) {
			log.error("Error actualizando Deuda de Cliente", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message(Constantes.UPDATE_ERROR).code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

}
