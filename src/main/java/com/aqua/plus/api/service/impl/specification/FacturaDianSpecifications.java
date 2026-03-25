package com.aqua.plus.api.service.impl.specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.InvoiceEntity;
import com.aqua.plus.commons.enums.LegalStatusEnum;

/* 
 * Specifications para InvoiceEntity.
 * Mantiene TODOS los filtros organizados y reutilizables.
 * 
 * @author nicolmm
 * @version 0.1
 */
public class FacturaDianSpecifications {

    /* 
    * Especificación para filtrar por id de empresa 
    */
    public static Specification<InvoiceEntity> idEmpresaEquals(Integer idEmpresa) {
        return (root, query, cb) -> idEmpresa != null ? 
        cb.equal(root.get("empresa").get("id"), idEmpresa) : cb.conjunction();
    }

    /*
     * Especificación para filtrar por código de factura
     */
    public static Specification<InvoiceEntity> codigoLike(String codigo) {
        if (codigo == null || codigo.isBlank()) return null;
        return (root, query, cb) -> codigo != null ? 
        cb.like(cb.upper(root.get("factura").get("codigo")), "%" + codigo.trim().toUpperCase() + "%") : cb.conjunction();
    }

    /*
    * Especificación para filtrar por estado de factura 
    */
    public static Specification<InvoiceEntity> estadoLegalLike(String estado) {
        if (estado == null || estado.trim().isEmpty()) {
            return null;
         }
        String estadoCodigo = LegalStatusEnum.fromDescripcion(estado);
        if (estadoCodigo == null) return null; // Si no se encuentra el código, no se aplica el filtro
        return (root, query, cb) -> cb.like(cb.upper(root.get("estadoLegal")), "%" + estadoCodigo.trim().toUpperCase() + "%");
       
    } 

    /*
     * Especificación para filtrar por nombre completo del cliente
     */
    public static Specification<InvoiceEntity> personaNombreCompletoLike(String personaNombreCompleto) {
        if (personaNombreCompleto == null || personaNombreCompleto.isBlank())
            return null;

        return (root, cq, cb) -> {
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

            String pattern = "%" + personaNombreCompleto.toLowerCase().trim().replaceAll("\\s+", " ") + "%";
            return cb.like(fullNameLower, pattern);
        };
    }

    /*
     * Especificación para filtrar por número de cédula de la persona
     */
    public static Specification<InvoiceEntity> numeroCedulaPersonaLike(String numeroCedula) {
        if (numeroCedula == null || numeroCedula.isBlank()) return null;
         return (root, query, cb) -> {
            query.distinct(true);
            var cli = root.join("cliente");
            return cb.like(cb.lower(cli.get("numeroCedula")), "%" + numeroCedula.toLowerCase().trim() + "%");
         };
    }

    /*
     * Especificación para filtrar por consumo
     */
    public static Specification<InvoiceEntity> consumoEquals(Integer consumo) {
        return (root, query, cb) -> consumo != null ? 
        cb.equal(root.get("factura").get("consumo"), consumo) : cb.conjunction();
    }

    /*
     * Especificación para filtrar por precio de la factura
     */
    public static Specification<InvoiceEntity> precioEquals(BigDecimal precio) {    
        return (root, query, cb) -> precio != null ? cb.equal(root.get("factura").get("precio"), precio) : cb.conjunction();
    }

    /*
    * Especificación para filtrar por fecha de emisión (fechaCreacion) entre un rango de fechas (fechaEmision y fechaEmision + 1 día)
     * Esto permite buscar facturas emitidas en una fecha específica sin importar la hora.
    */
    	public static Specification<InvoiceEntity> fechaEmisionBetween(LocalDate fechaEmision) {
        if (fechaEmision == null) return null;
        return (root, query, cb) -> {
            return cb.between(root.get("fechaCreacion"), fechaEmision.atStartOfDay(),
            fechaEmision.plusDays(1).atStartOfDay());
        };
	}

    /*
     * Especificación para filtrar por número de facturas elctronicas (número de factura)
     */
    public static Specification<InvoiceEntity> numeroEquals(Long numero) {
        return (root, query, cb) -> numero != null ? 
        cb.equal(root.get("numero"), numero) : cb.conjunction();
    }

    /** Helper opcional para encadenar specs ignorando nulls */
    @SafeVarargs
    public static <T> Specification<T> allOfNonNull(Specification<T>... specs) {
        Specification<T> result = (root, query, cb) -> cb.conjunction();
        for (Specification<T> s : java.util.Arrays.stream(specs).filter(Objects::nonNull).toList()) {
            result = result.and(s);
        }
        return result;
    }
}
