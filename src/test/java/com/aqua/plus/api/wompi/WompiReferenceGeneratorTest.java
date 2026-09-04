package com.aqua.plus.api.wompi;

import com.aqua.plus.commons.repositories.PagoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WompiReferenceGeneratorTest {

    @Mock PagoRepository pagoRepository;

    @Test
    void generaFormatoFac() {
        when(pagoRepository.existsByReferencia(anyString())).thenReturn(false);
        WompiReferenceGenerator generator = new WompiReferenceGenerator(pagoRepository);
        String ref = generator.generar(84521);
        assertTrue(ref.matches("FAC-84521-\\d{8}-[0-9A-F]{5}"));
    }

    @Test
    void fallaSiTodasDuplicadas() {
        when(pagoRepository.existsByReferencia(anyString())).thenReturn(true);
        WompiReferenceGenerator generator = new WompiReferenceGenerator(pagoRepository);
        assertThrows(IllegalStateException.class, () -> generator.generar(84521));
    }
}
