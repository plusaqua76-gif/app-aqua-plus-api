package com.aqua.plus.api.wompi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WompiSignatureServiceTest {

    private final WompiSignatureService service = new WompiSignatureService();

    @Test
    void generaFirmaIntegridadConOrdenDocumentado() {
        // reference + amountInCents + currency + integritySecret
        String expected = service.sha256("FAC-84521-20260826-A8F3121500000COPintegrity-secret");

        String actual = service.generarFirmaIntegridad(
                "FAC-84521-20260826-A8F31",
                21500000L,
                "COP",
                "integrity-secret");

        assertEquals(expected, actual);
        assertEquals(64, actual.length());
    }

    @Test
    void rechazaParametrosNulos() {
        assertThrows(IllegalArgumentException.class,
                () -> service.generarFirmaIntegridad(null, 1L, "COP", "secret"));
    }
}
