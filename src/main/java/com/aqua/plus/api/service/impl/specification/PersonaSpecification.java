package com.aqua.plus.api.service.impl.specification;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.CorreoGeneralEntity;
import com.aqua.plus.commons.entities.EmpresaClienteContadorEntity;
import com.aqua.plus.commons.entities.PersonaEntity;
import com.aqua.plus.commons.entities.TelefonoGeneralEntity;

import jakarta.persistence.criteria.JoinType;

public final class PersonaSpecification {

	private PersonaSpecification() {}

    // Pertenece a empresa (vía empresa_cliente_contador activo)
    public static Specification<PersonaEntity> belongsToEmpresa(Integer empresaId) {
        return (root, query, cb) -> {
            if (empresaId == null) return cb.conjunction();
            var sub = query.subquery(Integer.class);
            var ecc = sub.from(EmpresaClienteContadorEntity.class);
            sub.select(ecc.get("id"))
               .where(
                   cb.equal(ecc.get("persona").get("id"), root.get("id")),
                   cb.equal(ecc.get("empresa").get("id"), empresaId),
                   cb.isTrue(ecc.get("activo"))
               );
            return cb.exists(sub);
        };
    }

    public static Specification<PersonaEntity> nameLike(String nombreLike) {
        return (root, q, cb) -> {
            if (nombreLike == null || nombreLike.isBlank()) return cb.conjunction();
            String p = "%" + nombreLike.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("primerNombre")), p),
                cb.like(cb.lower(root.get("segundoNombre")), p),
                cb.like(cb.lower(root.get("primerApellido")), p),
                cb.like(cb.lower(root.get("segundoApellido")), p)
            );
        };
    }

    public static Specification<PersonaEntity> hasNumeroCedula(String cedula) {
        return (root, q, cb) -> (cedula != null && !cedula.isBlank())
                ? cb.equal(cb.lower(root.get("numeroCedula")), cedula.toLowerCase())
                : cb.conjunction();
    }

    public static Specification<PersonaEntity> hasCodigo(String codigo) {
        return (root, q, cb) -> (codigo != null && !codigo.isBlank())
                ? cb.equal(cb.lower(root.get("codigo")), codigo.toLowerCase())
                : cb.conjunction();
    }

    // Por nombre de departamento (JOIN dirección -> departamento)
    public static Specification<PersonaEntity> byDepartamentoNombre(String depNombre) {
        return (root, q, cb) -> {
            if (depNombre == null || depNombre.isBlank()) return cb.conjunction();
            var dir = root.join("direccion", JoinType.LEFT);
            var dep = dir.join("departamento", JoinType.LEFT);
            return cb.like(cb.lower(dep.get("nombre")), "%" + depNombre.toLowerCase().trim() + "%");
        };
    }

    // Por nombre de ciudad
    public static Specification<PersonaEntity> byCiudadNombre(String cityNombre) {
        return (root, q, cb) -> {
            if (cityNombre == null || cityNombre.isBlank()) return cb.conjunction();
            var dir = root.join("direccion", JoinType.LEFT);
            var city = dir.join("ciudad", JoinType.LEFT);
            return cb.like(cb.lower(city.get("nombre")), "%" + cityNombre.toLowerCase().trim() + "%");
        };
    }

    // Por nombre de corregimiento
    public static Specification<PersonaEntity> byCorregimientoNombre(String corrNombre) {
        return (root, q, cb) -> {
            if (corrNombre == null || corrNombre.isBlank()) return cb.conjunction();
            var dir = root.join("direccion", JoinType.LEFT);
            var corr = dir.join("corregimiento", JoinType.LEFT);
            return cb.like(cb.lower(corr.get("nombre")), "%" + corrNombre.toLowerCase().trim() + "%");
        };
    }

    // Existe teléfono que contenga 'telefono'
    public static Specification<PersonaEntity> hasTelefonoLike(String telefono) {
        return (root, query, cb) -> {
            if (telefono == null || telefono.isBlank()) return cb.conjunction();
            var sub = query.subquery(Integer.class);
            var tel = sub.from(TelefonoGeneralEntity.class);
            sub.select(tel.get("id"))
               .where(
                   cb.equal(tel.get("persona").get("id"), root.get("id")),
                   cb.isTrue(tel.get("activo")),
                   cb.like(cb.lower(tel.get("numero")), "%" + telefono.toLowerCase().trim() + "%")
               );
            return cb.exists(sub);
        };
    }

    // Existe correo que contenga 'correo'
    public static Specification<PersonaEntity> hasCorreoLike(String correo) {
        return (root, query, cb) -> {
            if (correo == null || correo.isBlank()) return cb.conjunction();
            var sub = query.subquery(Integer.class);
            var c = sub.from(CorreoGeneralEntity.class);
            sub.select(c.get("id"))
               .where(
                   cb.equal(c.get("persona").get("id"), root.get("id")),
                   cb.isTrue(c.get("activo")),
                   cb.like(cb.lower(c.get("correo")), "%" + correo.toLowerCase().trim() + "%")
               );
            return cb.exists(sub);
        };
    }
}
