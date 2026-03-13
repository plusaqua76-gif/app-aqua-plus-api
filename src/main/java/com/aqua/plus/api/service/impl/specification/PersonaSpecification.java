package com.aqua.plus.api.service.impl.specification;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.CorreoGeneralEntity;
import com.aqua.plus.commons.entities.EmpresaClienteContadorEntity;
import com.aqua.plus.commons.entities.TelefonoGeneralEntity;

import jakarta.persistence.criteria.JoinType;

/**
 * @author nicope
 * @version 1.0
 *
 *          Especificaciones JPA reutilizables para filtrar Personas en
 *          consultas; combinables dinámicamente.
 */

public final class PersonaSpecification {

	private PersonaSpecification() {
	}

	/** ecc.empresa.id = :idEmpresa */
	public static Specification<EmpresaClienteContadorEntity> empresaId(Integer idEmpresa) {
		if (idEmpresa == null)
			return null;
		return (root, q, cb) -> cb.equal(root.join("empresa").get("id"), idEmpresa);
	}

	/** cliente.activo = true */
	public static Specification<EmpresaClienteContadorEntity> clienteActivoTrue() {
		return (root, q, cb) -> cb.isTrue(root.join("cliente").get("activo"));
	}

	/**
	 * Nombre completo del cliente (Persona) – estilo DeudaCliente: concat +
	 * regexp_replace + lower
	 */
	public static Specification<EmpresaClienteContadorEntity> clienteNombreLike(String clienteNombreLike) {
		if (clienteNombreLike == null || clienteNombreLike.isBlank())
			return null;

		return (root, cq, cb) -> {
			cq.distinct(true);
			var cli = root.join("cliente");

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

			String pattern = "%" + clienteNombreLike.toLowerCase().trim().replaceAll("\\s+", " ") + "%";
			return cb.like(fullNameLower, pattern);
		};
	}

	public static Specification<EmpresaClienteContadorEntity> clienteCedulaIgual(String cedula) {
		if (cedula == null || cedula.isBlank())
			return null;
		return (root, q, cb) -> cb.equal(cb.lower(root.join("cliente").get("numeroCedula")),
				cedula.toLowerCase().trim());
	}

	public static Specification<EmpresaClienteContadorEntity> clienteCodigoIgual(String codigo) {
		if (codigo == null || codigo.isBlank())
			return null;
		return (root, q, cb) -> cb.equal(cb.lower(root.join("cliente").get("codigo")), codigo.toLowerCase().trim());
	}

	public static Specification<EmpresaClienteContadorEntity> direccionDepartamentoNombreLike(String depNombre) {
		if (depNombre == null || depNombre.isBlank())
			return null;
		return (root, q, cb) -> {
			var cli = root.join("cliente");
			var dir = cli.join("direccion", JoinType.LEFT);
			var dep = dir.join("departamento", JoinType.LEFT);
			return cb.like(cb.lower(dep.get("nombre")), "%" + depNombre.toLowerCase().trim() + "%");
		};
	}

	public static Specification<EmpresaClienteContadorEntity> direccionCiudadNombreLike(String cityNombre) {
		if (cityNombre == null || cityNombre.isBlank())
			return null;
		return (root, q, cb) -> {
			var cli = root.join("cliente");
			var dir = cli.join("direccion", JoinType.LEFT);
			var city = dir.join("ciudad", JoinType.LEFT);
			return cb.like(cb.lower(city.get("nombre")), "%" + cityNombre.toLowerCase().trim() + "%");
		};
	}

	public static Specification<EmpresaClienteContadorEntity> direccionCorregimientoNombreLike(String corrNombre) {
		if (corrNombre == null || corrNombre.isBlank())
			return null;
		return (root, q, cb) -> {
			var cli = root.join("cliente");
			var dir = cli.join("direccion", JoinType.LEFT);
			var corr = dir.join("corregimiento", JoinType.LEFT);
			return cb.like(cb.lower(corr.get("nombre")), "%" + corrNombre.toLowerCase().trim() + "%");
		};
	}

	public static Specification<EmpresaClienteContadorEntity> direccionDescripcionLike(String texto) {
		if (texto == null || texto.isBlank())
			return null;
		return (root, q, cb) -> {
			var cli = root.join("cliente");
			var dir = cli.join("direccion", JoinType.LEFT);
			return cb.like(cb.lower(cb.coalesce(dir.get("descripcion"), "")), "%" + texto.toLowerCase().trim() + "%");
		};
	}

	public static Specification<EmpresaClienteContadorEntity> clienteTelefonoLike(String telefono) {
		if (telefono == null || telefono.isBlank())
			return null;
		return (root, q, cb) -> {
			var cli = root.join("cliente");
			var sub = q.subquery(Integer.class);
			var t = sub.from(TelefonoGeneralEntity.class);
			sub.select(t.get("id")).where(cb.equal(t.get("persona").get("id"), cli.get("id")),
					cb.isTrue(t.get("activo")),
					cb.like(cb.lower(t.get("numero")), "%" + telefono.toLowerCase().trim() + "%"));
			return cb.exists(sub);
		};
	}

	public static Specification<EmpresaClienteContadorEntity> clienteCorreoLike(String correo) {
		if (correo == null || correo.isBlank())
			return null;
		return (root, q, cb) -> {
			var cli = root.join("cliente");
			var sub = q.subquery(Integer.class);
			var c = sub.from(CorreoGeneralEntity.class);
			sub.select(c.get("id")).where(cb.equal(c.get("persona").get("id"), cli.get("id")),
					cb.isTrue(c.get("activo")),
					cb.like(cb.lower(c.get("correo")), "%" + correo.toLowerCase().trim() + "%"));
			return cb.exists(sub);
		};
	}
	
	public static Specification<EmpresaClienteContadorEntity> clienteTipoDocumentoNombreLike(String tdNombre) {
	    if (tdNombre == null || tdNombre.isBlank()) return null;
	    return (root, q, cb) -> {
	        var cli = root.join("cliente");
	        var td  = cli.join("tipoDocumento", JoinType.LEFT);
	        return cb.like(cb.lower(td.get("nombre")), "%" + tdNombre.toLowerCase().trim() + "%");
	    };
	}
	
	public static Specification<EmpresaClienteContadorEntity> contadorSerialLike(String serialLike) {
        if (serialLike == null || serialLike.isBlank()) return null;
        return (root, q, cb) -> {
            var cont = root.join("contador", jakarta.persistence.criteria.JoinType.LEFT);
            return cb.like(cb.lower(cont.get("serial")), "%" + serialLike.toLowerCase().trim() + "%");
        };
    }

		/** Cliente estado */
	public static Specification<EmpresaClienteContadorEntity> clienteEstado(Boolean estado) {
		return (root, q, cb) -> estado != null ? 
		cb.equal(root.get("cliente").get("activo"), estado) : cb.conjunction();
	}

	/** Cliente NUID */
	public static Specification<EmpresaClienteContadorEntity> contadorNuid(Integer nuid) {
		return (root, q, cb) -> nuid != null ? 
		cb.equal(root.get("contador").get("nuid"), nuid) : cb.conjunction();
	}

}
