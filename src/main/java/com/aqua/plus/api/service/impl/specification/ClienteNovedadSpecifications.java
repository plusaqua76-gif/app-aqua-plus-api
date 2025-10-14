package com.aqua.plus.api.service.impl.specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.data.jpa.domain.Specification;

import com.aqua.plus.commons.entities.ClienteNovedadEntity;

public class ClienteNovedadSpecifications {

	private ClienteNovedadSpecifications() {
	}

	public static Specification<ClienteNovedadEntity> empresaId(Integer idEmpresa) {
		if (idEmpresa == null)
			return null;
		return (root, q, cb) -> cb.equal(root.join("empresaClienteContador").join("empresa").get("id"), idEmpresa);
	}

	public static Specification<ClienteNovedadEntity> tipoNovedadNombreLike(String novedad) {
		if (novedad == null || novedad.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.join("tipoNovedad").get("novedad")),
				"%" + novedad.trim().toUpperCase() + "%");
	}

	public static Specification<ClienteNovedadEntity> clienteNombreLike(String nombreCliente) {
		if (nombreCliente == null || nombreCliente.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.join("empresaClienteContador").join("cliente").get("nombre")),
				"%" + nombreCliente.trim().toUpperCase() + "%");
	}

	public static Specification<ClienteNovedadEntity> contadorSerialLike(String serial) {
		if (serial == null || serial.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.join("empresaClienteContador").join("contador").get("serial")),
				"%" + serial.trim().toUpperCase() + "%");
	}

	public static Specification<ClienteNovedadEntity> estadoDescripcionLike(String estadoDesc) {
		if (estadoDesc == null || estadoDesc.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.join("estado").get("descripcion")),
				"%" + estadoDesc.trim().toUpperCase() + "%");
	}

	public static Specification<ClienteNovedadEntity> codigoLike(String codigo) {
		if (codigo == null || codigo.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.get("codigo")), "%" + codigo.trim().toUpperCase() + "%");
	}

	public static Specification<ClienteNovedadEntity> descripcionLike(String descripcion) {
		if (descripcion == null || descripcion.isBlank())
			return null;
		return (root, q, cb) -> cb.like(cb.upper(root.get("descripcion")),
				"%" + descripcion.trim().toUpperCase() + "%");
	}

	public static Specification<ClienteNovedadEntity> activoEquals(Boolean activo) {
		if (activo == null)
			return null;
		return (root, q, cb) -> cb.equal(root.get("activo"), activo);
	}

	public static Specification<ClienteNovedadEntity> fechaCreacionEquals(LocalDate fecha) {
		if (fecha == null)
			return null;
		return (root, q, cb) -> {
			var start = fecha.atStartOfDay(ZoneId.systemDefault()).toInstant();
			var end = fecha.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();
			return cb.between(root.get("fechaCreacion"), Date.from(start), Date.from(end));
		};
	}
}
