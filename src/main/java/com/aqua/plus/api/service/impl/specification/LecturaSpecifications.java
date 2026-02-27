package com.aqua.plus.api.service.impl.specification;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.EmpresaClienteContadorEntity;
import com.aqua.plus.commons.entities.LecturaEntity;

/**
 * @author nicope
 * @version 1.0
 *
 *          Especificaciones JPA reutilizables para filtrar Lecturas en
 *          consultas; combinables dinámicamente.
 */

public final class LecturaSpecifications {

	private LecturaSpecifications() {
	}

	public static Specification<LecturaEntity> perteneceAEmpresa(Integer empresaId) {
		if (empresaId == null)
			return null;
		return (root, query, cb) -> {
			var sub = query.subquery(Integer.class);
			var ecc = sub.from(EmpresaClienteContadorEntity.class);
			sub.select(ecc.get("contador").get("id")).where(cb.equal(ecc.get("empresa").get("id"), empresaId),
					cb.isTrue(ecc.get("activo")));
			return root.get("contador").get("id").in(sub);
		};
	}

	public static Specification<LecturaEntity> serialLike(String serial) {
		if (serial == null || serial.isBlank())
			return null;
		return (root, query, cb) -> {
			var c = root.join("contador", jakarta.persistence.criteria.JoinType.INNER);
			return cb.like(cb.upper(c.get("serial")), "%" + serial.trim().toUpperCase() + "%");
		};
	}

	public static Specification<LecturaEntity> lecturaEquals(Integer lectura) {
		if (lectura == null)
			return null;
		return (root, query, cb) -> cb.equal(root.get("lectura"), lectura);
	}

	public static Specification<LecturaEntity> fechaBetween(java.time.LocalDate desde, java.time.LocalDate hasta) {
		if (desde == null && hasta == null)
			return null;
		return (root, query, cb) -> {
			if (desde != null && hasta != null) {
				var iDesde = desde.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
				var iHasta = hasta.atTime(java.time.LocalTime.MAX).atZone(java.time.ZoneId.systemDefault()).toInstant();
				return cb.between(root.get("fechaLectura"), java.util.Date.from(iDesde), java.util.Date.from(iHasta));
			} else if (desde != null) {
				var iDesde = desde.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
				return cb.greaterThanOrEqualTo(root.get("fechaLectura"), java.util.Date.from(iDesde));
			} else {
				var iHasta = hasta.atTime(java.time.LocalTime.MAX).atZone(java.time.ZoneId.systemDefault()).toInstant();
				return cb.lessThanOrEqualTo(root.get("fechaLectura"), java.util.Date.from(iHasta));
			}
		};
	}

	public static Specification<LecturaEntity> consumoAnormalEquals(Boolean consumoAnormal) {
		if (consumoAnormal == null)
			return null;
		return (root, query, cb) -> cb.equal(root.get("consumoAnormal"), consumoAnormal);
	}

	// “observacion” = campo descripcion
	public static Specification<LecturaEntity> observacionLike(String observacion) {
		if (observacion == null || observacion.isBlank())
			return null;
		return (root, query, cb) -> cb.like(cb.upper(root.get("descripcion")),
				"%" + observacion.trim().toUpperCase() + "%");
	}

	/**
	 * Nombre completo del cliente
	 */
	public static Specification<LecturaEntity> clienteNombreLike(String clienteNombreLike) {
		if (clienteNombreLike == null || clienteNombreLike.isBlank())
			return null;

		return (root, cq, cb) -> {
			cq.distinct(true);
			var cli = root.join("contador").join("cliente");

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

			String pattern = "%" + clienteNombreLike.toLowerCase().trim().replaceAll("\\s+", " ") + "%";
			return cb.like(fullNameLower, pattern);
		};
	}

	/*
	 * Comentario (campo descripcion) de la lectura
	 */
	public static Specification<LecturaEntity> comentarioLike(String descripcion) {
		if (descripcion == null || descripcion.isBlank())
			return null;
		return (root, query, cb) -> cb.like(cb.upper(root.get("descripcion")), "%" + descripcion.trim().toUpperCase() + "%");
	}
}
