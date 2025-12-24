package com.aqua.plus.api.service.impl;

import java.util.ArrayList;
import java.util.Optional;

import com.aqua.plus.commons.repositories.EmpresaClienteContadorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.configs.security.utils.JwtUtil;
import com.aqua.plus.api.utils.EncriptarDesencriptar;
import com.aqua.plus.commons.dtos.AutenticacionDTO;
import com.aqua.plus.commons.dtos.CiudadDTO;
import com.aqua.plus.commons.dtos.DepartamentoDTO;
import com.aqua.plus.commons.dtos.DireccionDTO;
import com.aqua.plus.commons.dtos.EmpresaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.UsuarioDTO;
import com.aqua.plus.commons.entities.DireccionEntity;
import com.aqua.plus.commons.entities.EmpresaEntity;
import com.aqua.plus.commons.entities.UsuarioEntity;
import com.aqua.plus.commons.repositories.EmpresaRepository;
import com.aqua.plus.commons.repositories.UsuarioRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author nicope
 * @version 1.0
 * 
 *          Clase que implementa lógica de autenticado.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class AutenticacionServiceImpl implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;
	private final EmpresaRepository empresaRepository;
	private final EmpresaClienteContadorRepository empresaClienteContadorRepository;
	private final EncriptarDesencriptar serviceEncriptacion;
	private final JwtUtil jwtTokenUtil;

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> autenticar(UsuarioDTO usuario) {
	    log.info("Inicio metodo autenticar: {}", (usuario != null ? usuario.getNombre() : null));

	    // ===== Validación =====
	    if (usuario == null || usuario.getNombre() == null || usuario.getNombre().isBlank()
	            || usuario.getContrasena() == null || usuario.getContrasena().isBlank()) {
	        return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
	                .message(Constantes.DATA_VALIDATION_MESSAGE).code(HttpStatus.BAD_REQUEST.value()).build());
	    }

	    // ===== Autenticación =====
	    final String passHash = serviceEncriptacion.encriptar(usuario.getContrasena());
	    var optUser = usuarioRepository.findByNombreAndContrasena(usuario.getNombre(), passHash);

	    if (optUser.isEmpty()) {
	        log.info("Fin metodo autenticar (credenciales inválidas) para usuario {}", usuario.getNombre());
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResponseDTO.builder().success(false)
	                .message(Constantes.PLEASE_VERIFY_INCORRECT_DATA).code(HttpStatus.UNAUTHORIZED.value()).build());
	    }

	    UsuarioEntity user = optUser.get();

	    if (!Boolean.TRUE.equals(user.getActivo())) {
	        log.info("Usuario {} encontrado pero NO activo", user.getNombre());
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResponseDTO.builder().success(false)
	                .message("El usuario no está activo.").code(HttpStatus.UNAUTHORIZED.value()).build());
	    }

	    // ===== Resolución de persona/empresa =====
	    final Integer personaId = (user.getPersona() != null ? user.getPersona().getId() : null);
	    Integer empresaId = null;

	    if (personaId != null) {
	        empresaId = empresaClienteContadorRepository.findFirstEmpresaIdByClienteId(personaId).orElse(null);

	        if (empresaId == null) {
	            log.warn("[AUTH] No hay ECC activo para personaId={}. Fallback por usuario.id={}", personaId, user.getId());
	            empresaId = empresaRepository.findByUsuario_Id(user.getId()).map(EmpresaEntity::getId).orElse(null);
	        }

	        log.info("[AUTH] Empresa resuelta: personaId={}, empresaId={}", personaId, empresaId);
	    } else {
	        empresaId = empresaRepository.findByUsuario_Id(user.getId()).map(EmpresaEntity::getId).orElse(null);
	        log.info("[AUTH] Empresa por usuario.id ({}): {}", user.getId(), empresaId);
	    }

	    // ===== Cargar empresa + dirección (si existe empresaId) =====
	    EmpresaDTO empresaDTO = null;
	    if (empresaId != null) {
	        var optEmpresa = empresaRepository.findByIdWithDireccion(empresaId); // método con JOIN FETCH
	        if (optEmpresa.isPresent()) {
	            var e = optEmpresa.get();
	            empresaDTO = EmpresaDTO.builder()
	                    .id(e.getId())
	                    .usuario(null) // evita payloads pesados/ciclos
	                    .direccion(mapDireccionToDto(e.getDireccion()))
	                    .nombre(e.getNombre())
	                    .nit(e.getNit())
	                    .codigo(e.getCodigo())
	                    .idEmpresaDian(e.getIdEmpresaDian())
	                    .activo(e.getActivo())
	                    .usuarioCreacion(e.getUsuarioCreacion())
	                    .fechaCreacion(e.getFechaCreacion())
	                    .usuarioModificacion(e.getUsuarioModificacion())
	                    .fechaModificacion(e.getFechaModificacion())
	                    .build();
	            log.info("[AUTH] EmpresaDTO construido para empresaId={}", empresaId);
	        } else {
	            log.warn("[AUTH] No se encontró Empresa con id={} usando findByIdWithDireccion", empresaId);
	        }
	    }

	    // ===== Tokens =====
	    final String accessToken = jwtTokenUtil.generateToken(
	            user.getNombre(), Constantes.KEY_TOKEN, Constantes.TIEMPO_VIGENCIA_TOKEN);

	    final String refreshToken = jwtTokenUtil.generateRefreshToken(
	            user.getNombre(), Constantes.KEY_TOKEN, Constantes.TIEMPO_VIGENCIA_REFRESH);

	    // Null-safe para rol/rolId
	    final String  rolNombre = (user.getRol() != null ? user.getRol().getNombre() : null);
	    final Integer rolId     = (user.getRol() != null ? user.getRol().getId()     : null);

	    // ===== Payload de respuesta =====
	    AutenticacionDTO authData = AutenticacionDTO.builder()
	            .id(user.getId())
	            .nombre(user.getNombre())
	            .token(Constantes.BEARER + accessToken)
	            .refreshToken(Constantes.BEARER + refreshToken)
	            .rol(rolNombre)
	            .rolId(rolId)
	            .personaId(personaId)
	            .empresaId(empresaId)
	            .empresa(empresaDTO)
	            .build();

	    var ok = ResponseDTO.builder()
	            .success(true)
	            .message(Constantes.AUTHENTICATION_SUCCESSFUL)
	            .code(HttpStatus.OK.value())
	            .response(authData)
	            .build();

	    log.info("Fin metodo autenticar OK para usuario {} (empresaId={}, empresaDTO? {})",
	            user.getNombre(), empresaId, (empresaDTO != null));
	    return ResponseEntity.ok(ok);
	}

	/**
	 * Mapea DireccionEntity -> DireccionDTO con DTOs anidados.
	 */
	private DireccionDTO mapDireccionToDto(DireccionEntity d) {
	    if (d == null) return null;

	    DepartamentoDTO depDto = null;
	    if (d.getDepartamento() != null) {
	        depDto = DepartamentoDTO.builder()
	                .id(d.getDepartamento().getId())
	                .build();
	    }

	    CiudadDTO ciuDto = null;
	    if (d.getCiudad() != null) {
	        ciuDto = CiudadDTO.builder()
	                .id(d.getCiudad().getId())
	                .build();
	    }

	    return DireccionDTO.builder()
	            .id(d.getId())
	            .departamento(depDto)
	            .ciudad(ciuDto)
	            .build();
	}


	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> refreshToken(String authorizationHeader) {
		log.info("Inicio metodo refreshToken");

		try {
			if (authorizationHeader == null || authorizationHeader.isBlank()) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("Falta el refresh token").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			String refresh = authorizationHeader.startsWith(Constantes.BEARER)
					? authorizationHeader.substring(Constantes.BEARER.length()).trim()
					: authorizationHeader.trim();

			if (!jwtTokenUtil.isSignatureValid(refresh, Constantes.KEY_TOKEN)) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResponseDTO.builder().success(false)
						.message("Refresh token inválido (firma)").code(HttpStatus.UNAUTHORIZED.value()).build());
			}

			String tipo = jwtTokenUtil.getClaimFromToken(refresh, c -> c.get("typ", String.class),
					Constantes.KEY_TOKEN);
			if (tipo == null || !"refresh".equalsIgnoreCase(tipo)) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResponseDTO.builder().success(false)
						.message("El token no es de tipo refresh").code(HttpStatus.UNAUTHORIZED.value()).build());
			}

			String username;
			try {
				username = jwtTokenUtil.getUsernameFromToken(refresh, Constantes.KEY_TOKEN);
			} catch (Exception e) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResponseDTO.builder().success(false)
						.message("Refresh token expirado").code(HttpStatus.UNAUTHORIZED.value()).build());
			}

			if (username == null || username.isBlank()) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResponseDTO.builder().success(false)
						.message("Refresh token inválido (sin subject)").code(HttpStatus.UNAUTHORIZED.value()).build());
			}

			Optional<UsuarioEntity> opt = usuarioRepository.findByNombre(username);
			if (opt.isEmpty() || !Boolean.TRUE.equals(opt.get().getActivo())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResponseDTO.builder().success(false)
						.message("Usuario inactivo o no existe").code(HttpStatus.UNAUTHORIZED.value()).build());
			}
			UsuarioEntity user = opt.get();

			String nuevoAccess = jwtTokenUtil.generateToken(user.getNombre(), Constantes.KEY_TOKEN,
					Constantes.TIEMPO_VIGENCIA_TOKEN);

			AutenticacionDTO authData = AutenticacionDTO.builder().token(Constantes.BEARER + nuevoAccess).build();

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message("Token refrescado correctamente")
					.code(HttpStatus.OK.value()).response(authData).build());

		} catch (Exception e) {
			log.error("Error en refreshToken", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseDTO.builder().success(false).message("Error interno al refrescar token")
							.code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	public UserDetails loadUserByUsername(String nombre) throws UsernameNotFoundException {
		Optional<UsuarioEntity> usuario = usuarioRepository.findByNombre(nombre);
		if (!usuario.isPresent()) {
			throw new UsernameNotFoundException("Nombre no encontrado: " + nombre);
		}
		return new User(nombre, usuario.get().getContrasena(), new ArrayList<>());
	}
}
