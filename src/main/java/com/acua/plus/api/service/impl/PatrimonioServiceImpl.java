package com.acua.plus.api.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acua.plus.api.service.IPatrimonioService;
import com.acua.plus.commons.dtos.PatrimonioDTO;
import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.entities.PatrimonioEntity;
import com.acua.plus.commons.maps.PatrimonioMapper;
import com.acua.plus.commons.repositories.PatrimonioRepository;
import com.acua.plus.commons.utils.Constantes;

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
public class PatrimonioServiceImpl implements IPatrimonioService {
	
	private final PatrimonioRepository patrimonioRepository;
	private final PatrimonioMapper patrimonioMapper;
	
	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findByEnterpriseId(Integer idEmpresa) {
	    log.info("Buscar patrimonio por id de empresa: {}", idEmpresa);
	    try {
	        List<PatrimonioEntity> patrimonio= patrimonioRepository.findByEmpresa_Id(idEmpresa);

	        if (patrimonio.isEmpty()) {
	            ResponseDTO responseDTO = ResponseDTO.builder()
	                    .success(false)
	                    .message("No se encontraron patrimonios para la empresa con id " + idEmpresa)
	                    .code(HttpStatus.NOT_FOUND.value())
	                    .build();
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
	        }

	        List<PatrimonioDTO> dtoList = patrimonioMapper.listEntityToDtoList(patrimonio);

	        ResponseDTO responseDTO = ResponseDTO.builder()
	                .success(true)
	                .message(Constantes.CONSULTED_SUCCESSFULLY)
	                .code(HttpStatus.OK.value())
	                .response(dtoList)
	                .build();

	        return ResponseEntity.ok(responseDTO);
	    } catch (Exception e) {
	        log.error("Error al buscar patrimonios por id de empresa: {}", idEmpresa, e);
	        ResponseDTO responseDTO = ResponseDTO.builder()
	                .success(false)
	                .message(Constantes.ERROR_QUERY_RECORD_BY_ID)
	                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
	                .build();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
	    }
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
	    log.info("Buscar patrimonio por id: {}", id);
	    try {
	        Optional<PatrimonioEntity> patrimonio = patrimonioRepository.findById(id);
	        if (patrimonio.isPresent()) {
	        	PatrimonioDTO dto = patrimonioMapper.entityToDto(patrimonio.get());
	            ResponseDTO responseDTO = ResponseDTO.builder()
	                    .success(true)
	                    .message(Constantes.CONSULTED_SUCCESSFULLY)
	                    .code(HttpStatus.OK.value())
	                    .response(dto)
	                    .build();
	            return ResponseEntity.ok(responseDTO);
	        } else {
	            ResponseDTO responseDTO = ResponseDTO.builder()
	                    .success(false)
	                    .message(Constantes.CONSULTING_ERROR)
	                    .code(HttpStatus.NOT_FOUND.value())
	                    .build();
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
	        }
	    } catch (Exception e) {
	        log.error("Error al buscar patrimonio por id: {}", id, e);
	        ResponseDTO responseDTO = ResponseDTO.builder()
	                .success(false)
	                .message(Constantes.ERROR_QUERY_RECORD_BY_ID)
	                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
	                .build();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
	    }
	}
	
	@Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findAll() {
        log.info("Listar todos los patrimonios");
        try {
            var list = patrimonioRepository.findAll();
            var dtoList = patrimonioMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar los patrimonios", e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.CONSULTING_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .response(null)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}
