package com.aqua.plus.api.service.impl.specification;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
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

		return (root, cq, cb) -> {
			cq.distinct(true);
			var cli = root.join("empresaClienteContador").join("cliente");

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

			String pattern = "%" + nombreCompleto.toLowerCase().trim().replaceAll("\\s+", " ") + "%";
			return cb.like(fullNameLower, pattern);
		};
	}

	public static Specification<FacturaEntity> consumoEquals(Integer consumo) {
		if (consumo == null)
			return null;

		return (root, query, cb) -> {
			var lec = root.join("lectura", JoinType.INNER); // o LEFT si lo necesitas opcional
			return cb.equal(lec.get("lectura"), consumo);
		};
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

	public static Specification<FacturaEntity> activoNotFalse() {
		return (root, query, cb) -> cb.or(cb.isTrue(root.get("activo")), cb.isNull(root.get("activo")));
	}

	public static Specification<FacturaEntity> personaIdEquals(Integer idPersona) {
		if (idPersona == null)
			return null;
		return (root, query, cb) -> {
			query.distinct(true); // evita duplicados por joins
			var ecc = root.join("empresaClienteContador", JoinType.INNER);
			var cliente = ecc.join("cliente", JoinType.INNER);
			return cb.equal(cliente.get("id"), idPersona);
		};
	}

	public static Specification<FacturaEntity> personaIdIn(Collection<Integer> personaIds) {
		if (personaIds == null || personaIds.isEmpty())
			return null;
		return (root, query, cb) -> {
			query.distinct(true);
			var ecc = root.join("empresaClienteContador", JoinType.INNER);
			var cliente = ecc.join("cliente", JoinType.INNER);
			return cliente.get("id").in(personaIds);
		};
	}

	public static Specification<FacturaEntity> precioEquals(Double precio) {
		if (precio == null)
			return null;
		return (root, query, cb) -> cb.equal(root.get("precio"), precio);
	}

	public static Specification<FacturaEntity> tipoPagoLike(String tipoPago) {
		if (tipoPago == null || tipoPago.isBlank())
			return null;
		return (root, query, cb) -> cb.like(cb.upper(root.get("tipoPago").get("nombre")),
				"%" + tipoPago.trim().toUpperCase() + "%");
	}

	public static Specification<FacturaEntity> corregimientoNombreLike(String corregimientoNombre) {
		if (corregimientoNombre == null || corregimientoNombre.isBlank())
			return null;
		String like = "%" + corregimientoNombre.trim().toUpperCase() + "%";
		return (root, query, cb) -> {
			query.distinct(true);
			var ecc = root.join("empresaClienteContador", JoinType.INNER);
			var cliente = ecc.join("cliente", JoinType.INNER);
			var direccion = cliente.join("direccion", JoinType.LEFT);
			var corregimiento = direccion.join("corregimiento", JoinType.LEFT);
			return cb.like(cb.upper(corregimiento.get("nombre")), like);
		};
	}

	public static Specification<FacturaEntity> contadorNuid(Integer nuid) {
		return (root, q, cb) -> nuid != null
				? cb.equal(root.get("empresaClienteContador").get("contador").get("nuid"), nuid)
				: cb.conjunction();
	}

	public static Specification<FacturaEntity> periodoEquals(String periodo) {
		if (periodo == null || periodo.isBlank())
			return null;
		return (root, query, cb) -> cb.equal(root.get("periodo"), periodo.trim());
	}
}
