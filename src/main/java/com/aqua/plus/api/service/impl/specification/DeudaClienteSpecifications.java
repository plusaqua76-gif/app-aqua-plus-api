package com.aqua.plus.api.service.impl.specification;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.DeudaClienteEntity;
import com.aqua.plus.commons.utils.Constantes;

import jakarta.persistence.criteria.JoinType;

public class DeudaClienteSpecifications {

	private DeudaClienteSpecifications() {
	}

	public static Specification<DeudaClienteEntity> perteneceAEmpresa(Integer idEmpresa) {
		if (idEmpresa == null)
			return null;
		return (root, cq, cb) -> {
			var ecc = root.join("empresaClienteContador");
			var emp = ecc.join("empresa");
			return cb.equal(emp.get("id"), idEmpresa);
		};
	}

	/** DATE(fechaCreacion) = :fecha (yyyy-MM-dd). */
	public static Specification<DeudaClienteEntity> fechaCreacionIgual(LocalDate fecha) {
	    if (fecha == null)
	        return null;
	    return (root, cq, cb) -> cb.equal(
	            cb.function("DATE", Date.class, root.get("fechaCreacion")),
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

	public static Specification<DeudaClienteEntity> clienteNombreLike(String clienteNombreLike) {
		if (clienteNombreLike == null || clienteNombreLike.isBlank())
			return null;

		return (root, cq, cb) -> {
			var ecc = root.join("empresaClienteContador");
			var cli = ecc.join("cliente");

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

			var fullNameNorm = fullNameLower;

			String pattern = "%" + clienteNombreLike.toLowerCase().trim().replaceAll("\\s+", " ") + "%";

			return cb.like(fullNameNorm, pattern);
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

	/** plazoPago igual a valor. */
	public static Specification<DeudaClienteEntity> plazoPagoIgual(Integer plazoPago) {
		return (root, cq, cb) -> plazoPago != null ? cb.equal(root.get("plazoPago"), plazoPago) : cb.conjunction();
	}

	/**
	 * Combina specs ignorando nulos (Spring Data 3.5+: usar Specification.allOf).
	 */
	@SafeVarargs
	public static Specification<DeudaClienteEntity> allOfNonNull(Specification<DeudaClienteEntity>... specs) {
		List<Specification<DeudaClienteEntity>> list = java.util.Arrays.stream(specs).filter(Objects::nonNull).toList();
		return Specification.allOf(list);
	}

	public static Specification<DeudaClienteEntity> activoTrue() {
		return (root, cq, cb) -> cb.isTrue(root.get("activo"));
	}

	public static Specification<DeudaClienteEntity> saldoPendientePorEstado() {
	    return (root, cq, cb) -> {
	        var estado = root.join("estado", JoinType.LEFT);
	        return cb.or(
	                cb.isNull(estado.get("id")),
	                cb.notEqual(estado.get("codigo"), Constantes.EST_DEU_PAGADA)
	        );
	    };
	}

}
