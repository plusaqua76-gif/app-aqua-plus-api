package com.aqua.plus.api.jobs;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aqua.plus.api.helpers.EmpresaHelper;
import com.aqua.plus.api.utils.Utils;
import com.aqua.plus.commons.entities.EmpresaEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
@RequiredArgsConstructor
@Slf4j
public class FacturaMasivaAutomaticaScheduler {
	
	@Value("${app.jobs.facturas.electronica.scheduled}")
	private String scheduled;

	private final EmpresaHelper empresaHelper;

	//@Scheduled(cron  = "${app.jobs.facturas.masiva.automatica.scheduled}", zone="America/Bogota")
	@Scheduled(fixedDelayString = "${app.jobs.facturas.masiva.automatica.scheduled}")
	@SchedulerLock(name = "facturacion_masiva_auto")
	public void procesarEmpresaAutomaticas() {
		log.warn("Inicio metodo procesarEmpresaAutomaticas");
	    List<EmpresaEntity> empresas = empresaHelper.consultarEmpresasFacturar(Boolean.TRUE, Utils.obtenerFechaActual());

	    for (EmpresaEntity item : empresas) {
	    	empresaHelper.procesar(item);
	    }
		log.warn("Fin metodo procesarEmpresaAutomaticas");
	}
}
