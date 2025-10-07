package com.aqua.plus.api.service.impl.specification;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.ReporteEntity;

/**
 * @author nicope
 * @version 1.0
 * 
 */

public class ReporteSpecifications {

	private ReporteSpecifications() {
	}

	/** nombre LIKE (case-insensitive) */
	public static Specification<ReporteEntity> nombreLike(String nombre) {
		if (nombre == null || nombre.isBlank())
			return null;
		String like = "%" + nombre.trim().toUpperCase() + "%";
		return (root, query, cb) -> cb.like(cb.upper(root.get("nombre")), like);
	}

	/** activo = true */
	public static Specification<ReporteEntity> activoTrue() {
		return (root, query, cb) -> cb.isTrue(root.get("activo"));
	}

	/** Helper para combinar solo los no-nulos */
	@SafeVarargs
	public static Specification<ReporteEntity> allOfNonNull(Specification<ReporteEntity>... specs) {
		Specification<ReporteEntity> result = null;
		for (Specification<ReporteEntity> s : specs) {
			if (s == null)
				continue;
			result = (result == null) ? s : result.and(s);
		}
		return result;
	}
}
