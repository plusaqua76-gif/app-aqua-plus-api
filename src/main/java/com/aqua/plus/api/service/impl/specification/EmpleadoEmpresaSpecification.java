package com.aqua.plus.api.service.impl.specification;

import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.CorreoGeneralEntity;
import com.aqua.plus.commons.entities.EmpleadoEmpresaEntity;
import com.aqua.plus.commons.entities.TelefonoGeneralEntity;

import jakarta.persistence.criteria.Expression;

/**
 * Specifications para EmpleadoEmpresaEntity.
 * Mantiene TODOS los filtros organizados y reutilizables.
 * 
 * @author npeñafiel
 * @version 0.1
 */

public class EmpleadoEmpresaSpecification {

	private EmpleadoEmpresaSpecification() { }

    public static Specification<EmpleadoEmpresaEntity> belongsToEmpresa(Integer empresaId) {
        return (root, query, cb) ->
            (empresaId == null) ? cb.conjunction() : cb.equal(root.get("empresa").get("id"), empresaId);
    }

    /** nombre completo: nombre [segundoNombre] apellido [segundoApellido] (LIKE, case-insensitive) */
    public static Specification<EmpleadoEmpresaEntity> personaNombreCompletoLike(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            var p = root.join("persona");

            Expression<String> fullName = cb.lower(
                cb.trim(
                    cb.concat(
                        cb.concat(
                            cb.concat(
                                cb.concat(cb.coalesce(p.get("nombre"), ""), cb.literal(" ")),
                                cb.coalesce(p.get("segundoNombre"), "")
                            ),
                            cb.literal(" ")
                        ),
                        cb.concat(
                            cb.coalesce(p.get("apellido"), ""),
                            cb.concat(cb.literal(" "), cb.coalesce(p.get("segundoApellido"), ""))
                        )
                    )
                )
            );
            return cb.like(fullName, "%" + q.toLowerCase().trim() + "%");
        };
    }

    public static Specification<EmpleadoEmpresaEntity> personaCedulaEquals(String cedula) {
        return (root, query, cb) -> {
            if (cedula == null || cedula.isBlank()) return cb.conjunction();
            var p = root.join("persona");
            return cb.equal(p.get("numeroCedula"), cedula.trim());
        };
    }

    public static Specification<EmpleadoEmpresaEntity> personaCodigoLike(String codigo) {
        return (root, query, cb) -> {
            if (codigo == null || codigo.isBlank()) return cb.conjunction();
            var p = root.join("persona");
            return cb.like(cb.lower(p.get("codigo")), "%" + codigo.toLowerCase().trim() + "%");
        };
    }

    /** estado: "ACTIVO"|"INACTIVO" → Boolean en Persona.activo */
    public static Specification<EmpleadoEmpresaEntity> personaEstadoEquals(Boolean activo) {
        return (root, query, cb) -> {
            if (activo == null) return cb.conjunction();
            var p = root.join("persona");
            return cb.equal(p.get("activo"), activo);
        };
    }

    /** Teléfono LIKE (existe algún teléfono activo que contenga el patrón) */
    public static Specification<EmpleadoEmpresaEntity> telefonoLike(String telefono) {
        return (root, query, cb) -> {
            if (telefono == null || telefono.isBlank()) return cb.conjunction();
            var p = root.join("persona");

            var sub = query.subquery(Integer.class);
            var t = sub.from(TelefonoGeneralEntity.class);
            sub.select(cb.literal(1));
            sub.where(
                cb.equal(t.get("persona").get("id"), p.get("id")),
                cb.isTrue(t.get("activo")),
                cb.like(cb.lower(t.get("numero")), "%" + telefono.toLowerCase().trim() + "%")
            );
            return cb.exists(sub);
        };
    }

    /** Correo LIKE (existe algún correo activo que contenga el patrón) */
    public static Specification<EmpleadoEmpresaEntity> correoLike(String correo) {
        return (root, query, cb) -> {
            if (correo == null || correo.isBlank()) return cb.conjunction();
            var p = root.join("persona");

            var sub = query.subquery(Integer.class);
            var c = sub.from(CorreoGeneralEntity.class);
            sub.select(cb.literal(1));
            sub.where(
                cb.equal(c.get("persona").get("id"), p.get("id")),
                cb.isTrue(c.get("activo")),
                cb.like(cb.lower(c.get("correo")), "%" + correo.toLowerCase().trim() + "%")
            );
            return cb.exists(sub);
        };
    }

    /** Helper opcional para encadenar specs ignorando nulls */
    @SafeVarargs
    public static <T> Specification<T> allOfNonNull(Specification<T>... specs) {
        Specification<T> result = (root, query, cb) -> cb.conjunction();
        for (Specification<T> s : java.util.Arrays.stream(specs).filter(Objects::nonNull).toList()) {
            result = result.and(s);
        }
        return result;
    }
}
