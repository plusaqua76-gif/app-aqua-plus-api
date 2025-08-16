package com.aqua.plus.api.service.impl;

import java.util.ArrayList;
import java.util.Objects;
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
import com.aqua.plus.commons.entities.UsuarioEntity;
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
    private final EncriptarDesencriptar serviceEncriptacion;
    private final JwtUtil jwtTokenUtil;

    /**
	 *
	 * @author npeñafiel
	 * @version 1.0
	 */
	@Transactional
	public ResponseEntity<ResponseDTO> autenticar(UsuarioDTO usuario) {
	    if (Objects.isNull(usuario) || Objects.isNull(usuario.getNombre()) || Objects.isNull(usuario.getContrasena())
	            || usuario.getNombre().isEmpty() || usuario.getContrasena().isEmpty()) {

	        ResponseDTO errorResponse = ResponseDTO.builder()
	                .success(false)
	                .message(Constantes.DATA_VALIDATION_MESSAGE)
	                .code(HttpStatus.BAD_REQUEST.value())
	                .response(null)
	                .build();

	        return ResponseEntity.badRequest().body(errorResponse);
	    }

	    Optional<UsuarioEntity> responseUsuario = usuarioRepository.findByNombreAndContrasena(
	            usuario.getNombre(),
	            serviceEncriptacion.encriptar(usuario.getContrasena())
	    );

	    if (responseUsuario.isPresent()) {
	        UsuarioEntity user = responseUsuario.get();

	        final String token = jwtTokenUtil.generateToken(user.getNombre());

	        AutenticacionDTO authData = AutenticacionDTO.builder()
	                .id(user.getId())
	                .nombre(user.getNombre())
	                .token(Constantes.BEARER + token)
	                .rolId(user.getRol() != null ? user.getRol().getId() : null)
	                .personaId(user.getPersona() != null ? user.getPersona().getId() : null)
	                .build();

	        ResponseDTO successResponse = ResponseDTO.builder()
	                .success(true)
	                .message(Constantes.AUTHENTICATION_SUCCESSFUL)
	                .code(HttpStatus.OK.value())
	                .response(authData)
	                .build();

	        return ResponseEntity.ok(successResponse);

	    } else {
	        ResponseDTO errorResponse = ResponseDTO.builder()
	                .success(false)
	                .message(Constantes.PLEASE_VERIFY_INCORRECT_DATA)
	                .code(HttpStatus.BAD_REQUEST.value())
	                .response(null)
	                .build();

	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
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
