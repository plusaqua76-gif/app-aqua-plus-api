package com.aqua.plus.api.service.impl.specification;

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

	public static Specification<AbonoEntity> cliente(String texto) {
		if (texto == null || texto.isBlank())
			return null;
		String like = "%" + texto.trim().toLowerCase() + "%";
		return (root, cq, cb) -> {
			Join<Object, Object> dc = root.join("deudaCliente");
			Join<Object, Object> ecc = dc.join("empresaClienteContador");
			Join<Object, Object> cli = ecc.join("cliente");

			Expression<String> n1 = cb.lower(cb.coalesce(cli.get("nombre"), ""));
			Expression<String> n2 = cb.lower(cb.coalesce(cli.get("segundoNombre"), ""));
			Expression<String> a1 = cb.lower(cb.coalesce(cli.get("apellido"), ""));
			Expression<String> a2 = cb.lower(cb.coalesce(cli.get("segundoApellido"), ""));

			return cb.or(cb.like(n1, like), cb.like(n2, like), cb.like(a1, like), cb.like(a2, like), cb.like(cb.lower(
					cb.concat(cb.concat(cb.concat(n1, " "), n2), cb.concat(" ", cb.concat(a1, cb.concat(" ", a2))))),
					like));
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
		specs.add(cliente(clienteLike));
		specs.add(codigoFactura(codigoFacturaLike));

		Specification<AbonoEntity> result = null;
		for (Specification<AbonoEntity> s : specs) {
			if (s == null)
				continue;
			result = (result == null) ? s : result.and(s);
		}
		return result;
	}
}
