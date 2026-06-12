// package com.aqua.plus.api.jobs;

// import com.aqua.plus.api.helpers.PagoHelper;
// import com.aqua.plus.api.service.external.IWompiService;
// import com.aqua.plus.commons.entities.PagoEntity;
// import com.aqua.plus.commons.repositories.PagoRepository;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Component;

// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Map;


// @Slf4j
// @Component
// @RequiredArgsConstructor
// @ConditionalOnProperty(name = "app.jobs.pagos.enabled", havingValue = "true", matchIfMissing = true)
// public class PagoScheduler {

//     private final PagoRepository  pagoRepo;
//     private final IWompiService   wompiService;
//     private final PagoHelper      pagoHelper;

//     private static final List<String> ESTADOS_TERMINALES =
//         List.of("APPROVED", "DECLINED", "ERROR", "VOIDED");

//     private static final List<String> MEDIOS_REDIRECT =
//         List.of("PSE", "BANCOLOMBIA_TRANSFER");

//     /**
//      * Cada 5 minutos consulta en Wompi los pagos PENDING dentro de una ventana de tiempo.
//      *
//      * Ventana: entre 5 y 15 minutos de antigüedad.
//      * - Mínimo 5 min: da tiempo a que el webhook llegue solo antes de intervenir.
//      * - Máximo 15 min: Wompi suele resolver en segundos; 15 min es el tiempo de
//      *   vigencia del redirect link. Pagos más antiguos están definitivamente
//      *   abandonados — no se reintentan por seguridad ni eficiencia.
//      *
//      * Si Wompi ya tiene estado terminal (APPROVED/DECLINED/ERROR/VOIDED),
//      * se actualiza la BD. Si Wompi aún dice PENDING pasadas 2 horas,
//      * el pago se queda como PENDING en la BD (sirve como auditoría).
//      */
//     @Scheduled(cron = "${app.jobs.pagos.sincronizar-cron:0 */5 * * * *}")
//     @SchedulerLock(name = "pago_sincronizar_pendientes", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
//     public void sincronizarPendientes() {
//         LocalDateTime umbralReciente = LocalDateTime.now().minusMinutes(5);    // más de 5 min
//         LocalDateTime umbralAntiguo  = LocalDateTime.now().minusMinutes(60);   // menos de 60 min

//         List<PagoEntity> pendientes = pagoRepo.buscarPendientesSinResolver(umbralReciente, umbralAntiguo);

//         if (pendientes.isEmpty()) {
//             return;
//         }

//         log.info("PagoScheduler — sincronizando {} pago(s) PENDING (ventana: 5min–15min)", pendientes.size());

//         for (PagoEntity pago : pendientes) {
//             try {
//                 Map<String, Object> txData = wompiService.consultarTransaccion(pago.getIdTransaccionWompi());
//                 String estadoWompi = (String) txData.get("status");
//                 String metodoPago  = (String) txData.get("payment_method_type");

//                 if (estadoWompi != null && ESTADOS_TERMINALES.contains(estadoWompi)
//                         && !estadoWompi.equals(pago.getEstado())) {
//                     pagoRepo.actualizarEstado(
//                         pago.getReferencia(), estadoWompi,
//                         pago.getIdTransaccionWompi(), metodoPago, "SYNC_JOB");
//                     log.info("PagoScheduler — {} → {} (referencia: {})",
//                         pago.getEstado(), estadoWompi, pago.getReferencia());
//                 }

//                 // Para PSE/Bancolombia sin redirect_url aún disponible, reintentar en background.
//                 // Solo tiene sentido dentro de los primeros 15 minutos (vigencia del link).
//                 if (MEDIOS_REDIRECT.contains(pago.getMetodoPago())
//                         && pago.getRedirectUrlWompi() == null
//                         && pago.getFechaCreacion().isAfter(LocalDateTime.now().minusMinutes(15))) {
//                     pagoHelper.obtenerYGuardarRedirectUrl(
//                         pago.getIdTransaccionWompi(), pago.getReferencia());
//                 }

//             } catch (Exception e) {
//                 log.warn("PagoScheduler — error sincronizando referencia {}: {}",
//                     pago.getReferencia(), e.getMessage());
//             }
//         }
//     }
// }
