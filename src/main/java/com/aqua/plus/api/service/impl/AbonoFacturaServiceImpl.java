package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IAbonoFacturaService;
import com.aqua.plus.commons.dtos.AbonoFacturaDTO;
import com.aqua.plus.commons.dtos.FacturaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.AbonoFacturaEntity;
import com.aqua.plus.commons.entities.FacturaEntity;
import com.aqua.plus.commons.maps.AbonoFacturaMapper;
import com.aqua.plus.commons.maps.EstadoMapper;
import com.aqua.plus.commons.maps.FacturaMapper;
import com.aqua.plus.commons.maps.TipoPagoMapper;
import com.aqua.plus.commons.repositories.AbonoFacturaRepository;
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
	private final EstadoMapper estadoMapper;
	private final TipoPagoMapper tipoPagoMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ResponseEntity<ResponseDTO> save(AbonoFacturaDTO abonoFacturaDTO) {
		log.info("Guardar/Actualizar Abono Factura");

		try {
			if (abonoFacturaDTO.getFactura() == null || abonoFacturaDTO.getFactura().getId() == null) {
				throw new RuntimeException("El abono debe estar asociado a una factura válida");
			}

			if (abonoFacturaDTO.getValor() == null || abonoFacturaDTO.getValor() <= 0) {
				throw new RuntimeException("El valor del abono debe ser mayor a cero");
			}

			boolean isUpdate = abonoFacturaDTO.getId() != null
					&& abonoFacturaRepository.existsById(abonoFacturaDTO.getId());

			AbonoFacturaEntity entity;

			if (isUpdate) {
				entity = abonoFacturaRepository.findById(abonoFacturaDTO.getId()).orElseThrow(
						() -> new RuntimeException("Abono no encontrado con id=" + abonoFacturaDTO.getId()));

				abonoFacturaMapper.updateEntityFromDto(abonoFacturaDTO, entity);
				entity.setFechaModificacion(new Date());
				entity.setUsuarioModificacion(abonoFacturaDTO.getUsuarioModificacion());

			} else {
				entity = abonoFacturaMapper.dtoToEntity(abonoFacturaDTO);
				entity.setActivo(true);
				entity.setFechaCreacion(new Date());
				entity.setUsuarioCreacion(abonoFacturaDTO.getUsuarioCreacion());
			}

			AbonoFacturaEntity savedAbono = abonoFacturaRepository.save(entity);
			log.info("Abono {} correctamente con id={}", isUpdate ? "actualizado" : "creado", savedAbono.getId());

			FacturaDTO facturaDTO = abonoFacturaDTO.getFactura();

			String usuarioResponsable = isUpdate ? abonoFacturaDTO.getUsuarioModificacion()
					: abonoFacturaDTO.getUsuarioCreacion();

			FacturaEntity facturaEntity = facturaRepository.findById(facturaDTO.getId())
					.orElseThrow(() -> new RuntimeException("Factura no encontrada con id=" + facturaDTO.getId()));

			facturaEntity.setUsuarioModificacion(usuarioResponsable);
			facturaEntity.setFechaModificacion(new Date());

			facturaMapper.updateEntityFromDto(facturaDTO, facturaEntity);

			if (facturaDTO.getEstado() != null)
				facturaEntity.setEstado(estadoMapper.dtoToEntity(facturaDTO.getEstado()));

			if (facturaDTO.getTipoPago() != null)
				facturaEntity.setTipoPago(tipoPagoMapper.dtoToEntity(facturaDTO.getTipoPago()));

			facturaEntity.setFechaModificacion(new Date());
			facturaEntity.setUsuarioModificacion(facturaDTO.getUsuarioModificacion());

			FacturaEntity savedFactura = facturaRepository.save(facturaEntity);
			log.info("Factura id={} actualizada a estado={}", savedFactura.getId(),
					facturaDTO.getEstado() != null ? facturaDTO.getEstado() : "sin cambio");

			AbonoFacturaDTO savedAbonoDTO = abonoFacturaMapper.entityToDto(savedAbono);
			FacturaDTO savedFacturaDTO = facturaMapper.entityToDto(savedFactura);

			String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
			int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

			Map<String, Object> responsePayload = new LinkedHashMap<>();
			responsePayload.put("abonoFactura", savedAbonoDTO);
			responsePayload.put("factura", savedFacturaDTO);

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
