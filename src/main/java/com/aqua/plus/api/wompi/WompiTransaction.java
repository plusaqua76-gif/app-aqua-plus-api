package com.aqua.plus.api.wompi;

/**
 * Transacción consultada en la API pública de Wompi ({@code GET /v1/transactions/{id}}).
 */
public record WompiTransaction(
        String id,
        String status,
        String reference,
        Long amountInCents,
        String currency,
        String paymentMethodType
) {}
