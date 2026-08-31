package com.aqua.plus.api.wompi;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcula el cargo Wompi a trasladar al pagador.
 * <p>
 * Comisión = (valorFactura × 2,65%) + $700<br>
 * IVA 19% se aplica solo sobre la comisión<br>
 * Total a cobrar = valorFactura + comisión + IVA
 */
public final class WompiFeeCalculator {

    private static final BigDecimal PORCENTAJE = new BigDecimal("0.0265");
    private static final BigDecimal FIJO = new BigDecimal("700");
    private static final BigDecimal IVA = new BigDecimal("0.19");
    private static final RoundingMode ROUND = RoundingMode.HALF_UP;

    private WompiFeeCalculator() {
    }

    public static FeeBreakdown calcular(Double precioFactura) {
        if (precioFactura == null || precioFactura <= 0) {
            throw new IllegalArgumentException("El precio de la factura debe ser positivo");
        }

        BigDecimal factura = BigDecimal.valueOf(precioFactura).setScale(2, ROUND);
        BigDecimal porcentaje = factura.multiply(PORCENTAJE).setScale(2, ROUND);
        BigDecimal comision = porcentaje.add(FIJO).setScale(2, ROUND);
        BigDecimal iva = comision.multiply(IVA).setScale(2, ROUND);
        BigDecimal costoTotal = comision.add(iva).setScale(2, ROUND);
        BigDecimal totalCobrar = factura.add(costoTotal).setScale(2, ROUND);

        return new FeeBreakdown(
                aCentavos(factura),
                aCentavos(porcentaje),
                aCentavos(FIJO),
                aCentavos(comision),
                aCentavos(iva),
                aCentavos(costoTotal),
                aCentavos(totalCobrar));
    }

    private static long aCentavos(BigDecimal pesos) {
        return pesos.movePointRight(2).setScale(0, ROUND).longValueExact();
    }

    public static final class FeeBreakdown {
        private final long facturaAmountInCents;
        private final long porcentajeInCents;
        private final long fijoInCents;
        private final long comisionInCents;
        private final long ivaInCents;
        private final long feeTotalInCents;
        private final long totalAmountInCents;

        public FeeBreakdown(
                long facturaAmountInCents,
                long porcentajeInCents,
                long fijoInCents,
                long comisionInCents,
                long ivaInCents,
                long feeTotalInCents,
                long totalAmountInCents) {
            this.facturaAmountInCents = facturaAmountInCents;
            this.porcentajeInCents = porcentajeInCents;
            this.fijoInCents = fijoInCents;
            this.comisionInCents = comisionInCents;
            this.ivaInCents = ivaInCents;
            this.feeTotalInCents = feeTotalInCents;
            this.totalAmountInCents = totalAmountInCents;
        }

        public long getFacturaAmountInCents() {
            return facturaAmountInCents;
        }

        public long getPorcentajeInCents() {
            return porcentajeInCents;
        }

        public long getFijoInCents() {
            return fijoInCents;
        }

        public long getComisionInCents() {
            return comisionInCents;
        }

        public long getIvaInCents() {
            return ivaInCents;
        }

        public long getFeeTotalInCents() {
            return feeTotalInCents;
        }

        public long getTotalAmountInCents() {
            return totalAmountInCents;
        }
    }
}
