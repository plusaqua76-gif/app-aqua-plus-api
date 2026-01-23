package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.ICuentaTotalService;
import com.aqua.plus.commons.dtos.CategoriaCuentaDTO;
import com.aqua.plus.commons.dtos.CuentaTotalDTO;
import com.aqua.plus.commons.dtos.EmpresaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.CuentaTotalEntity;
import com.aqua.plus.commons.repositories.CuentaTotalRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CuentaTotalServiceImpl implements ICuentaTotalService {

    private final CuentaTotalRepository cuentaTotalRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findCuentasTotales(Integer idEmpresa, Date fechaInicio, Date fechaFin,
            Integer page, Integer size) {

        log.info("Consultar cuentas totales: idEmpresa={}, fechaInicio={}, fechaFin={}, page={}, size={}",
                idEmpresa, fechaInicio, fechaFin, page, size);

        try {
            if (idEmpresa == null) {
                return ResponseEntity.badRequest().body(ResponseDTO.builder()
                        .success(false)
                        .message("Parámetro requerido: idEmpresa")
                        .code(HttpStatus.BAD_REQUEST.value())
                        .build());
            }

            int pageNumber = (page == null || page < 0) ? 0 : page;
            int pageSize = (size == null || size <= 0) ? 10 : size;
            Pageable pageable = PageRequest.of(pageNumber, pageSize);

            Page<CuentaTotalEntity> resultPage;

            if (fechaInicio != null && fechaFin != null) {
                if (fechaInicio.after(fechaFin)) {
                    return ResponseEntity.badRequest().body(ResponseDTO.builder()
                            .success(false)
                            .message("fechaInicio no puede ser mayor que fechaFin")
                            .code(HttpStatus.BAD_REQUEST.value())
                            .build());
                }

                resultPage = cuentaTotalRepository
                        .findByEmpresa_IdAndFechaCreacionBetweenOrderByFechaCreacionDesc(
                                idEmpresa, fechaInicio, fechaFin, pageable);

            } else {
                resultPage = cuentaTotalRepository
                        .findByEmpresa_IdOrderByFechaCreacionDesc(idEmpresa, pageable);
            }

            if (resultPage == null || resultPage.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.builder()
                        .success(false)
                        .message("No se encontraron cuentas totales para los filtros indicados")
                        .code(HttpStatus.NOT_FOUND.value())
                        .totalCount(0L)
                        .pageSize(pageable.getPageSize())
                        .currentPage(pageable.getPageNumber())
                        .totalPages(0)
                        .response(List.of())
                        .build());
            }

            List<CuentaTotalDTO> dtos = resultPage.getContent().stream().map(ct -> {

                EmpresaDTO empresaDTO = null;
                if (ct.getEmpresa() != null) {
                    var e = ct.getEmpresa();
                    empresaDTO = EmpresaDTO.builder()
                            .id(e.getId())
                            .build();
                }

                CategoriaCuentaDTO categoriaDTO = null;
                if (ct.getCategoria() != null) {
                    var cat = ct.getCategoria();
                    categoriaDTO = CategoriaCuentaDTO.builder()
                            .id(cat.getId())
                            .nombre(cat.getNombre()) // si existe en tu entidad CategoriaCuentaEntity
                            .build();
                }

                return CuentaTotalDTO.builder()
                        .id(ct.getId())
                        .empresa(empresaDTO)
                        .categoria(categoriaDTO)
                        .total(ct.getTotal())
                        .activo(ct.getActivo())
                        .usuarioCreacion(ct.getUsuarioCreacion())
                        .fechaCreacion(ct.getFechaCreacion())
                        .usuarioModificacion(ct.getUsuarioModificacion())
                        .fechaModificacion(ct.getFechaModificacion())
                        .build();

            }).toList();

            return ResponseEntity.ok(ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtos)
                    .totalCount(resultPage.getTotalElements())
                    .pageSize(resultPage.getSize())
                    .currentPage(resultPage.getNumber())
                    .totalPages(resultPage.getTotalPages())
                    .build());

        } catch (Exception e) {
            log.error("Error consultando cuentas totales", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.ERROR_QUERY_RECORD_BY_ID)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }
    }
}
