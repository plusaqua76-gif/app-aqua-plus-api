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
 *          Especificaciones JPA reutilizables para filtrar Usuarios en
 *          consultas; combinables dinámicamente.
 */

public final class UsuarioSpecification {

	private UsuarioSpecification() {
	}

	/** nombre LIKE (case-insensitive) */
	public static Specification<UsuarioEntity> nombreLike(String nombre) {
		if (nombre == null || nombre.isBlank())
			return null;
		String like = "%" + nombre.trim().toUpperCase() + "%";
		return (root, query, cb) -> cb.like(cb.upper(root.get("nombre")), like);
	}

	/** estado.nombre = value (case-insensitive) */
	public static Specification<UsuarioEntity> estadoNombreEquals(String estadoNombre) {
		if (estadoNombre == null || estadoNombre.isBlank())
			return null;
		String wanted = estadoNombre.trim().toUpperCase();
		return (root, query, cb) -> {
			Join<Object, Object> est = root.join("estado", JoinType.LEFT);
			return cb.equal(cb.upper(est.get("nombre")), wanted);
		};
	}

	/** estado.nombre IN (values) (case-insensitive) */
	public static Specification<UsuarioEntity> estadoNombreIn(Collection<String> nombres) {
		if (nombres == null || nombres.isEmpty())
			return null;
		var uppers = nombres.stream().filter(Objects::nonNull).map(s -> s.trim().toUpperCase())
				.filter(s -> !s.isBlank()).toList();
		if (uppers.isEmpty())
			return null;

		return (root, query, cb) -> {
			Join<Object, Object> est = root.join("estado", JoinType.LEFT);
			Predicate[] ors = uppers.stream().map(u -> cb.equal(cb.upper(est.get("nombre")), u))
					.toArray(Predicate[]::new);
			return cb.or(ors);
		};
	}

	/** Combina specs ignorando nulos */
	@SafeVarargs
	public static Specification<UsuarioEntity> allOfNonNull(Specification<UsuarioEntity>... specs) {
		Specification<UsuarioEntity> out = null;
		for (var s : specs) {
			if (s == null)
				continue;
			out = (out == null) ? s : out.and(s);
		}
		return out;
	}

	/** Usuario.activo = true */
	public static Specification<UsuarioEntity> activoTrue() {
		return (root, q, cb) -> cb.isTrue(root.get("activo"));
	}

	/** p.id IN (...) */
	public static Specification<UsuarioEntity> personaIdIn(Collection<Integer> personaIds) {
		if (personaIds == null || personaIds.isEmpty())
			return null;
		return (root, q, cb) -> {
			var p = root.join("persona", JoinType.LEFT);
			q.distinct(true);
			return p.get("id").in(personaIds);
		};
	}

	/** Nombre completo LIKE (case-insensitive) sobre persona */
	public static Specification<UsuarioEntity> personaNombreLike(String nombreLike) {
		if (nombreLike == null || nombreLike.isBlank())
			return null;
		String like = "%" + nombreLike.trim().toUpperCase() + "%";
		return (root, q, cb) -> {
			var p = root.join("persona", JoinType.LEFT);
			q.distinct(true);

			var n1 = cb.coalesce(cb.upper(p.get("nombre")), "");
			var n2 = cb.coalesce(cb.upper(p.get("segundoNombre")), "");
			var a1 = cb.coalesce(cb.upper(p.get("apellido")), "");
			var a2 = cb.coalesce(cb.upper(p.get("segundoApellido")), "");

			var space = cb.literal(" ");
			var full = cb.function("concat", String.class, n1, space, n2, space, a1, space, a2);
			return cb.like(full, like);
		};
	}

	/** Número de documento LIKE */
	public static Specification<UsuarioEntity> personaNumeroCedulaLike(String numeroLike) {
		if (numeroLike == null || numeroLike.isBlank())
			return null;
		String like = "%" + numeroLike.trim().toUpperCase() + "%";
		return (root, q, cb) -> {
			var p = root.join("persona", JoinType.LEFT);
			q.distinct(true);
			return cb.like(cb.upper(p.get("numeroCedula")), like);
		};
	}

	/** Tipo de documento por nombre (equals case-insensitive) */
	public static Specification<UsuarioEntity> personaTipoDocNombreEquals(String tipoDocNombre) {
		if (tipoDocNombre == null || tipoDocNombre.isBlank())
			return null;
		String wanted = tipoDocNombre.trim().toUpperCase();
		return (root, q, cb) -> {
			var p = root.join("persona", JoinType.LEFT);
			var td = p.join("tipoDocumento", JoinType.LEFT);
			q.distinct(true);
			return cb.equal(cb.upper(td.get("nombre")), wanted);
		};
	}

	/** Rol por nombre (equals case-insensitive) */
	public static Specification<UsuarioEntity> rolNombreEquals(String rolNombre) {
		if (rolNombre == null || rolNombre.isBlank())
			return null;
		String wanted = rolNombre.trim().toUpperCase();
		return (root, q, cb) -> {
			var r = root.join("rol", JoinType.LEFT);
			q.distinct(true);
			return cb.equal(cb.upper(r.get("nombre")), wanted);
		};
	}

	/** Fetch joins para evitar proxies y N+1 */
	public static Specification<UsuarioEntity> withFetchJoins() {
		return (root, query, cb) -> {
			if (query.getResultType() != Long.class) {
				var pFetch = root.fetch("persona", JoinType.LEFT);
				pFetch.fetch("tipoDocumento", JoinType.LEFT);
				root.fetch("rol", JoinType.LEFT);
				root.fetch("estado", JoinType.LEFT);
				query.distinct(true);
			}
			return cb.conjunction();
		};
	}
}
