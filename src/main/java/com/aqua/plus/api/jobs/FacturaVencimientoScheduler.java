package com.aqua.plus.api.jobs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.commons.repositories.EstadoRepository;
import com.aqua.plus.commons.repositories.FacturaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ConditionalOnProperty(name = "app.jobs.facturas.enabled", havingValue = "true", matchIfMissing = true)
@Component
@RequiredArgsConstructor
@Slf4j
public class FacturaVencimientoScheduler {

	private final FacturaRepository facturaRepository;
	private final EstadoRepository estadoRepository;

	@Value("${app.jobs.facturas.usuario-cron:AquaPlus}")
	private String usuarioCron;

	//@Scheduled(cron = "${app.jobs.facturas.cron:0 0 1 * * *}")
	//@Scheduled(cron = "0 */2 * * * *")
	@Transactional
	public void marcarFacturasVencidas() {
		var estadoVen = estadoRepository.findByCodigoIgnoreCaseAndActivoTrue("VEN")
				.orElseThrow(() -> new IllegalStateException("No existe estado VEN activo"));

		long candidatas = facturaRepository.contarPendientesVencidasPorFechaFin();
		int actualizadas = facturaRepository.marcarPendientesComoVencidasPorFechaFin(estadoVen.getId(), usuarioCron);

		log.info("Job marcarFacturasVencidas (fecha_fin): candidatas={}, actualizadas={}, estadoVEN={}, usuario={}",
				candidatas, actualizadas, estadoVen.getId(), usuarioCron);

	}

	@Transactional
	public void marcarPendientesComoVencidas(String usuarioOverride) {
		String original = usuarioCron;
		if (usuarioOverride != null && !usuarioOverride.isBlank()) {
			usuarioCron = usuarioOverride;
		}
		try {
			marcarFacturasVencidas();
		} finally {
			usuarioCron = original;
		}
	}
}
