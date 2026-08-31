package com.aqua.plus.api.wompi;

/**
 * Configuración Wompi de una empresa (secretos ya descifrados).
 * La clave privada no existe en Web Checkout.
 */
public record WompiEmpresaConfig(
        String publicKey,
        String integritySecret,
        String eventSecret,
        String checkoutUrl,
        String redirectUrl
) {}
