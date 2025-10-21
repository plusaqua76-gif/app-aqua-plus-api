package com.aqua.plus.api.service.impl.specification;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.InventarioEntity;

public class InventarioSpecifications {

	private InventarioSpecifications() {
	}

	public static Specification<InventarioEntity> perteneceAEmpresa(Integer idEmpresa) {
		if (idEmpresa == null)
			return null;
		return (root, query, cb) -> cb.equal(root.join("producto").join("empresa").get("id"), idEmpresa);
	}

	public static Specification<InventarioEntity> cantidadEquals(Integer cantidad) {
		if (cantidad == null)
			return null;
		return (root, q, cb) -> cb.equal(root.get("cantidad"), cantidad);
	}

	public static Specification<InventarioEntity> precioUnitarioEquals(Double precioUnitario) {
		if (precioUnitario == null)
			return null;
		return (root, q, cb) -> cb.equal(root.get("precioUnitario"), precioUnitario);
	}

	public static Specification<InventarioEntity> precioVentaEquals(Double precioVenta) {
		if (precioVenta == null)
			return null;
		return (root, q, cb) -> cb.equal(root.get("precioVenta"), precioVenta);
	}

	public static Specification<InventarioEntity> porcentajeEquals(Integer porcentaje) {
		if (porcentaje == null)
			return null;
		return (root, q, cb) -> cb.equal(root.get("porcentaje"), porcentaje);
	}

	public static Specification<InventarioEntity> codigoProductoLike(String codigo) {
		if (codigo == null || codigo.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.join("producto").get("codigo")),
				"%" + codigo.trim().toUpperCase() + "%");
	}

	public static Specification<InventarioEntity> nombreProductoLike(String nombre) {
		if (nombre == null || nombre.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.join("producto").get("nombre")),
				"%" + nombre.trim().toUpperCase() + "%");
	}

	/** NUEVO: descripción del PRODUCTO (distinta a inventario.descripcion) */
	public static Specification<InventarioEntity> descripcionProductoLike(String descripcionProducto) {
		if (descripcionProducto == null || descripcionProducto.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.join("producto").get("descripcion")),
				"%" + descripcionProducto.trim().toUpperCase() + "%");
	}

	/** Ya tienes esta, y apunta a inventario.descripcion */
	public static Specification<InventarioEntity> descripcionLike(String descripcion) {
		if (descripcion == null || descripcion.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.get("descripcion")),
				"%" + descripcion.trim().toUpperCase() + "%");
	}

	/** NUEVO: categoría del PRODUCTO */
	public static Specification<InventarioEntity> categoriaNombreLike(String categoriaNombre) {
		if (categoriaNombre == null || categoriaNombre.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.join("producto").join("categoria").get("nombre")),
				"%" + categoriaNombre.trim().toUpperCase() + "%");
	}
}
