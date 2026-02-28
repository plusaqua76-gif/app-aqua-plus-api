package com.aqua.plus.api.jobs;

import java.util.List;

import com.aqua.plus.commons.dtos.InvoiceDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aqua.plus.api.helpers.FacturaDianHelper;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
@RequiredArgsConstructor
public class FacturaElectronicaScheduler {
	
	@Value("${app.jobs.facturas.electronica.scheduled}")
	private String scheduled;

	private final FacturaDianHelper facturaDianHelper;
	
	@Scheduled(fixedDelayString = "${app.jobs.facturas.electronica.scheduled}")
	@SchedulerLock(name = "facturacion_auto",lockAtMostFor = "PT5M",
			lockAtLeastFor = "PT30S")
	public void iniciarFactura() {

	    List<InvoiceDto> facturas = facturaDianHelper.tomarFacturasPendientes();

	    for (InvoiceDto item : facturas) {
	    	facturaDianHelper.procesar(item);
	    }
	}

}
