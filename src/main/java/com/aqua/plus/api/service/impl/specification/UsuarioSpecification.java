package com.aqua.plus.api.service.impl.specification;

import java.util.Collection;
import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.UsuarioEntity;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/**
 * @author nicope
 * @version 1.0
 *
 * Especificaciones JPA reutilizables para filtrar Usuarios en consultas; combinables dinámicamente.
 */

public final class UsuarioSpecification {

	private UsuarioSpecification() {}
	
	/** nombre LIKE (case-insensitive) */
    public static Specification<UsuarioEntity> nombreLike(String nombre) {
        if (nombre == null || nombre.isBlank()) return null;
        String like = "%" + nombre.trim().toUpperCase() + "%";
        return (root, query, cb) -> cb.like(cb.upper(root.get("nombre")), like);
    }

    /** estado.nombre = value (case-insensitive) */
    public static Specification<UsuarioEntity> estadoNombreEquals(String estadoNombre) {
        if (estadoNombre == null || estadoNombre.isBlank()) return null;
        String wanted = estadoNombre.trim().toUpperCase();
        return (root, query, cb) -> {
            Join<Object, Object> est = root.join("estado", JoinType.LEFT);
            return cb.equal(cb.upper(est.get("nombre")), wanted);
        };
    }

    /** estado.nombre IN (values) (case-insensitive) */
    public static Specification<UsuarioEntity> estadoNombreIn(Collection<String> nombres) {
        if (nombres == null || nombres.isEmpty()) return null;
        var uppers = nombres.stream()
                .filter(Objects::nonNull)
                .map(s -> s.trim().toUpperCase())
                .filter(s -> !s.isBlank())
                .toList();
        if (uppers.isEmpty()) return null;

        return (root, query, cb) -> {
            Join<Object, Object> est = root.join("estado", JoinType.LEFT);
            Predicate[] ors = uppers.stream()
                    .map(u -> cb.equal(cb.upper(est.get("nombre")), u))
                    .toArray(Predicate[]::new);
            return cb.or(ors);
        };
    }
}
