package com.aqua.plus.api.wompi;

import com.aqua.plus.commons.repositories.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class WompiReferenceGenerator {

    private static final DateTimeFormatter DIA = DateTimeFormatter.BASIC_ISO_DATE;
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    private static final int MAX_INTENTOS = 8;

    private final PagoRepository pagoRepository;
    private final SecureRandom random = new SecureRandom();

    /**
     * Genera referencia única: FAC-{facturaId}-{yyyyMMdd}-{5 hex}
     */
    public String generar(Integer facturaId) {
        String dia = LocalDate.now().format(DIA);
        for (int i = 0; i < MAX_INTENTOS; i++) {
            String referencia = "FAC-" + facturaId + "-" + dia + "-" + randomHex(5);
            if (!pagoRepository.existsByReferencia(referencia)) {
                return referencia;
            }
        }
        throw new IllegalStateException("No se pudo generar una referencia única para la factura " + facturaId);
    }

    private String randomHex(int length) {
        char[] out = new char[length];
        for (int i = 0; i < length; i++) {
            out[i] = HEX[random.nextInt(HEX.length)];
        }
        return new String(out);
    }
}
