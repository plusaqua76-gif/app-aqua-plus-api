package com.aqua.plus.api.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IAbonoFacturaService;
import com.aqua.plus.commons.dtos.AbonoFacturaDTO;
import com.aqua.plus.commons.dtos.FacturaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.AbonoEntity;
import com.aqua.plus.commons.entities.AbonoFacturaEntity;
import com.aqua.plus.commons.entities.DeudaClienteEntity;
import com.aqua.plus.commons.entities.FacturaEntity;
import com.aqua.plus.commons.maps.AbonoFacturaMapper;
import com.aqua.plus.commons.maps.EstadoMapper;
import com.aqua.plus.commons.maps.FacturaMapper;
import com.aqua.plus.commons.maps.TipoPagoMapper;
import com.aqua.plus.commons.repositories.AbonoFacturaRepository;
import com.aqua.plus.commons.repositories.AbonoRepository;
import com.aqua.plus.commons.repositories.DeudaClienteRepository;
import com.aqua.plus.commons.repositories.FacturaRepository;
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
public class AbonoFacturaServiceImpl implements IAbonoFacturaService {

	private final AbonoFacturaRepository abonoFacturaRepository;
	private final AbonoFacturaMapper abonoFacturaMapper;
	private final FacturaMapper facturaMapper;
	private final FacturaRepository facturaRepository;
	private final DeudaClienteRepository deudaClienteRepository;
	private final EstadoMapper estadoMapper;
	private final TipoPagoMapper tipoPagoMapper;
	private final AbonoRepository abonoRepository;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ResponseEntity<ResponseDTO> save(AbonoFacturaDTO abonoFacturaDTO) {
		log.info("Guardar/Actualizar Abono Factura");

		try {
			if (abonoFacturaDTO.getFactura() == null || abonoFacturaDTO.getFactura().getId() == null)
				throw new RuntimeException("El abono debe estar asociado a una factura válida");

			if (abonoFacturaDTO.getValor() == null || abonoFacturaDTO.getValor() <= 0)
				throw new RuntimeException("El valor del abono debe ser mayor a cero");

			boolean isUpdate = abonoFacturaDTO.getId() != null
					&& abonoFacturaRepository.existsById(abonoFacturaDTO.getId());

			if (isUpdate && (abonoFacturaDTO.getUsuarioModificacion() == null
					|| abonoFacturaDTO.getUsuarioModificacion().isBlank()))
				throw new RuntimeException("usuarioModificacion es requerido para actualizar");

			if (!isUpdate
					&& (abonoFacturaDTO.getUsuarioCreacion() == null || abonoFacturaDTO.getUsuarioCreacion().isBlank()))
				throw new RuntimeException("usuarioCreacion es requerido para crear");

			String usuarioResponsable = isUpdate ? abonoFacturaDTO.getUsuarioModificacion()
					: abonoFacturaDTO.getUsuarioCreacion();

			FacturaDTO facturaDTO = abonoFacturaDTO.getFactura();

			FacturaEntity facturaEntity = facturaRepository.findById(facturaDTO.getId())
					.orElseThrow(() -> new RuntimeException("Factura no encontrada con id=" + facturaDTO.getId()));

			AbonoFacturaEntity entity;

			if (isUpdate) {
				entity = abonoFacturaRepository.findById(abonoFacturaDTO.getId()).orElseThrow(
						() -> new RuntimeException("Abono no encontrado con id=" + abonoFacturaDTO.getId()));
				abonoFacturaMapper.updateEntityFromDto(abonoFacturaDTO, entity);
				entity.setFechaModificacion(new Date());
				entity.setUsuarioModificacion(usuarioResponsable);
			} else {
				entity = abonoFacturaMapper.dtoToEntity(abonoFacturaDTO);
				entity.setActivo(true);
				entity.setFechaCreacion(new Date());
				entity.setUsuarioCreacion(usuarioResponsable);
			}

			AbonoFacturaEntity savedAbono = abonoFacturaRepository.save(entity);
			log.info("AbonoFactura {} con id={}", isUpdate ? "actualizado" : "creado", savedAbono.getId());

			Map<String, Object> resumenDeudas = new LinkedHashMap<>();

			Optional<DeudaClienteEntity> optDeuda = deudaClienteRepository
					.findByFacturaIdAndActivoTrue(facturaEntity.getId());

			if (optDeuda.isPresent()) {
				DeudaClienteEntity deuda = optDeuda.get();

				BigDecimal valorDeuda = BigDecimal.valueOf(deuda.getValor() == null ? 0.0 : deuda.getValor())
						.setScale(2, RoundingMode.HALF_UP);

				BigDecimal totalAbonadoPrevio = abonoRepository.findAllActiveByDeudaIds(List.of(deuda.getId())).stream()
						.map(a -> BigDecimal.valueOf(a.getValor() == null ? 0.0 : a.getValor()))
						.reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

				BigDecimal saldoDeuda = valorDeuda.subtract(totalAbonadoPrevio).setScale(2, RoundingMode.HALF_UP);

				BigDecimal valorAbono = BigDecimal.valueOf(abonoFacturaDTO.getValor()).setScale(2,
						RoundingMode.HALF_UP);

				BigDecimal montoAplicado = valorAbono.min(saldoDeuda).setScale(2, RoundingMode.HALF_UP);

				BigDecimal sobrante = valorAbono.subtract(montoAplicado).setScale(2, RoundingMode.HALF_UP);

				log.info("Deuda id={} | saldo={} | abonoIntentado={} | montoAplicado={} | sobrante={}", deuda.getId(),
						saldoDeuda, valorAbono, montoAplicado, sobrante);

				AbonoEntity abonoDeuda = new AbonoEntity();
				abonoDeuda.setDeudaCliente(deuda);
				abonoDeuda.setValor(montoAplicado.doubleValue());
				abonoDeuda.setActivo(true);
				abonoDeuda.setFechaCreacion(new Date());
				abonoDeuda.setUsuarioCreacion(usuarioResponsable);
				AbonoEntity savedAbonoDeuda = abonoRepository.save(abonoDeuda);
				log.info("Abono a deuda registrado con id={}", savedAbonoDeuda.getId());

				resumenDeudas.put("deudaId", deuda.getId());
				resumenDeudas.put("saldoPrevio", saldoDeuda);
				resumenDeudas.put("montoAplicado", montoAplicado);

				// ── 5. Si hay sobrante → crear nueva deuda ────────────────────
				if (sobrante.compareTo(BigDecimal.ZERO) > 0) {
					log.info("Sobrante de {} → generando nueva deuda para factura id={}", sobrante,
							facturaEntity.getId());

					DeudaClienteEntity nuevaDeuda = new DeudaClienteEntity();
					nuevaDeuda.setFactura(facturaEntity);
					nuevaDeuda.setEmpresaClienteContador(deuda.getEmpresaClienteContador());
					nuevaDeuda.setTipoDeuda(deuda.getTipoDeuda());
					nuevaDeuda.setEstado(deuda.getEstado());
					nuevaDeuda.setValor(sobrante.doubleValue());
					nuevaDeuda.setDescripcion("Deuda generada por sobrante de abono parcial");
					nuevaDeuda.setActivo(true);
					nuevaDeuda.setFechaCreacion(new Date());
					nuevaDeuda.setUsuarioCreacion(usuarioResponsable);

					DeudaClienteEntity savedNuevaDeuda = deudaClienteRepository.save(nuevaDeuda);
					log.info("Nueva deuda por sobrante creada con id={} valor={}", savedNuevaDeuda.getId(), sobrante);

					resumenDeudas.put("sobrante", sobrante);
					resumenDeudas.put("nuevaDeudaId", savedNuevaDeuda.getId());
				} else {
					resumenDeudas.put("sobrante", BigDecimal.ZERO);
				}

			} else {
				log.info("Factura id={} sin deuda activa, solo se registra el AbonoFactura", facturaEntity.getId());
				resumenDeudas.put("mensaje", "Sin deuda activa asociada a la factura");
			}

			// ── 6. Actualizar la Factura (estado + tipoPago desde el front) ───
			facturaMapper.updateEntityFromDto(facturaDTO, facturaEntity);

			if (facturaDTO.getEstado() != null)
				facturaEntity.setEstado(estadoMapper.dtoToEntity(facturaDTO.getEstado()));

			if (facturaDTO.getTipoPago() != null)
				facturaEntity.setTipoPago(tipoPagoMapper.dtoToEntity(facturaDTO.getTipoPago()));

			facturaEntity.setFechaModificacion(new Date());
			facturaEntity.setUsuarioModificacion(usuarioResponsable);

			FacturaEntity savedFactura = facturaRepository.save(facturaEntity);
			log.info("Factura id={} actualizada a estado={}", savedFactura.getId(),
					facturaDTO.getEstado() != null ? facturaDTO.getEstado() : "sin cambio");

			// ── 7. Construir respuesta ─────────────────────────────────────────
			AbonoFacturaDTO savedAbonoDTO = abonoFacturaMapper.entityToDto(savedAbono);
			FacturaDTO savedFacturaDTO = facturaMapper.entityToDto(savedFactura);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			Map<String, Object> responsePayload = new LinkedHashMap<>();
			responsePayload.put("abonoFactura", savedAbonoDTO);
			responsePayload.put("factura", savedFacturaDTO);
			responsePayload.put("deudas", resumenDeudas);

			return ResponseEntity.status(statusCode).body(ResponseDTO.builder().success(true).message(message)
					.code(statusCode).response(responsePayload).build());

		} catch (RuntimeException e) {
			log.error("Error de negocio guardando AbonoFactura: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
					.message(e.getMessage()).code(HttpStatus.BAD_REQUEST.value()).build());

		} catch (Exception e) {
			log.error("Error inesperado guardando AbonoFactura", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false)
							.message("Error interno del servidor: " + e.getClass().getSimpleName())
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> deleteById(Integer id) {
		log.info("Inicio método para eliminar el Abono Factura por id: {}", id);
		try {
			if (!abonoFacturaRepository.existsById(id)) {
				ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
						.code(HttpStatus.NOT_FOUND.value()).build();
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
			}
			abonoFacturaRepository.deleteById(id);
			ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).build();
			return ResponseEntity.ok(responseDTO);
		} catch (Exception e) {
			log.error("Error al eliminar el aAbono Factura con id: {}", id, e);
			ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
					.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
		}
	}
}
