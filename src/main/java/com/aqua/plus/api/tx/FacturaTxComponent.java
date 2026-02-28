package com.aqua.plus.api.tx;

import com.aqua.plus.commons.repositories.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FacturaTxComponent {

    @Value("${dian.estados.en-proceso}")
    private String estadoEnProceso;

    @Value("${dian.estados.pendiente}")
    private String estadoPendiente;

    private final InvoiceRepository facturaRepository;

    @Transactional
    public boolean marcarEnProceso(Integer id) {
        int updated = facturaRepository.marcarEnProcesoDirecto(
                id, estadoEnProceso, estadoPendiente
        );
        return updated > 0;
    }

    @Transactional
    public void actualizarEstadoFinal(Integer id, String estado) {
        facturaRepository.actualizarEstado(id, estado);
    }
}
