package com.aqua.plus.api.service.impl.specification;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.ContadorEntity;
import com.aqua.plus.commons.entities.EmpresaClienteContadorEntity;

import jakarta.persistence.criteria.JoinType;

/**
 * @author nicope
 * @version 1.0
 *
 *          Especificaciones JPA reutilizables para filtrar Contadores en
 *          consultas; combinables dinámicamente.
 */

public final class ContadorSpecification {

	private ContadorSpecification() {
	}

	public static Specification<ContadorEntity> belongsToEmpresa(Integer empresaId) {
		return (root, query, cb) -> {
			if (empresaId == null)
				return cb.conjunction();
			var sub = query.subquery(Integer.class);
			var ecc = sub.from(EmpresaClienteContadorEntity.class);
			sub.select(ecc.get("id")).where(cb.equal(ecc.get("contador").get("id"), root.get("id")),
					cb.equal(ecc.get("empresa").get("id"), empresaId), cb.isTrue(ecc.get("activo")));
			return cb.exists(sub);
		};
	}

	public static Specification<ContadorEntity> serialLike(String serial) {
		return (root, q, cb) -> {
			if (serial == null || serial.isBlank())
				return cb.conjunction();
			return cb.like(cb.lower(root.get("serial")), "%" + serial.toLowerCase().trim() + "%");
		};
	}

	public static Specification<ContadorEntity> isActivoTrue() {
		return (root, query, cb) -> cb.isTrue(root.get("activo"));
	}
	
	/** Filtro por nombre de TipoContador */
    public static Specification<ContadorEntity> tipoContadorNombreLike(String nombre) {
        return (root, q, cb) -> {
            if (nombre == null || nombre.isBlank()) return cb.conjunction();
            var tc = root.join("tipoContador", JoinType.LEFT);
            return cb.like(cb.lower(tc.get("nombre")), "%" + nombre.toLowerCase().trim() + "%");
        };
    }

    /** La propiedad 'descripcion' en ContadorEntity es una DireccionEntity */
    public static Specification<ContadorEntity> direccionDescripcionLike(String desc) {
        return (root, q, cb) -> {
            if (desc == null || desc.isBlank()) return cb.conjunction();
            var dir = root.join("descripcion", JoinType.LEFT); // DireccionEntity
            return cb.like(cb.lower(cb.coalesce(dir.get("descripcion"), "")),
                           "%" + desc.toLowerCase().trim() + "%");
        };
    }

    /** Nombre de persona: match por cualquiera de sus partes (LIKE) */
    public static Specification<ContadorEntity> personaNombreLike(String nombreLike) {
        return (root, q, cb) -> {
            if (nombreLike == null || nombreLike.isBlank()) return cb.conjunction();
            String p = "%" + nombreLike.toLowerCase().trim() + "%";
            var per = root.join("cliente", JoinType.LEFT); // PersonaEntity
            return cb.or(
                cb.like(cb.lower(per.get("nombre")), p),
                cb.like(cb.lower(per.get("segundoNombre")), p),
                cb.like(cb.lower(per.get("apellido")), p),
                cb.like(cb.lower(per.get("segundoApellido")), p)
            );
        };
    }

    /** Cédula exacta (case-insensitive por si acaso) */
    public static Specification<ContadorEntity> personaCedulaEquals(String cedula) {
        return (root, q, cb) -> {
            if (cedula == null || cedula.isBlank()) return cb.conjunction();
            var per = root.join("cliente", JoinType.LEFT);
            return cb.equal(cb.lower(per.get("numeroCedula")), cedula.toLowerCase().trim());
        };
    }

}
