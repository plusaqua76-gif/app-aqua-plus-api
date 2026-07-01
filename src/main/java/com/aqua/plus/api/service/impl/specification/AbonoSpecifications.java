package com.aqua.plus.api.service.impl.specification;

import java.time.LocalDate;
import java.util.Date;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.AbonoEntity;

import jakarta.persistence.criteria.*;

public class AbonoSpecifications {

	private AbonoSpecifications() {
	}

	@SuppressWarnings("unchecked")
	private static <X, Y> Join<X, Y> getOrJoin(From<X, ?> from, String attr, JoinType type) {
		return (Join<X, Y>) from.getJoins().stream().filter(j -> j.getAttribute().getName().equals(attr)).findFirst()
				.orElseGet(() -> from.join(attr, type));
	}

	private static Join<AbonoEntity, Object> joinDc(Root<AbonoEntity> root) {
		return getOrJoin(root, "deudaCliente", JoinType.LEFT);
	}

	private static <X> Join<X, Object> joinEcc(Join<X, Object> dc) {
		return getOrJoin(dc, "empresaClienteContador", JoinType.LEFT);
	}

	private static <X> Join<X, Object> joinEmpresa(Join<X, Object> ecc) {
		return getOrJoin(ecc, "empresa", JoinType.LEFT);
	}

	private static <X> Join<X, Object> joinCliente(Join<X, Object> ecc) {
		return getOrJoin(ecc, "cliente", JoinType.LEFT);
	}

	private static <X> Join<X, Object> joinFactura(Join<X, Object> dc) {
		return getOrJoin(dc, "factura", JoinType.LEFT);
	}

	// ── Specs ──

	public static Specification<AbonoEntity> porIdEmpresa(Integer idEmpresa) {
		if (idEmpresa == null)
			return null;
		return (root, cq, cb) -> {
			var dc = joinDc(root);
			var ecc = joinEcc(dc);
			var emp = joinEmpresa(ecc);
			return cb.equal(emp.get("id"), idEmpresa);
		};
	}

	public static Specification<AbonoEntity> activoIgual(Boolean activo) {
		if (activo == null)
			return null;
		return (root, cq, cb) -> cb.equal(root.get("activo"), activo);
	}

	public static Specification<AbonoEntity> fechaIgual(LocalDate fecha) {
		if (fecha == null)
			return null;
		var ini = java.sql.Timestamp.valueOf(fecha.atStartOfDay());
		var fin = java.sql.Timestamp.valueOf(fecha.plusDays(1).atStartOfDay());
		return (root, cq, cb) -> cb.and(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), ini),
				cb.lessThan(root.get("fechaCreacion"), fin));
	}

	public static Specification<AbonoEntity> valorIgual(Double valor) {
		if (valor == null)
			return null;
		return (root, cq, cb) -> cb.equal(root.get("valor"), valor);
	}

	public static Specification<AbonoEntity> clienteLike(String texto) {
		if (texto == null || texto.isBlank())
			return null;
		return (root, cq, cb) -> {
			var dc = joinDc(root);
			var ecc = joinEcc(dc);
			var cli = joinCliente(ecc);

			var fullName = cb.function("concat_ws", String.class, cb.literal(" "),
					cb.function("nullif", String.class, cli.get("nombre"), cb.literal("")),
					cb.function("nullif", String.class, cli.get("segundoNombre"), cb.literal("")),
					cb.function("nullif", String.class, cli.get("apellido"), cb.literal("")),
					cb.function("nullif", String.class, cli.get("segundoApellido"), cb.literal("")));

			var normalizado = cb
					.lower(cb.function("regexp_replace", String.class, cb.function("btrim", String.class, fullName),
							cb.literal("\\s+"), cb.literal(" "), cb.literal("g")));

			String pattern = "%" + texto.toLowerCase().trim().replaceAll("\\s+", " ") + "%";
			return cb.like(normalizado, pattern);
		};
	}

	public static Specification<AbonoEntity> codigoFactura(String codigo) {
		if (codigo == null || codigo.isBlank())
			return null;
		String like = "%" + codigo.trim().toLowerCase() + "%";
		return (root, cq, cb) -> {
			var dc = joinDc(root);
			var f = joinFactura(dc);
			return cb.like(cb.lower(cb.coalesce(f.get("codigo"), "")), like);
		};
	}

	// ── Legacy (se conservan) ──

	public static Specification<AbonoEntity> fechaEntre(Date desde, Date hasta) {
		if (desde == null && hasta == null)
			return null;
		return (root, cq, cb) -> {
			Path<Date> campo = root.get("fechaCreacion");
			if (desde != null && hasta != null)
				return cb.between(campo, desde, hasta);
			if (desde != null)
				return cb.greaterThanOrEqualTo(campo, desde);
			return cb.lessThanOrEqualTo(campo, hasta);
		};
	}

	public static Specification<AbonoEntity> valorEntre(Double min, Double max) {
		if (min == null && max == null)
			return null;
		return (root, cq, cb) -> {
			Path<Double> campo = root.get("valor");
			if (min != null && max != null)
				return cb.between(campo, min, max);
			if (min != null)
				return cb.greaterThanOrEqualTo(campo, min);
			return cb.lessThanOrEqualTo(campo, max);
		};
	}
}
