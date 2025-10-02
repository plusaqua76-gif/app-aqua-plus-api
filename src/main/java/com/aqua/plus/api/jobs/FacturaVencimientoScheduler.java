package com.aqua.plus.api.jobs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

	@Value("${app.jobs.facturas.param.llave:DIAS}")
	private String llaveParametro;

	@Value("${app.jobs.facturas.param.default-dias:30}")
	private int defaultDias;

	@Value("${app.jobs.facturas.avsu.dias:60}")
	private int avsuDias;

	@Transactional
	public void marcarFacturasVencidas() {
		var estadoVen = estadoRepository.findByCodigoIgnoreCaseAndActivoTrue("VEN")
				.orElseThrow(() -> new IllegalStateException("No existe estado VEN activo"));

		long candidatas = facturaRepository.contarPendientesVencidasPorParametro(llaveParametro, defaultDias);
		int actualizadas = facturaRepository.marcarVencidasPorParametro(llaveParametro, defaultDias, estadoVen.getId(),
				usuarioCron);

		log.info("Job marcarFacturasVencidas: candidatas={}, actualizadas={}, estadoVEN={}, llave={}, defaultDias={}",
				candidatas, actualizadas, estadoVen.getId(), llaveParametro, defaultDias);

		var estadoAvsu = estadoRepository.findByCodigoIgnoreCaseAndActivoTrue("AVSU")
				.orElseThrow(() -> new IllegalStateException("No existe estado AVSU activo"));

		int promovidas = facturaRepository.promoverVencidasAAviso(avsuDias, estadoVen.getId(), estadoAvsu.getId(),
				usuarioCron);

		log.info("Job promover a AVSU: promovidasVEN->AVSU={}, dias={}", promovidas, avsuDias);
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
