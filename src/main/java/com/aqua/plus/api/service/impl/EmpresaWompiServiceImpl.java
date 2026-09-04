package com.aqua.plus.api.service.impl;

import java.util.Collections;
import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IEmpresaWompiService;
import com.aqua.plus.api.utils.EncriptarDesencriptar;
import com.aqua.plus.commons.dtos.EmpresaWompiDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.EmpresaEntity;
import com.aqua.plus.commons.entities.EmpresaWompiEntity;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import com.aqua.plus.commons.maps.EmpresaWompiMapper;
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
    private final EmpresaRepository empresaRepository;
    private final EmpresaWompiMapper empresaWompiMapper;
    private final EncriptarDesencriptar encriptarDesencriptar;

    @Override
    @Transactional
    public ResponseEntity<ResponseDTO> guardar(EmpresaWompiDTO dto) {
        log.info("Guardar/actualizar credenciales Wompi empresa id={}", dto.getIdEmpresa());

        EmpresaEntity empresa = empresaRepository.findById(dto.getIdEmpresa())
                .orElseThrow(() -> new ProcessGenericException(
                        "Empresa no encontrada con id: " + dto.getIdEmpresa()));

        EmpresaWompiEntity entity = empresaWompiRepository
                .findByEmpresa_Id(dto.getIdEmpresa())
                .orElse(new EmpresaWompiEntity());

        boolean isUpdate = entity.getId() != null;

        if (isUpdate) {
            empresaWompiMapper.updateEntityFromDto(dto, entity);
            entity.setFechaModificacion(new Date());
            entity.setUsuarioModificacion(dto.getUsuarioModificacion());
        } else {
            entity = empresaWompiMapper.dtoToEntity(dto);
            entity.setFechaCreacion(new Date());
            entity.setUsuarioCreacion(dto.getUsuarioCreacion());
            entity.setActivo(true);
        }

        entity.setEmpresa(empresa);
        entity.setWompiClavePublica(dto.getWompiClavePublica());
        entity.setWompiSecretoIntegridad(encriptarDesencriptar.encriptar(dto.getWompiSecretoIntegridad()));
        entity.setWompiSecretoEventos(encriptarDesencriptar.encriptar(dto.getWompiSecretoEventos()));
        entity.setCheckoutUrl(dto.getCheckoutUrl());
        entity.setRedirectUrl(dto.getRedirectUrl());

        empresaWompiRepository.save(entity);

        String message = isUpdate ? Constantes.UPDATED_SUCCESSFULLY : Constantes.SAVED_SUCCESSFULLY;
        int status = isUpdate ? HttpStatus.OK.value() : HttpStatus.CREATED.value();

        return ResponseEntity.status(status).body(ResponseDTO.builder()
                .success(true).message(message).code(status)
                .response(Collections.emptyMap()).build());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findByEmpresaId(Integer idEmpresa) {
        log.info("Buscar credenciales Wompi empresa id={}", idEmpresa);

        EmpresaWompiEntity entity = empresaWompiRepository.findByEmpresa_Id(idEmpresa)
                .orElseThrow(() -> new ProcessGenericException(
                        "No se encontraron credenciales Wompi para la empresa id: " + idEmpresa));

        EmpresaWompiDTO response = empresaWompiMapper.entityToDto(entity);
        response.setWompiSecretoIntegridad("***");
        response.setWompiSecretoEventos("***");

        return ResponseEntity.ok(ResponseDTO.builder()
                .success(true)
                .message(Constantes.CONSULTED_SUCCESSFULLY)
                .code(HttpStatus.OK.value())
                .response(response)
                .build());
    }
}
