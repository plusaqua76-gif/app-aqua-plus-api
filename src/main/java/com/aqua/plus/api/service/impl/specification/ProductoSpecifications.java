package com.aqua.plus.api.service.impl.specification;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.ProductoEntity;

public class ProductoSpecifications {

	private ProductoSpecifications() {
	}

	public static Specification<ProductoEntity> perteneceAEmpresa(Integer idEmpresa) {
		if (idEmpresa == null)
			return null;
		return (root, q, cb) -> cb.equal(root.join("empresa").get("id"), idEmpresa);
	}

	public static Specification<ProductoEntity> codigoLike(String codigo) {
		if (codigo == null || codigo.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.get("codigo")), "%" + codigo.trim().toUpperCase() + "%");
	}

	public static Specification<ProductoEntity> nombreLike(String nombre) {
		if (nombre == null || nombre.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.get("nombre")), "%" + nombre.trim().toUpperCase() + "%");
	}

	public static Specification<ProductoEntity> descripcionLike(String descripcion) {
		if (descripcion == null || descripcion.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.get("descripcion")),
				"%" + descripcion.trim().toUpperCase() + "%");
	}

	public static Specification<ProductoEntity> categoriaNombreLike(String categoriaNombre) {
		if (categoriaNombre == null || categoriaNombre.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.join("categoria").get("nombre")),
				"%" + categoriaNombre.trim().toUpperCase() + "%");
	}
}
