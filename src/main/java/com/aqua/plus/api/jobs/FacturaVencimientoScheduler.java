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

	// llave del parámetro y valor por defecto si la empresa no lo tiene
	@Value("${app.jobs.facturas.param.llave:DIAS}")
	private String llaveParametro;

	@Value("${app.jobs.facturas.param.default-dias:30}")
	private int defaultDias;

	@Scheduled(cron = "${app.jobs.facturas.vencidas-cron:0 0 0 * * *}", zone = "${app.jobs.tz:America/Bogota}")
	@Transactional
	public void marcarFacturasVencidas() {
		try {
			var estadoVen = estadoRepository.findByCodigoIgnoreCaseAndActivoTrue("VEN")
					.orElseThrow(() -> new IllegalStateException("No existe estado VEN activo"));

			long candidatas = facturaRepository.contarPendientesVencidasPorParametro(llaveParametro, defaultDias);

			int actualizadas = facturaRepository.marcarVencidasPorParametro(llaveParametro, defaultDias,
					estadoVen.getId(), usuarioCron);

			log.info(
					"Job marcarFacturasVencidas: candidatas={}, actualizadas={}, estadoVEN={}, llave={}, defaultDias={}",
					candidatas, actualizadas, estadoVen.getId(), llaveParametro, defaultDias);
		} catch (Exception e) {
			log.error("Job marcarFacturasVencidas falló", e);
		}
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
