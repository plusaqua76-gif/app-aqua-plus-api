package com.aqua.plus.api.service.impl.specification;

import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.CorreoGeneralEntity;
import com.aqua.plus.commons.entities.EmpleadoEmpresaEntity;
import com.aqua.plus.commons.entities.TelefonoGeneralEntity;


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
	public static Specification<EmpleadoEmpresaEntity> personaNombreCompletoLike(String personaNombreCompleto) {
		if (personaNombreCompleto == null || personaNombreCompleto.isBlank())
			return null;

		return (root, cq, cb) -> {
			var cli = root.join("persona");

			var pNombre = cb.coalesce(cli.get("nombre"), "");
			var sNombre = cb.coalesce(cli.get("segundoNombre"), "");
			var pApellido = cb.coalesce(cli.get("apellido"), "");
			var sApellido = cb.coalesce(cli.get("segundoApellido"), "");

			var part1 = cb.concat(pNombre, cb.literal(" "));
			var part2 = cb.concat(sNombre, cb.literal(" "));
			var part3 = cb.concat(pApellido, cb.literal(" "));
			var fullNameRaw = cb.concat(cb.concat(cb.concat(part1, part2), part3), sApellido);

			var fullNameSpNorm = cb.function("regexp_replace", String.class, fullNameRaw, cb.literal("\\s+"),
					cb.literal(" "), cb.literal("g"));

			var fullNameLower = cb.lower(fullNameSpNorm);

			String pattern = "%" + personaNombreCompleto.toLowerCase().trim().replaceAll("\\s+", " ") + "%";
			return cb.like(fullNameLower, pattern);
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
