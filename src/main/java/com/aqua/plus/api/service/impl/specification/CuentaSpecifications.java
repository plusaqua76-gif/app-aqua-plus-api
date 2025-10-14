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

	public static Specification<CuentaEntity> tipoNaturalezaLike(String naturaleza) {
		if (naturaleza == null || naturaleza.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.join("tipoCuenta").get("naturaleza")),
				"%" + naturaleza.trim().toUpperCase() + "%");
	}
}
