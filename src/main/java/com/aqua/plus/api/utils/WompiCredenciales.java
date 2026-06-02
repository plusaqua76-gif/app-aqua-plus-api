package com.aqua.plus.api.utils;

public record WompiCredenciales(
        String clavePublica,
        String clavePrivada,
        String secretoIntegridad,
        String secretoEventos
) {}
