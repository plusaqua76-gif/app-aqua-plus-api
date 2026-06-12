package com.aqua.plus.api.configs.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un endpoint como protegido por el sobre seguro (SecurePayloadFilter).
 *
 * Al anotar el método con {@code @SecuredEndpoint} se documenta
 * explícitamente que el controlador espera recibir el body ya descifrado
 * por el filtro — el payload original viene cifrado desde Angular.
 *
 * Uso:
 * <pre>
 *   {@literal @}PostMapping("/api/v1/secure/pago")
 *   {@literal @}SecuredEndpoint
 *   public ResponseEntity<ResponseDTO> crearPago(@RequestBody PagoRequestDTO dto) { ... }
 * </pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SecuredEndpoint {

    /** Descripción opcional del dato que se espera en el payload. */
    String value() default "";
}
