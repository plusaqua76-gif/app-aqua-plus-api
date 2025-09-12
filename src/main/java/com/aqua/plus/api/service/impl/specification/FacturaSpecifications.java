package com.aqua.plus.api.service.impl.specification;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.FacturaEntity;

import jakarta.persistence.criteria.JoinType;

public final class FacturaSpecifications {

	private FacturaSpecifications() {
	}

	// Empresa (obligatorio en tu caso de uso)
	public static Specification<FacturaEntity> perteneceAEmpresa(Integer idEmpresa) {
		if (idEmpresa == null)
			return null;
		return (root, query, cb) -> {
			var ecc = root.join("empresaClienteContador", JoinType.INNER);
			var emp = ecc.join("empresa", JoinType.INNER);
			return cb.equal(emp.get("id"), idEmpresa);
		};
	}

	public static Specification<FacturaEntity> codigoLike(String codigo) {
		if (codigo == null || codigo.isBlank())
			return null;
		String like = "%" + codigo.trim().toUpperCase() + "%";
		return (root, query, cb) -> cb.like(cb.upper(root.get("codigo")), like);
	}

	// Nombre completo del cliente (persona de ECC)
	public static Specification<FacturaEntity> clienteNombreCompletoLike(String nombreCompleto) {
		if (nombreCompleto == null || nombreCompleto.isBlank())
			return null;
		String like = "%" + nombreCompleto.trim().toUpperCase() + "%";
		return (root, query, cb) -> {
			var ecc = root.join("empresaClienteContador", JoinType.INNER);
			var persona = ecc.join("cliente", JoinType.INNER);

			// concat COALESCE para evitar NPE en BD
			var n1 = cb.coalesce(cb.upper(persona.get("nombre")), "");
			var n2 = cb.coalesce(cb.upper(persona.get("segundoNombre")), "");
			var a1 = cb.coalesce(cb.upper(persona.get("apellido")), "");
			var a2 = cb.coalesce(cb.upper(persona.get("segundoApellido")), "");

			// construir "NOMBRE SEGUNDONOMBRE APELLIDO SEGUNDOAPELLIDO"
			var space = cb.literal(" ");
			var full = cb.function("concat", String.class, n1, space, n2, space, a1, space, a2);

			return cb.like(full, like);
		};
	}

	public static Specification<FacturaEntity> consumoEquals(Integer consumo) {
		if (consumo == null)
			return null;
		return (root, query, cb) -> cb.equal(root.get("consumo"), consumo);
	}

	public static Specification<FacturaEntity> fechaEmisionBetween(LocalDate desde, LocalDate hasta) {
		if (desde == null && hasta == null)
			return null;
		return (root, query, cb) -> {
			if (desde != null && hasta != null) {
				Date d = Date.from(desde.atStartOfDay(ZoneId.systemDefault()).toInstant());
				Date h = Date.from(hasta.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());
				return cb.between(root.get("fechaEmision"), d, h);
			} else if (desde != null) {
				Date d = Date.from(desde.atStartOfDay(ZoneId.systemDefault()).toInstant());
				return cb.greaterThanOrEqualTo(root.get("fechaEmision"), d);
			} else {
				Date h = Date.from(hasta.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());
				return cb.lessThanOrEqualTo(root.get("fechaEmision"), h);
			}
		};
	}

	public static Specification<FacturaEntity> fechaFinBetween(LocalDate desde, LocalDate hasta) {
		if (desde == null && hasta == null)
			return null;
		return (root, query, cb) -> {
			if (desde != null && hasta != null) {
				Date d = Date.from(desde.atStartOfDay(ZoneId.systemDefault()).toInstant());
				Date h = Date.from(hasta.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());
				return cb.between(root.get("fechaFin"), d, h);
			} else if (desde != null) {
				Date d = Date.from(desde.atStartOfDay(ZoneId.systemDefault()).toInstant());
				return cb.greaterThanOrEqualTo(root.get("fechaFin"), d);
			} else {
				Date h = Date.from(hasta.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());
				return cb.lessThanOrEqualTo(root.get("fechaFin"), h);
			}
		};
	}

	public static Specification<FacturaEntity> estadoNombreLike(String estadoNombre) {
		if (estadoNombre == null || estadoNombre.isBlank())
			return null;
		String like = "%" + estadoNombre.trim().toUpperCase() + "%";
		return (root, query, cb) -> {
			var est = root.join("estado", JoinType.LEFT);
			return cb.like(cb.upper(est.get("nombre")), like);
		};
	}

	// consumoAnormal está en Lectura, no en Factura
	public static Specification<FacturaEntity> consumoAnormalEquals(Boolean consumoAnormal) {
		if (consumoAnormal == null)
			return null;
		return (root, query, cb) -> {
			var lec = root.join("lectura", JoinType.LEFT);
			return (consumoAnormal) ? cb.isTrue(lec.get("consumoAnormal")) : cb.isFalse(lec.get("consumoAnormal"));
		};
	}

	public static Specification<FacturaEntity> precioBetween(Double min, Double max) {
		if (min == null && max == null)
			return null;
		return (root, query, cb) -> {
			if (min != null && max != null) {
				double lo = Math.min(min, max);
				double hi = Math.max(min, max);
				return cb.between(root.get("precio"), lo, hi);
			} else if (min != null) {
				return cb.greaterThanOrEqualTo(root.get("precio"), min);
			} else {
				return cb.lessThanOrEqualTo(root.get("precio"), max);
			}
		};
	}

	public static Specification<FacturaEntity> activoTrue() {
		return (root, query, cb) -> cb.isTrue(root.get("activo"));
	}

	/**
	 * Variante opcional: trata NULL como activo (equivalente a IS DISTINCT FROM
	 * FALSE).
	 */
	public static Specification<FacturaEntity> activoNotFalse() {
		return (root, query, cb) -> cb.or(cb.isTrue(root.get("activo")), cb.isNull(root.get("activo")));
	}
}
