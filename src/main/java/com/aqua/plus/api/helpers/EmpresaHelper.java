package com.aqua.plus.api.helpers;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.aqua.plus.api.service.impl.FacturaServiceImpl;
import com.aqua.plus.api.utils.Utils;
import com.aqua.plus.commons.entities.EmpresaEntity;
import com.aqua.plus.commons.entities.ParametrosEmpresaEntity;
import com.aqua.plus.commons.enums.ParametroEmpresaEnum;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import com.aqua.plus.commons.repositories.EmpresaRepository;
import com.aqua.plus.commons.repositories.ParametrosEmpresaRepository;
import com.aqua.plus.commons.utils.Constantes;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmpresaHelper {
	
	@Value("${dian.usuario}")
	private String usuario;
	
	private final EmpresaRepository empresaRepository;
	private final FacturaServiceImpl facturaServiceImpl;
	private final ParametrosEmpresaRepository parametrosEmpresaRepository;
	
	@Async("empresaMasivaExecutor")
	@Transactional
	public void procesar(EmpresaEntity empresa) {
		EmpresaEntity entity = empresaRepository.findByIdForUpdate(empresa.getId());
		entity.setFechaProximoCorte(Utils.sumarMes(entity.getFechaProximoCorte(), obtenerMesesPeriodo(empresa.getId())));
		this.facturaServiceImpl.generarFacturasMasivas(empresa.getId(), usuario);
	}
	
	@Transactional
    public List<EmpresaEntity> consultarEmpresasFacturar(Boolean facAutomatica, Date fechaProximoCorte) {
        return this.empresaRepository.obtenerEmpresasFacturacionAutomatica(facAutomatica, fechaProximoCorte);
    }
	
	public Integer obtenerMesesPeriodo(Integer idEmpresa) {
		ParametrosEmpresaEntity entity = this.parametrosEmpresaRepository
				.findFirstByEmpresa_IdAndLlaveAndActivoTrue(idEmpresa, ParametroEmpresaEnum.PERIODOS_FACT.getCodigo())
				.orElseThrow(() -> new ProcessGenericException(Constantes.PARAM_NOT_FOUND));
		return Integer.parseInt(entity.getValorParametro());
	}
}
