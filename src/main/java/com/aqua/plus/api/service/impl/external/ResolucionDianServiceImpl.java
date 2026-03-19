package com.aqua.plus.api.service.impl.external;

import com.aqua.plus.commons.dtos.ResolutionDto;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.external.RequestFacturaDto;
import com.aqua.plus.commons.entities.ResolutionEntity;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import com.aqua.plus.commons.maps.ResolucionMapper;
import com.aqua.plus.commons.repositories.ResolutionRepository;
import com.aqua.plus.commons.utils.Constantes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResolucionDianServiceImpl {

    private final ResolutionRepository resolutionRepository;

    @Transactional
    public ResponseEntity<ResponseDTO> guardarResolution(ResolutionDto resolution) {
        log.info("Inicio metodo guardarResolution: {} " , resolution);
        this.resolutionRepository.save(ResolucionMapper.INSTANCE.dtoToEntity(resolution));
        log.info("Fin metodo guardarResolution: {} " , resolution);
        return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.CREATED.value()).message(HttpStatus.CREATED.name()).build(), HttpStatus.CREATED);
    }

    public ResponseEntity<ResponseDTO> consultarResolucionPorId(Integer id) {
        log.info("Inicio metodo consultarResolucionPorId: {} " , id);
        ResolutionEntity entity = resolutionRepository.findByEmpresaId(id).orElseThrow(() -> new ProcessGenericException(Constantes.RESOLUTION_NOT_FOUND));
        return new ResponseEntity<ResponseDTO>(ResponseDTO.builder().code(HttpStatus.OK.value()).message(HttpStatus.OK.name()).response(ResolucionMapper.INSTANCE.entityToDto(entity)).build(), HttpStatus.CREATED);
    }

    @Transactional
    public Long actualizarResolucion(final RequestFacturaDto request, final ResolutionDto resolucion) {
        log.warn("Inicio metodo actualizarResolucion:{} ", request.getIdEmpresa());
        ResolutionEntity entityResolucion =this.resolutionRepository.findByIdForUpdate(resolucion.getId());

        if (entityResolucion.getNumeroActual() >= entityResolucion.getNumeroMaximo()) {
            throw new ProcessGenericException(Constantes.RANGE_DIAN_EXHAUSTED);
        }

        Long siguiente = entityResolucion.getNumeroActual() + 1;
        entityResolucion.setNumeroActual(siguiente);
        log.warn("Fin metodo actualizarResolucion:{},{} ", request.getIdEmpresa(), siguiente);

        return siguiente;
    }

    @Transactional(readOnly = true)
    public ResolutionDto getResolucion(final RequestFacturaDto request) {
        return ResolucionMapper.INSTANCE.entityToDto(this.resolutionRepository.findByEmpresaId(request.getIdEmpresa())
                .orElseThrow(() -> new ProcessGenericException(Constantes.RESOLUTION_NOT_FOUND)));
    }
}
