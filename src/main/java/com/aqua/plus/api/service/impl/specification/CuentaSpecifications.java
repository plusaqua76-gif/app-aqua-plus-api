package com.aqua.plus.api.service.impl.specification;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.CuentaEntity;

public class CuentaSpecifications {

	private CuentaSpecifications() {
	}

	public static Specification<CuentaEntity> empresaIdEquals(Integer empresaId) {
		if (empresaId == null)
			return null;
		return (root, q, cb) -> cb.equal(root.join("empresa").get("id"), empresaId);
	}

	public static Specification<CuentaEntity> codigoLike(String codigo) {
		if (codigo == null || codigo.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.get("codigo")), "%" + codigo.trim().toUpperCase() + "%");
	}

	public static Specification<CuentaEntity> nombreLike(String nombre) {
		if (nombre == null || nombre.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.get("nombre")), "%" + nombre.trim().toUpperCase() + "%");
	}

	public static Specification<CuentaEntity> valorEquals(Double valor) {
		if (valor == null)
			return null;
		return (root, q, cb) -> cb.equal(root.get("valor"), valor);
	}

	public static Specification<CuentaEntity> tipoNombreLike(String tipoNombre) {
		if (tipoNombre == null || tipoNombre.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.join("tipoCuenta").get("nombre")),
				"%" + tipoNombre.trim().toUpperCase() + "%");
	}

	public static Specification<CuentaEntity> tipoCategoriaCuentaLike(String tipoCategoria) {
		if (tipoCategoria == null || tipoCategoria.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.join("categoriaCuenta").get("nombre")),
				"%" + tipoCategoria.trim().toUpperCase() + "%");
	}

	public static Specification<CuentaEntity> tipoNaturalezaLike(String naturaleza) {
		if (naturaleza == null || naturaleza.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.join("tipoCuenta").get("naturaleza")),
				"%" + naturaleza.trim().toUpperCase() + "%");
	}

	public static Specification<CuentaEntity> fechaBetween(java.time.LocalDate desde, java.time.LocalDate hasta) {
		if (desde == null && hasta == null)
			return null;
		return (root, query, cb) -> {
			if (desde != null && hasta != null) {
				var iDesde = desde.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
				var iHasta = hasta.atTime(java.time.LocalTime.MAX).atZone(java.time.ZoneId.systemDefault()).toInstant();
				return cb.between(root.get("fechaCreacion"), java.util.Date.from(iDesde), java.util.Date.from(iHasta));
			} else if (desde != null) {
				var iDesde = desde.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
				return cb.greaterThanOrEqualTo(root.get("fechaCreacion"), java.util.Date.from(iDesde));
			} else {
				var iHasta = hasta.atTime(java.time.LocalTime.MAX).atZone(java.time.ZoneId.systemDefault()).toInstant();
				return cb.lessThanOrEqualTo(root.get("fechaCreacion"), java.util.Date.from(iHasta));
			}
		};
	}
}
