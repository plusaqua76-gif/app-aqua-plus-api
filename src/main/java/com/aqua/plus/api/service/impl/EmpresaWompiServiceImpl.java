package com.aqua.plus.api.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IEmpresaWompiService;
import com.aqua.plus.commons.dtos.EmpresaWompiDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.EmpresaEntity;
import com.aqua.plus.commons.entities.EmpresaWompiEntity;
import com.aqua.plus.commons.repositories.EmpresaRepository;
import com.aqua.plus.commons.repositories.EmpresaWompiRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmpresaWompiServiceImpl implements IEmpresaWompiService {

    private final EmpresaWompiRepository empresaWompiRepository;
    private final EmpresaRepository      empresaRepository;

    @Override
    @Transactional
    public ResponseEntity<ResponseDTO> guardar(EmpresaWompiDTO dto) {
        log.info("Guardar/actualizar credenciales Wompi empresa id={}", dto.getIdEmpresa());
        try {
            Optional<EmpresaEntity> empresaOpt = empresaRepository.findById(dto.getIdEmpresa());
            if (empresaOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseDTO.builder().success(false)
                                .message("Empresa no encontrada con id: " + dto.getIdEmpresa())
                                .code(HttpStatus.NOT_FOUND.value()).build());
            }

            EmpresaEntity empresa = empresaOpt.get();
            EmpresaWompiEntity entity = empresaWompiRepository
                    .findByEmpresa_Id(dto.getIdEmpresa())
                    .orElse(new EmpresaWompiEntity());

            boolean isUpdate = entity.getId() != null;

            entity.setEmpresa(empresa);
            entity.setWompiClavePublica(dto.getWompiClavePublica());
            entity.setWompiClavePrivada(dto.getWompiClavePrivada());
            entity.setWompiSecretoIntegridad(dto.getWompiSecretoIntegridad());
            entity.setWompiSecretoEventos(dto.getWompiSecretoEventos());

            if (isUpdate) {
                entity.setFechaModificacion(new Date());
                entity.setUsuarioModificacion(dto.getUsuarioModificacion());
            } else {
                entity.setFechaCreacion(new Date());
                entity.setUsuarioCreacion(dto.getUsuarioCreacion());
                entity.setActivo(true);
            }

            empresaWompiRepository.save(entity);

            String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
            int statusCode = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

            return ResponseEntity.status(statusCode).body(ResponseDTO.builder()
                    .success(true).message(message).code(statusCode)
                    .response(Collections.emptyMap()).build());

        } catch (Exception e) {
            log.error("Error al guardar credenciales Wompi empresa id={}", dto.getIdEmpresa(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder()
                    .success(false).message(Constantes.SAVE_ERROR).code(HttpStatus.BAD_REQUEST.value())
                    .response(Map.of("exception", e.getClass().getSimpleName(),
                                     "detail", e.getMessage() != null ? e.getMessage() : "")).build());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findByEmpresaId(Integer idEmpresa) {
        log.info("Buscar credenciales Wompi empresa id={}", idEmpresa);
        try {
            Optional<EmpresaWompiEntity> opt = empresaWompiRepository.findByEmpresa_Id(idEmpresa);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.builder()
                        .success(false)
                        .message("No se encontraron credenciales Wompi para la empresa id: " + idEmpresa)
                        .code(HttpStatus.NOT_FOUND.value()).build());
            }

            EmpresaWompiEntity e = opt.get();
            EmpresaWompiDTO response = new EmpresaWompiDTO();
            response.setId(e.getId());
            response.setIdEmpresa(e.getEmpresa().getId());
            response.setWompiClavePublica(e.getWompiClavePublica());
            response.setWompiClavePrivada("***");
            response.setWompiSecretoIntegridad("***");
            response.setWompiSecretoEventos("***");
            response.setActivo(e.getActivo());

            return ResponseEntity.ok(ResponseDTO.builder().success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY).code(HttpStatus.OK.value())
                    .response(response).build());

        } catch (Exception e) {
            log.error("Error al buscar credenciales Wompi empresa id={}", idEmpresa, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder()
                    .success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
        }
    }
}
