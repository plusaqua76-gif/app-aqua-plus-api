package com.aqua.plus.api.service.impl;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IHistoricoLecturaService;
import com.aqua.plus.commons.dtos.ContadorDTO;
import com.aqua.plus.commons.dtos.HistoricoLecturaDTO;
import com.aqua.plus.commons.dtos.LecturaDTO;
import com.aqua.plus.commons.dtos.PersonaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.EmpresaClienteContadorEntity;
import com.aqua.plus.commons.entities.HistoricoLecturaEntity;
import com.aqua.plus.commons.repositories.EmpresaClienteContadorRepository;
import com.aqua.plus.commons.repositories.HistoricoLecturaRepository;
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
public class HistoricoLecturaServiceImpl implements IHistoricoLecturaService {

	private final HistoricoLecturaRepository historicoLecturaRepository;
	private final EmpresaClienteContadorRepository empresaClienteContadorRepository;

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findHistoricoByLecturaId(Integer idLectura) {
		log.info("Buscando histórico de lectura para idLectura={}", idLectura);
		try {
			if (idLectura == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("Parámetro requerido: idLectura").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			List<HistoricoLecturaEntity> entities = historicoLecturaRepository
					.findByLectura_IdOrderByFechaCreacionAsc(idLectura);

			if (entities == null || entities.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseDTO.builder().success(false)
								.message("No se encontraron registros de histórico para la lectura indicada")
								.code(HttpStatus.NOT_FOUND.value()).totalCount(0L).build());
			}

			var primerContador = entities.getFirst().getContador();
			var eccOpt = empresaClienteContadorRepository.findByContador_IdAndActivoTrue(primerContador.getId());

			PersonaDTO clienteDTO = eccOpt.map(ecc -> {
				var cli = ecc.getCliente();
				return (cli != null)
						? PersonaDTO.builder().id(cli.getId()).nombre(cli.getNombre())
								.segundoNombre(cli.getSegundoNombre()).apellido(cli.getApellido())
								.segundoApellido(cli.getSegundoApellido()).build()
						: null;
			}).orElse(null);

			Integer eccId = eccOpt.map(EmpresaClienteContadorEntity::getId).orElse(null);

			List<HistoricoLecturaDTO> dtos = entities.stream().map(h -> {

				ContadorDTO contadorDTO = ContadorDTO.builder().id(h.getContador().getId())
						.idEmpresaClienteContador(eccId).cliente(clienteDTO).serial(h.getContador().getSerial())
						.fechaInstalacion(h.getContador().getFechaInstalacion()).build();

				return HistoricoLecturaDTO.builder().id(h.getId())
						.lectura(
								h.getLectura() != null ? LecturaDTO.builder().id(h.getLectura().getId()).build() : null)
						.contador(contadorDTO).consumo(h.getConsumo()).fechaLectura(h.getFechaLectura())
						.consumoAnormal(h.getConsumoAnormal()).descripcion(h.getDescripcion()).activo(h.getActivo())
						.usuarioCreacion(h.getUsuarioCreacion()).fechaCreacion(h.getFechaCreacion())
						.usuarioModificacion(h.getUsuarioModificacion()).fechaModificacion(h.getFechaModificacion())
						.build();
			}).toList();

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
					.code(HttpStatus.OK.value()).response(dtos).totalCount((long) dtos.size()).build());

		} catch (Exception e) {
			log.error("Error al buscar histórico de lectura por idLectura={}", idLectura, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

}
