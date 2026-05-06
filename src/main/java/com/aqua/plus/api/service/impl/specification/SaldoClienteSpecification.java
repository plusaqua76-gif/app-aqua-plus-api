package com.aqua.plus.api.service.impl.specification;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.SaldoClienteEntity;

import jakarta.persistence.criteria.JoinType;

/**
 * @author nicope
 * @version 1.0
 *
 *          Especificaciones JPA reutilizables para filtrar Saldo
 *
 */

public final class SaldoClienteSpecification {

	private static final String ECC = "empresaClienteContador";
	private static final String CLI = "cliente";
	private static final String DIR = "direccion";
	private static final String TDOC = "tipoDocumento";
	private static final String DEP = "departamento";
	private static final String CIU = "ciudad";
	private static final String CORR = "corregimiento";
	private static final String CONT = "contador";

	/** SaldoCliente → ecc → empresa → id */
	public static Specification<SaldoClienteEntity> empresaId(Integer idEmpresa) {
		return (root, query, cb) -> {
			if (idEmpresa == null)
				return null;
			return cb.equal(root.join(ECC, JoinType.INNER).join("empresa", JoinType.INNER).get("id"), idEmpresa);
		};
	}

	/** Nombre completo (primer + segundo nombre + apellidos) LIKE */
	public static Specification<SaldoClienteEntity> clienteNombreLike(String nombre) {
		return (root, query, cb) -> {
			if (nombre == null || nombre.isBlank())
				return null;
			String pattern = "%" + nombre.trim().toLowerCase() + "%";
			var persona = root.join(ECC, JoinType.LEFT).join(CLI, JoinType.LEFT);

			var nombreCompleto = cb.concat(
					cb.concat(cb.concat(
							cb.concat(cb.concat(cb.concat(cb.coalesce(persona.<String>get("nombre"), ""), " "),
									cb.coalesce(persona.<String>get("segundoNombre"), "")), " "),
							cb.coalesce(persona.<String>get("apellido"), "")), " "),
					cb.coalesce(persona.<String>get("segundoApellido"), ""));

			return cb.or(cb.like(cb.lower(persona.get("nombre")), pattern),
					cb.like(cb.lower(persona.get("segundoNombre")), pattern),
					cb.like(cb.lower(persona.get("apellido")), pattern),
					cb.like(cb.lower(persona.get("segundoApellido")), pattern),
					cb.like(cb.lower(nombreCompleto), pattern));
		};
	}

	public static Specification<SaldoClienteEntity> clienteCedulaIgual(String cedula) {
		return (root, query, cb) -> {
			if (cedula == null || cedula.isBlank())
				return null;
			return cb.equal(root.join(ECC, JoinType.LEFT).join(CLI, JoinType.LEFT).get("numeroCedula"), cedula.trim());
		};
	}

	public static Specification<SaldoClienteEntity> clienteCodigoIgual(String codigo) {
		return (root, query, cb) -> {
			if (codigo == null || codigo.isBlank())
				return null;
			return cb.equal(root.join(ECC, JoinType.LEFT).join(CLI, JoinType.LEFT).get("codigo"), codigo.trim());
		};
	}

	public static Specification<SaldoClienteEntity> clienteEstado(Boolean estado) {
		return (root, query, cb) -> {
			if (estado == null)
				return null;
			return cb.equal(root.join(ECC, JoinType.LEFT).join(CLI, JoinType.LEFT).get("activo"), estado);
		};
	}

	public static Specification<SaldoClienteEntity> clienteTipoDocumentoNombreLike(String tipoDoc) {
		return (root, query, cb) -> {
			if (tipoDoc == null || tipoDoc.isBlank())
				return null;
			String pattern = "%" + tipoDoc.trim().toLowerCase() + "%";
			return cb.like(cb.lower(
					root.join(ECC, JoinType.LEFT).join(CLI, JoinType.LEFT).join(TDOC, JoinType.LEFT).get("nombre")),
					pattern);
		};
	}

	public static Specification<SaldoClienteEntity> clienteTelefonoLike(String telefono) {
		// Requiere join hacia TelefonoGeneralEntity si está mapeado,
		// o se puede omitir aquí y filtrar en memoria si no hay join directo.
		return (root, query, cb) -> null; // ajusta según tu modelo
	}

	public static Specification<SaldoClienteEntity> clienteCorreoLike(String correo) {
		return (root, query, cb) -> null; // ídem
	}

	// ── Dirección ─────────────────────────────────────────────────────────────

	public static Specification<SaldoClienteEntity> direccionDepartamentoNombreLike(String dep) {
		return (root, query, cb) -> {
			if (dep == null || dep.isBlank())
				return null;
			String pattern = "%" + dep.trim().toLowerCase() + "%";
			return cb.like(cb.lower(root.join(ECC, JoinType.LEFT).join(CLI, JoinType.LEFT).join(DIR, JoinType.LEFT)
					.join(DEP, JoinType.LEFT).get("nombre")), pattern);
		};
	}

	public static Specification<SaldoClienteEntity> direccionCiudadNombreLike(String ciu) {
		return (root, query, cb) -> {
			if (ciu == null || ciu.isBlank())
				return null;
			String pattern = "%" + ciu.trim().toLowerCase() + "%";
			return cb.like(cb.lower(root.join(ECC, JoinType.LEFT).join(CLI, JoinType.LEFT).join(DIR, JoinType.LEFT)
					.join(CIU, JoinType.LEFT).get("nombre")), pattern);
		};
	}

	public static Specification<SaldoClienteEntity> direccionCorregimientoNombreLike(String corr) {
		return (root, query, cb) -> {
			if (corr == null || corr.isBlank())
				return null;
			String pattern = "%" + corr.trim().toLowerCase() + "%";
			return cb.like(cb.lower(root.join(ECC, JoinType.LEFT).join(CLI, JoinType.LEFT).join(DIR, JoinType.LEFT)
					.join(CORR, JoinType.LEFT).get("nombre")), pattern);
		};
	}

	public static Specification<SaldoClienteEntity> direccionDescripcionLike(String desc) {
		return (root, query, cb) -> {
			if (desc == null || desc.isBlank())
				return null;
			String pattern = "%" + desc.trim().toLowerCase() + "%";
			return cb.like(cb.lower(
					root.join(ECC, JoinType.LEFT).join(CLI, JoinType.LEFT).join(DIR, JoinType.LEFT).get("descripcion")),
					pattern);
		};
	}

	// ── Contador ──────────────────────────────────────────────────────────────

	public static Specification<SaldoClienteEntity> contadorNuid(Integer nuid) {
		return (root, query, cb) -> {
			if (nuid == null)
				return null;
			return cb.equal(root.join(ECC, JoinType.LEFT).join(CONT, JoinType.LEFT).get("nuid"), nuid);
		};
	}

	// ── Atributos propios de SaldoCliente ─────────────────────────────────────

	public static Specification<SaldoClienteEntity> saldoTotalMin(Integer min) {
		return (root, query, cb) -> min == null ? null : cb.greaterThanOrEqualTo(root.get("saldoTotal"), min);
	}

	public static Specification<SaldoClienteEntity> saldoTotalMax(Integer max) {
		return (root, query, cb) -> max == null ? null : cb.lessThanOrEqualTo(root.get("saldoTotal"), max);
	}

	public static Specification<SaldoClienteEntity> saldoDisponibleMin(Integer min) {
		return (root, query, cb) -> min == null ? null : cb.greaterThanOrEqualTo(root.get("saldoDisponible"), min);
	}

	public static Specification<SaldoClienteEntity> saldoDisponibleMax(Integer max) {
		return (root, query, cb) -> max == null ? null : cb.lessThanOrEqualTo(root.get("saldoDisponible"), max);
	}

	public static Specification<SaldoClienteEntity> cuotasIgual(Integer cuotas) {
		return (root, query, cb) -> cuotas == null ? null : cb.equal(root.get("cuotas"), cuotas);
	}
}
