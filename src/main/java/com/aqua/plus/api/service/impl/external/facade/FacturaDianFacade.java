package com.aqua.plus.api.service.impl.external.facade;

import com.aqua.plus.api.utils.EncriptarDesencriptar;
import com.aqua.plus.api.utils.Utils;
import com.aqua.plus.commons.dtos.*;
import com.aqua.plus.commons.dtos.external.RequestFacturaDto;
import com.aqua.plus.commons.dtos.external.RequestInvoiceDto;
import com.aqua.plus.commons.dtos.external.ResponseInvoiceDto;
import com.aqua.plus.commons.entities.InvoiceEntity;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import com.aqua.plus.commons.maps.InvoiceMapper;
import com.aqua.plus.commons.repositories.InvoiceRepository;
import com.aqua.plus.commons.utils.Constantes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacturaDianFacade {

    private final EncriptarDesencriptar encriptarDesencriptar;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public void guardarFactura(final RequestFacturaDto request, PersonaDTO persona,
                               EmpresaDTO empresa, Long numeroFactura, String descripcion, ResponseInvoiceDto response,
                               RequestInvoiceDto rq) {
        log.info("Inicio metodo guardarFactura:{} ", numeroFactura);

        InvoiceDto invoiceDto = new InvoiceDto();
        invoiceDto.setId(request.getId());
        if (Objects.nonNull(response) && Objects.nonNull(response.getInvoice())) {
            invoiceDto.setEstado(response.getInvoice().getStatus());
            invoiceDto.setEstadoLegal(response.getInvoice().getLegalStatus());
            invoiceDto.setIdDian(response.getInvoice().getId());
            invoiceDto.setCufe(response.getInvoice().getCufe());
        }
        if (Objects.nonNull(response) && Objects.nonNull(response.getCreditNote())) {
            invoiceDto.setEstado(response.getCreditNote().getStatus());
            invoiceDto.setEstadoLegal(response.getCreditNote().getLegalStatus());
            invoiceDto.setIdDian(response.getCreditNote().getId());
            invoiceDto.setCufe(response.getCreditNote().getCufe());
        }
        invoiceDto.setCliente(persona);
        invoiceDto.setEmpresa(empresa);
        invoiceDto.setActivo(Boolean.TRUE);
        invoiceDto.setNumero(numeroFactura);
        invoiceDto.setDescripcion(descripcion);
        if(Objects.nonNull(request.getId())) {
            invoiceDto.setUsuarioModificacion(request.getUsuario());
        }else {
            invoiceDto.setUsuarioCreacion(request.getUsuario());
        }
        invoiceDto.setFechaUltimoIntento(request.getFechaUltimoIntento());
        rq.setResolution(null);
        invoiceDto.setData(this.encriptarDesencriptar.encriptar(Utils.objectToBase64(rq)));
        invoiceRepository.save(InvoiceMapper.INSTANCE.dtoToEntity(invoiceDto));
        log.info("Fin metodo guardarFactura:{} ", numeroFactura);
    }

    @Transactional
    public ResponseEntity<ResponseDTO> actualizarEstadoFactura(Long idEmpresa, String estadoActual, String nuevoEstado,
                                                               String usuario) {
        log.info("Inicio metodo actualizarEstadoFactura: {},{},{} ", idEmpresa, estadoActual, nuevoEstado);
        int canditadAfectado = this.invoiceRepository.actualizarEstadoPorEmpresa(idEmpresa, estadoActual, nuevoEstado,
                usuario);
        if (canditadAfectado > 0) {
            log.info("Fin metodo actualizarEstadoFactura: {},{},{},{} ", idEmpresa, estadoActual, nuevoEstado,
                    Constantes.UPDATED_SUCCESSFULLY);
            return new ResponseEntity<ResponseDTO>(
                    ResponseDTO.builder().code(HttpStatus.OK.value()).message(Constantes.UPDATED_SUCCESSFULLY).build(),
                    HttpStatus.OK);
        } else {
            log.info("Fin metodo actualizarEstadoFactura: {},{},{},{} ", idEmpresa, estadoActual, nuevoEstado,
                    Constantes.NOT_UPDATED_INVOICE);
            return new ResponseEntity<ResponseDTO>(
                    ResponseDTO.builder().code(HttpStatus.OK.value()).message(Constantes.NOT_UPDATED_INVOICE).build(),
                    HttpStatus.OK);
        }
    }

    public ResponseEntity<ResponseDTO> consultarFacturasPorEmpresa(Integer idEmpresa, Pageable pageable) {
        log.info("Inicio metodo consultarFacturasPorEmpresa:{},{},{}", idEmpresa, pageable.getPageSize(),
                pageable.getPageNumber());

        Page<InvoiceEntity> page = this.invoiceRepository.findByEmpresaId(idEmpresa, pageable);

        log.info("Fin metodo consultarFacturasPorEmpresa:{}, totalElements:{}, totalPages:{}", idEmpresa,
                page.getTotalElements(), page.getTotalPages());

        if (page.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseDTO.builder().success(false).code(HttpStatus.NOT_FOUND.value())
                            .message("No se encontraron facturas DIAN para la empresa con id " + idEmpresa)
                            .response(List.of()).totalCount(page.getTotalElements()).pageSize(page.getSize())
                            .currentPage(page.getNumber()).totalPages(page.getTotalPages()).build());
        }

        return ResponseEntity.ok(ResponseDTO.builder().success(true).code(HttpStatus.OK.value())
                .message(Constantes.CONSULTED_SUCCESSFULLY)
                .response(InvoiceMapper.INSTANCE.listEntityToDtoList(page.getContent()))
                .totalCount(page.getTotalElements()).pageSize(page.getSize()).currentPage(page.getNumber())
                .totalPages(page.getTotalPages()).build());
    }

    public ResponseEntity<ResponseDTO> consultarDataEnviaDian(final Integer id){
        InvoiceEntity invoice = this.invoiceRepository.findById(id).orElseThrow(() -> new ProcessGenericException(Constantes.FAC_NOT_FOUND));

        return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.OK.value())
                .message(HttpStatus.OK.name()).response(Utils.base64ToObject(encriptarDesencriptar.desencriptar(invoice.getData()))).build(), HttpStatus.OK);
    }
}
