package com.aqua.plus.api.service.impl.specification;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.dtos.ContadorFiltroDTO;
import com.aqua.plus.commons.entities.EmpresaClienteContadorEntity;

import jakarta.persistence.criteria.JoinType;

public final class EccSpecification {

	private EccSpecification() {
	}

	public static Specification<EmpresaClienteContadorEntity> build(Integer personaId, ContadorFiltroDTO f) {
		return soloActivos().and(porPersona(personaId)).and(porNumeroContador(f.getNumeroContador()))
				.and(porTipoUso(f.getTipoUsoId())).and(porAforo(f.getAforoNombre()))
				.and(porEmpleado(f.getEmpleadoEmpresaId())).and(porNuid(f.getNuid()));
	}

	private static Specification<EmpresaClienteContadorEntity> soloActivos() {
		return (root, q, cb) -> cb.isTrue(root.get("activo"));
	}

	private static Specification<EmpresaClienteContadorEntity> porPersona(Integer personaId) {
		return (root, q, cb) -> personaId == null ? cb.conjunction()
				: cb.equal(root.get("cliente").get("id"), personaId);
	}

	private static Specification<EmpresaClienteContadorEntity> porNumeroContador(String numero) {
		return (root, q, cb) -> {
			if (numero == null || numero.isBlank())
				return cb.conjunction();
			return cb.like(cb.lower(root.get("contador").get("serial")), "%" + numero.toLowerCase() + "%");
		};
	}

	private static Specification<EmpresaClienteContadorEntity> porTipoUso(Integer tipoUsoId) {
		return (root, q, cb) -> tipoUsoId == null ? cb.conjunction()
				: cb.equal(root.get("contador").get("tipoUso").get("id"), tipoUsoId);
	}

	private static Specification<EmpresaClienteContadorEntity> porAforo(String aforoNombre) {
		return (root, q, cb) -> {
			if (aforoNombre == null || aforoNombre.isBlank())
				return cb.conjunction();
			var joinContador = root.join("contador", JoinType.INNER);
			var joinAforoContador = joinContador.join("aforoContadores", JoinType.INNER);
			var joinAforo = joinAforoContador.join("aforo", JoinType.INNER);
			return cb.like(cb.lower(joinAforo.get("nombre")), "%" + aforoNombre.toLowerCase() + "%");
		};
	}

	private static Specification<EmpresaClienteContadorEntity> porEmpleado(Integer empleadoId) {
		return (root, q, cb) -> {
			if (empleadoId == null)
				return cb.conjunction();
			var joinRuta = root.join("rutaEmpleado", JoinType.LEFT);
			var joinEmpleado = joinRuta.join("empleadoEmpresa", JoinType.LEFT);
			return cb.equal(joinEmpleado.get("id"), empleadoId);
		};
	}

	private static Specification<EmpresaClienteContadorEntity> porNuid(Integer nuid) {
		return (root, q, cb) -> nuid == null ? cb.conjunction() : cb.equal(root.get("contador").get("nuid"), nuid);
	}
}
