package com.aqua.plus.api.helpers;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.aqua.plus.api.service.impl.FacturaServiceImpl;
import com.aqua.plus.commons.entities.EmpresaEntity;
import com.aqua.plus.commons.repositories.EmpresaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmpresaHelper {
	
	@Value("${dian.usuario}")
	private String usuario;
	
	private final EmpresaRepository empresaRepository;
	private final FacturaServiceImpl facturaServiceImpl;
	
	@Async("empresaMasivaExecutor")
	@Transactional
	public void procesar(EmpresaEntity empresa) {

		this.facturaServiceImpl.generarFacturasMasivas(empresa.getId(), usuario);
	}
	
	@Transactional
    public List<EmpresaEntity> consultarEmpresasFacturar(Boolean facAutomatica, Integer diaCorte) {
        return this.empresaRepository.obtenerEmpresasFacturacionAutomatica(facAutomatica, diaCorte);
    }
}
