package com.aqua.plus.api.wompi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WompiFeeCalculatorTest {

    @Test
    void ejemplo100000() {
        WompiFeeCalculator.FeeBreakdown fee = WompiFeeCalculator.calcular(100_000.0);
        assertEquals(10_000_000L, fee.getFacturaAmountInCents());
        assertEquals(265_000L, fee.getPorcentajeInCents());
        assertEquals(70_000L, fee.getFijoInCents());
        assertEquals(335_000L, fee.getComisionInCents());
        assertEquals(63_650L, fee.getIvaInCents());
        assertEquals(398_650L, fee.getFeeTotalInCents());
        assertEquals(10_398_650L, fee.getTotalAmountInCents());
    }

    @Test
    void ejemplo20000() {
        WompiFeeCalculator.FeeBreakdown fee = WompiFeeCalculator.calcular(20_000.0);
        assertEquals(146_370L, fee.getFeeTotalInCents());
        assertEquals(2_146_370L, fee.getTotalAmountInCents());
    }

    @Test
    void ejemplo50000() {
        WompiFeeCalculator.FeeBreakdown fee = WompiFeeCalculator.calcular(50_000.0);
        assertEquals(240_975L, fee.getFeeTotalInCents());
        assertEquals(5_240_975L, fee.getTotalAmountInCents());
    }

    @Test
    void factura215000() {
        WompiFeeCalculator.FeeBreakdown fee = WompiFeeCalculator.calcular(215_000.0);
        assertEquals(21_500_000L, fee.getFacturaAmountInCents());
        assertEquals(761_303L, fee.getFeeTotalInCents());
        assertEquals(22_261_303L, fee.getTotalAmountInCents());
    }

    @Test
    void precioInvalido() {
        assertThrows(IllegalArgumentException.class, () -> WompiFeeCalculator.calcular(0.0));
        assertThrows(IllegalArgumentException.class, () -> WompiFeeCalculator.calcular(null));
    }
}
