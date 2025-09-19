package com.aqua.plus.api.service.impl.specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.AbonoEntity;

import jakarta.persistence.criteria.*;

public class AbonoSpecifications {

	private AbonoSpecifications() {
	}

	public static Specification<AbonoEntity> porIdEmpresa(Integer idEmpresa) {
		return (root, cq, cb) -> {
			Join<Object, Object> dc = root.join("deudaCliente");
			Join<Object, Object> ecc = dc.join("empresaClienteContador");
			Join<Object, Object> emp = ecc.join("empresa");
			return cb.equal(emp.get("id"), idEmpresa);
		};
	}

	public static Specification<AbonoEntity> activoIgual(Boolean activo) {
		if (activo == null)
			return null;
		return (root, cq, cb) -> cb.equal(root.get("activo"), activo);
	}

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

	public static Specification<AbonoEntity> clienteLike(String texto) {
		if (texto == null || texto.isBlank())
			return null;

		return (root, cq, cb) -> {
			var dc = root.join("deudaCliente");
			var ecc = dc.join("empresaClienteContador");
			var cli = ecc.join("cliente");

			var pNombre = cb.function("nullif", String.class, cli.get("nombre"), cb.literal(""));
			var sNombre = cb.function("nullif", String.class, cli.get("segundoNombre"), cb.literal(""));
			var pApellido = cb.function("nullif", String.class, cli.get("apellido"), cb.literal(""));
			var sApellido = cb.function("nullif", String.class, cli.get("segundoApellido"), cb.literal(""));

			var fullName = cb.function("concat_ws", String.class, cb.literal(" "), pNombre, sNombre, pApellido,
					sApellido);

			var fullNameTrim = cb.function("btrim", String.class, fullName);

			var fullNameSpNorm = cb.function("regexp_replace", String.class, fullNameTrim, cb.literal("\\s+"),
					cb.literal(" "), cb.literal("g"));

			var fullNameLower = cb.lower(fullNameSpNorm);

			var fullNameNorm = fullNameLower;

			String pattern = "%" + texto.toLowerCase().trim().replaceAll("\\s+", " ") + "%";

			return cb.like(fullNameNorm, pattern);
		};
	}

	public static Specification<AbonoEntity> codigoFactura(String codigo) {
		if (codigo == null || codigo.isBlank())
			return null;
		String like = "%" + codigo.trim().toLowerCase() + "%";
		return (root, cq, cb) -> {
			Join<Object, Object> dc = root.join("deudaCliente");
			Join<Object, Object> f = dc.join("factura", JoinType.LEFT);
			return cb.like(cb.lower(cb.coalesce(f.get("codigo"), "")), like);
		};
	}

	/** Combina specs de forma segura, ignorando nulos. */
	public static Specification<AbonoEntity> build(Integer idEmpresa, Boolean activo, Date fechaDesde, Date fechaHasta,
			Double valorMin, Double valorMax, String clienteLike, String codigoFacturaLike) {

		List<Specification<AbonoEntity>> specs = new ArrayList<>();
		specs.add(porIdEmpresa(idEmpresa));
		specs.add(activoIgual(activo));
		specs.add(fechaEntre(fechaDesde, fechaHasta));
		specs.add(valorEntre(valorMin, valorMax));
		specs.add(clienteLike(clienteLike));
		specs.add(codigoFactura(codigoFacturaLike));

		Specification<AbonoEntity> result = null;
		for (Specification<AbonoEntity> s : specs) {
			if (s == null)
				continue;
			result = (result == null) ? s : result.and(s);
		}
		return result;
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

}
