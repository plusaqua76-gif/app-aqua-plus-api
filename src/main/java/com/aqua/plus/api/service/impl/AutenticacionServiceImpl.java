package com.aqua.plus.api.service.impl;

import java.util.ArrayList;
import java.util.Optional;

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
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.UsuarioDTO;
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
	private final EncriptarDesencriptar serviceEncriptacion;
	private final JwtUtil jwtTokenUtil;

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> autenticar(UsuarioDTO usuario) {
	    log.info("Inicio metodo autenticar:{} ", usuario.getNombre());
	    if (usuario == null || usuario.getNombre() == null || usuario.getContrasena() == null
	            || usuario.getNombre().isEmpty() || usuario.getContrasena().isEmpty()) {
	        return ResponseEntity.badRequest().body(
	            ResponseDTO.builder()
	                .success(false)
	                .message(Constantes.DATA_VALIDATION_MESSAGE)
	                .code(HttpStatus.BAD_REQUEST.value())
	                .build()
	        );
	    }

	    Optional<UsuarioEntity> responseUsuario =
	        usuarioRepository.findByNombreAndContrasena(
	            usuario.getNombre(),
	            serviceEncriptacion.encriptar(usuario.getContrasena())
	        );

	    if (responseUsuario.isEmpty()) {
	        log.info("Fin metodo autenticar:{} ", Constantes.PLEASE_VERIFY_INCORRECT_DATA);
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
	            ResponseDTO.builder()
	                .success(false)
	                .message(Constantes.PLEASE_VERIFY_INCORRECT_DATA)
	                .code(HttpStatus.BAD_REQUEST.value())
	                .build()
	        );
	    }

	    UsuarioEntity user = responseUsuario.get();

	    if (!Boolean.TRUE.equals(user.getActivo())) {
	        log.info("Usuario {} encontrado pero no está activo", user.getNombre());
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
	            ResponseDTO.builder()
	                .success(false)
	                .message("El usuario no está activo.")
	                .code(HttpStatus.UNAUTHORIZED.value())
	                .build()
	        );
	    }
	    
	    Integer empresaId = empresaRepository
	            .findByUsuario_Id(user.getId())
	            .map(EmpresaEntity::getId)
	            .orElse(null);

	    final String accessToken = jwtTokenUtil.generateToken(
	        user.getNombre(),
	        Constantes.KEY_TOKEN,
	        Constantes.TIEMPO_VIGENCIA_TOKEN
	    );

	    final String refreshToken = jwtTokenUtil.generateRefreshToken(
	        user.getNombre(),
	        Constantes.KEY_TOKEN,
	        Constantes.TIEMPO_VIGENCIA_REFRESH
	    );

	    AutenticacionDTO authData = AutenticacionDTO.builder()
	        .id(user.getId())
	        .nombre(user.getNombre())
	        .token(Constantes.BEARER + accessToken)       // access
	        .refreshToken(Constantes.BEARER + refreshToken) // refresh
	        .rol(user.getRol() != null ? user.getRol().getNombre() : null)
	        .personaId(user.getPersona() != null ? user.getPersona().getId() : null)
	        .empresaId(empresaId)
	        .build();

	    ResponseDTO successResponse = ResponseDTO.builder()
	        .success(true)
	        .message(Constantes.AUTHENTICATION_SUCCESSFUL)
	        .code(HttpStatus.OK.value())
	        .response(authData)
	        .build();

	    log.info("Fin metodo autenticar:{} ", usuario.getNombre());
	    return ResponseEntity.ok(successResponse);
	}

	
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> refreshToken(String authorizationHeader) {
	    log.info("Inicio metodo refreshToken");

	    try {
	        if (authorizationHeader == null || authorizationHeader.isBlank()) {
	            return ResponseEntity.badRequest().body(
	                ResponseDTO.builder()
	                    .success(false)
	                    .message("Falta el refresh token")
	                    .code(HttpStatus.BAD_REQUEST.value())
	                    .build()
	            );
	        }

	        String refresh = authorizationHeader.startsWith(Constantes.BEARER)
	                ? authorizationHeader.substring(Constantes.BEARER.length()).trim()
	                : authorizationHeader.trim();

	        if (!jwtTokenUtil.isSignatureValid(refresh, Constantes.KEY_TOKEN)) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
	                ResponseDTO.builder()
	                    .success(false)
	                    .message("Refresh token inválido (firma)")
	                    .code(HttpStatus.UNAUTHORIZED.value())
	                    .build()
	            );
	        }

	        String tipo = jwtTokenUtil.getClaimFromToken(refresh, c -> c.get("typ", String.class), Constantes.KEY_TOKEN);
	        if (tipo == null || !"refresh".equalsIgnoreCase(tipo)) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
	                ResponseDTO.builder()
	                    .success(false)
	                    .message("El token no es de tipo refresh")
	                    .code(HttpStatus.UNAUTHORIZED.value())
	                    .build()
	            );
	        }

	        String username;
	        try {
	            username = jwtTokenUtil.getUsernameFromToken(refresh, Constantes.KEY_TOKEN);
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
	                ResponseDTO.builder()
	                    .success(false)
	                    .message("Refresh token expirado")
	                    .code(HttpStatus.UNAUTHORIZED.value())
	                    .build()
	            );
	        }

	        if (username == null || username.isBlank()) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
	                ResponseDTO.builder()
	                    .success(false)
	                    .message("Refresh token inválido (sin subject)")
	                    .code(HttpStatus.UNAUTHORIZED.value())
	                    .build()
	            );
	        }

	        Optional<UsuarioEntity> opt = usuarioRepository.findByNombre(username);
	        if (opt.isEmpty() || !Boolean.TRUE.equals(opt.get().getActivo())) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
	                ResponseDTO.builder()
	                    .success(false)
	                    .message("Usuario inactivo o no existe")
	                    .code(HttpStatus.UNAUTHORIZED.value())
	                    .build()
	            );
	        }
	        UsuarioEntity user = opt.get();

	        String nuevoAccess = jwtTokenUtil.generateToken(
	            user.getNombre(),
	            Constantes.KEY_TOKEN,
	            Constantes.TIEMPO_VIGENCIA_TOKEN
	        );

	        AutenticacionDTO authData = AutenticacionDTO.builder()
	            .token(Constantes.BEARER + nuevoAccess)
	            .build();

	        return ResponseEntity.ok(
	            ResponseDTO.builder()
	                .success(true)
	                .message("Token refrescado correctamente")
	                .code(HttpStatus.OK.value())
	                .response(authData)
	                .build()
	        );

	    } catch (Exception e) {
	        log.error("Error en refreshToken", e);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
	            ResponseDTO.builder()
	                .success(false)
	                .message("Error interno al refrescar token")
	                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
	                .build()
	        );
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
