package com.aqua.plus.api.tx;

import com.aqua.plus.commons.repositories.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class EmpresaTxComponent {

    private final EmpresaRepository empresaRepository;

    @Transactional
    public boolean actualizarFechaCorte(Integer id, Date fecha) {
        int updated = empresaRepository.actualizarFechaCorte(
                id, fecha
        );
        return updated > 0;
    }
}
