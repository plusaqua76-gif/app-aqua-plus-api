package com.aqua.plus.api.jobs;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aqua.plus.api.helpers.EmpresaHelper;
import com.aqua.plus.api.utils.Utils;
import com.aqua.plus.commons.entities.EmpresaEntity;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
@RequiredArgsConstructor
public class FacturaMasivaAutomaticaScheduler {
	
	@Value("${app.jobs.facturas.electronica.scheduled}")
	private String scheduled;

	private final EmpresaHelper empresaHelper;
	
	@Scheduled(cron  = "${app.jobs.facturas.masiva.automatica.scheduled}", zone="America/Bogota")
	@SchedulerLock(name = "facturacion_masiva_auto")
	public void iniciarFactura() {

	    List<EmpresaEntity> empresas = empresaHelper.consultarEmpresasFacturar(Boolean.TRUE, Utils.obtenerDiaActual());

	    for (EmpresaEntity item : empresas) {
	    	empresaHelper.procesar(item);
	    }
	}
}
