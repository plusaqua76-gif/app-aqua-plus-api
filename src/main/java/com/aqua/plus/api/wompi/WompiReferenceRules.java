package com.aqua.plus.api.wompi;

import java.util.Optional;

/**
 * Referencia canónica: {@code FLU-{facturaId}-...}.
 * Se acepta también {@code FAC-} de checkouts ya emitidos.
 */
public final class WompiReferenceRules {

    public static final String PREFIJO = "FLU";
    private static final String PREFIJO_LEGADO = "FAC";

    private WompiReferenceRules() {
    }

    public static boolean perteneceAFactura(String reference, Integer facturaId) {
        if (facturaId == null) {
            return false;
        }
        return extraerFacturaId(reference).filter(facturaId::equals).isPresent();
    }

    public static Optional<Integer> extraerFacturaId(String reference) {
        if (reference == null || reference.isBlank()) {
            return Optional.empty();
        }
        String[] partes = reference.trim().split("-");
        if (partes.length < 2) {
            return Optional.empty();
        }
        if (!PREFIJO.equalsIgnoreCase(partes[0]) && !PREFIJO_LEGADO.equalsIgnoreCase(partes[0])) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.valueOf(partes[1]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
