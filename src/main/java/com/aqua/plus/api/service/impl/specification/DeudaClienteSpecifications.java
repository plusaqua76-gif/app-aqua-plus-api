package com.aqua.plus.api.service.impl.specification;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.DeudaClienteEntity;

import jakarta.persistence.criteria.JoinType;

public class DeudaClienteSpecifications {

	private DeudaClienteSpecifications() {
	}
	
	public static Specification<DeudaClienteEntity> perteneceAEmpresa(Integer idEmpresa) {
	    if (idEmpresa == null) return null;
	    return (root, cq, cb) -> {
	        var ecc = root.join("empresaClienteContador");
	        var emp = ecc.join("empresa");
	        return cb.equal(emp.get("id"), idEmpresa);
	    };
	}

	/** DATE(fechaDeuda) = :fecha (yyyy-MM-dd). */
	public static Specification<DeudaClienteEntity> fechaDeudaIgual(LocalDate fecha) {
		if (fecha == null)
			return null;
		return (root, cq, cb) -> cb.equal(cb.function("DATE", Date.class, root.get("fechaDeuda")),
				java.sql.Date.valueOf(fecha));
	}

	/** valor = :valor (exacto). */
	public static Specification<DeudaClienteEntity> valorIgual(Double valor) {
		if (valor == null)
			return null;
		return (root, cq, cb) -> cb.equal(root.get("valor"), valor);
	}

	/** descripcion ILIKE %texto%. */
	public static Specification<DeudaClienteEntity> descripcionLike(String descripcionLike) {
		if (descripcionLike == null || descripcionLike.isBlank())
			return null;
		String like = "%" + descripcionLike.trim().toLowerCase() + "%";
		return (root, cq, cb) -> cb.like(cb.lower(cb.coalesce(root.get("descripcion"), "")), like);
	}

	/** factura.codigo ILIKE %codigo%. */
	public static Specification<DeudaClienteEntity> facturaCodigoLike(String codigoLike) {
		if (codigoLike == null || codigoLike.isBlank())
			return null;
		String like = "%" + codigoLike.trim().toLowerCase() + "%";
		return (root, cq, cb) -> {
			var f = root.join("factura", JoinType.LEFT);
			return cb.like(cb.lower(cb.coalesce(f.get("codigo"), "")), like);
		};
	}

	/**
	 * clienteNombre ILIKE %texto% sobre los campos del cliente: nombre,
	 * segundoNombre, apellido, segundoApellido.
	 */
	public static Specification<DeudaClienteEntity> clienteNombreLike(String clienteNombreLike) {
		if (clienteNombreLike == null || clienteNombreLike.isBlank())
			return null;
		String like = "%" + clienteNombreLike.trim().toLowerCase() + "%";
		return (root, cq, cb) -> {
			var ecc = root.join("empresaClienteContador");
			var cli = ecc.join("cliente");
			var n1 = cb.lower(cb.coalesce(cli.get("nombre"), ""));
			var n2 = cb.lower(cb.coalesce(cli.get("segundoNombre"), ""));
			var a1 = cb.lower(cb.coalesce(cli.get("apellido"), ""));
			var a2 = cb.lower(cb.coalesce(cli.get("segundoApellido"), ""));
			return cb.or(cb.like(n1, like), cb.like(n2, like), cb.like(a1, like), cb.like(a2, like));
		};
	}

	/** tipoDeuda.nombre ILIKE %nombre%. */
	public static Specification<DeudaClienteEntity> tipoDeudaNombreLike(String tipoDeudaNombre) {
		if (tipoDeudaNombre == null || tipoDeudaNombre.isBlank())
			return null;
		String like = "%" + tipoDeudaNombre.trim().toLowerCase() + "%";
		return (root, cq, cb) -> {
			var td = root.join("tipoDeuda", JoinType.LEFT);
			return cb.like(cb.lower(cb.coalesce(td.get("nombre"), "")), like);
		};
	}

	/** plazoPago.nombre ILIKE %nombre%. */
	public static Specification<DeudaClienteEntity> plazoPagoNombreLike(String plazoPagoNombre) {
		if (plazoPagoNombre == null || plazoPagoNombre.isBlank())
			return null;
		String like = "%" + plazoPagoNombre.trim().toLowerCase() + "%";
		return (root, cq, cb) -> {
			var pp = root.join("plazoPago", JoinType.LEFT);
			return cb.like(cb.lower(cb.coalesce(pp.get("nombre"), "")), like);
		};
	}

	/**
	 * Combina specs ignorando nulos (Spring Data 3.5+: usar Specification.allOf).
	 */
	@SafeVarargs
	public static Specification<DeudaClienteEntity> allOfNonNull(Specification<DeudaClienteEntity>... specs) {
		List<Specification<DeudaClienteEntity>> list = java.util.Arrays.stream(specs).filter(Objects::nonNull).toList();
		return Specification.allOf(list);
	}
}
