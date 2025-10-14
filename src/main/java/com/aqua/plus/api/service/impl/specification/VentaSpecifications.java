package com.aqua.plus.api.service.impl.specification;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.VentaEntity;

public class VentaSpecifications {

	private VentaSpecifications() {
	}

	public static Specification<VentaEntity> empresaIdEquals(Integer empresaId) {
		return (root, q, cb) -> cb.equal(root.join("empresa").get("id"), empresaId);
	}

	public static Specification<VentaEntity> clienteNombreLike(String v) {
		if (v == null || v.isBlank())
			return null;
		return (r, q, cb) -> cb.like(cb.upper(r.join("cliente").get("nombre")), "%" + v.trim().toUpperCase() + "%");
	}

	public static Specification<VentaEntity> codigoLike(String v) {
		if (v == null || v.isBlank())
			return null;
		return (r, q, cb) -> cb.like(cb.upper(r.get("codigo")), "%" + v.trim().toUpperCase() + "%");
	}

	public static Specification<VentaEntity> cantidadEquals(Integer v) {
		if (v == null)
			return null;
		return (r, q, cb) -> cb.equal(r.get("cantidad"), v);
	}

	public static Specification<VentaEntity> nombreLike(String v) {
		if (v == null || v.isBlank())
			return null;
		return (r, q, cb) -> cb.like(cb.upper(r.get("nombre")), "%" + v.trim().toUpperCase() + "%");
	}

	public static Specification<VentaEntity> identificacionLike(String v) {
		if (v == null || v.isBlank())
			return null;
		return (r, q, cb) -> cb.like(cb.upper(r.get("identificacion")), "%" + v.trim().toUpperCase() + "%");
	}

	public static Specification<VentaEntity> precioVentaEquals(Double v) {
		if (v == null)
			return null;
		return (r, q, cb) -> cb.equal(r.get("precioVenta"), v);
	}

	public static Specification<VentaEntity> valorTotalEquals(Double v) {
		if (v == null)
			return null;
		return (r, q, cb) -> cb.equal(r.get("valorTotal"), v);
	}

	public static Specification<VentaEntity> descripcionLike(String v) {
		if (v == null || v.isBlank())
			return null;
		return (r, q, cb) -> cb.like(cb.upper(r.get("descripcion")), "%" + v.trim().toUpperCase() + "%");
	}

}
