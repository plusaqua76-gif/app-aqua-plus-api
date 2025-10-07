package com.aqua.plus.api.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aqua.plus.api.service.impl.AutenticacionServiceImpl;
import com.aqua.plus.api.service.impl.UsuarioServiceImpl;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.UpdatePasswordDTO;
import com.aqua.plus.commons.dtos.UsuarioDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * @author nicope
 * @version 1.0
 * 
 *          Controlador que expone los servicios para trabajar con objeto(s) de
 *          tipo (Usuario).
 */

@RestController
@RequestMapping("/api/v1/usuario")
@Tag(name = "Usuario - Controller", description = "Controller encargado de gestionar las operaciones de los usuarios")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
public class UsuarioController {

	private final UsuarioServiceImpl usuarioServiceImpl;
	private final AutenticacionServiceImpl autenticacionServiceImpl;

	@Operation(summary = "Autenticar Usuario")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Autenticación exitosa", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "Solicitud inválida", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Error interno del servidor", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@PostMapping("/validar")
	public ResponseEntity<ResponseDTO> validarUsuario(@RequestBody UsuarioDTO usuario) {
		return this.autenticacionServiceImpl.autenticar(usuario);
	}

	@Operation(summary = "Refrescar token Usuario Autenticación")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Autenticación exitosa", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "Solicitud inválida", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Error interno del servidor", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@PostMapping("/auth/refresh")
	public ResponseEntity<ResponseDTO> refresh(
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return autenticacionServiceImpl.refreshToken(authorization);
	}

	@Operation(summary = "Guardar o actualizar usuario")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Se ha guardado satisfactoriamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "200", description = "Se ha actualizado satisfactoriamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@PostMapping
	public ResponseEntity<ResponseDTO> save(@RequestBody UsuarioDTO usuarioDTO) {
		return usuarioServiceImpl.save(usuarioDTO);
	}

	@Operation(summary = "Buscar usuario por id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Se ha guardado satisfactoriamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@GetMapping("/{id}")
	public ResponseEntity<ResponseDTO> getById(@PathVariable Integer id) {
		return usuarioServiceImpl.findById(id);
	}

	@Operation(summary = "Buscar todos los usuarios inactivos")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Se ha guardado satisfactoriamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@GetMapping("/inactivos")
	public ResponseEntity<ResponseDTO> listarInactivos(@RequestParam(required = false) String nombre,
			@RequestParam(required = false, name = "estado") String estadoNombre,
			@PageableDefault(size = 20, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable,
			HttpServletRequest request) {

		boolean pagingRequested = request.getParameter("page") != null || request.getParameter("size") != null
				|| request.getParameter("sort") != null;

		Pageable pageToUse = pagingRequested ? pageable : Pageable.unpaged();

		return usuarioServiceImpl.findActivosEInactivos(nombre, estadoNombre, pageToUse);
	}

	@Operation(summary = "Listar todos los usuarios")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Se ha guardado satisfactoriamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@GetMapping("/all")
	public ResponseEntity<ResponseDTO> getAll() {
		return usuarioServiceImpl.findAll();
	}

	@Operation(summary = "Eliminar  usuario por id ")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Se ha guardado satisfactoriamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@DeleteMapping("/{id}")
	public ResponseEntity<ResponseDTO> deleteById(@PathVariable Integer id) {
		return usuarioServiceImpl.deleteById(id);
	}

	@Operation(summary = "Actualizar password por id usuario ")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Se ha guardado satisfactoriamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@PutMapping("/actualiza-contra/{id}")
	public ResponseEntity<ResponseDTO> actualizarContrasena(@PathVariable Integer id,
			@RequestBody UpdatePasswordDTO dto) {
		return usuarioServiceImpl.updatePassword(id, dto.getNuevaContrasena(), dto.getUsuarioModificacion());
	}

	@Operation(summary = "Recuperar password por id usuario ")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Se ha guardado satisfactoriamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@PostMapping("/recover-password")
	public ResponseEntity<ResponseDTO> recoverPassword(@RequestParam(required = true) String correo,
			@RequestParam(required = false) String codigoPlantilla) {
		return usuarioServiceImpl.recoverPassword(correo, codigoPlantilla);
	}

	@Operation(summary = "Actualizar contraseña por token")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Contraseña actualizada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "400", description = "Solicitud inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "401", description = "No autorizado (token ausente/ inválido/ expirado)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error interno", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@PostMapping("/update-password")
	public ResponseEntity<ResponseDTO> updatePasswordByToken(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestHeader(value = "Recover-Token", required = false) String recoverTokenHeader,
			@RequestParam(value = "Authorization", required = false) String authorizationQueryParam,
			@RequestBody UsuarioDTO usuarioDTO) {

		String tokenCarrier = firstNonBlank(recoverTokenHeader, authorizationHeader, authorizationQueryParam);

		return usuarioServiceImpl.updatePasswordByToken(tokenCarrier, usuarioDTO);
	}

	private static String firstNonBlank(String... vals) {
		if (vals == null)
			return null;
		for (String v : vals) {
			if (v != null && !v.isBlank())
				return v;
		}
		return null;
	}

	@Operation(summary = "Actualizar imagen del usuario")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Imagen actualizada exitosamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Error actualizando la imagen", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }) })

	@PutMapping("/imagen/{id}")
	public ResponseEntity<ResponseDTO> actualizarImagenUsuario(@PathVariable Integer id,
			@RequestParam("imagen") MultipartFile imagen,
			@RequestParam("usuarioModificacion") String usuarioModificacion) {
		try {
			byte[] imagenBytes = imagen.getBytes();
			return usuarioServiceImpl.updateImage(id, imagenBytes, usuarioModificacion);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message("Error procesando la imagen").code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Operation(summary = "Listar menús por empresa y rol (chucha Pipe)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Consulta exitosa", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "400", description = "Petición inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error inesperado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@GetMapping("/menus")
	public ResponseEntity<ResponseDTO> getMenusByEmpresaAndRol(
			@Parameter(description = "ID de la empresa", required = true) @RequestParam(name = "empresaId", required = true) Integer empresaId,

			@Parameter(description = "ID del rol", required = true) @RequestParam(name = "rolId", required = true) Integer rolId) {
		return usuarioServiceImpl.findMenusByEmpresaAndRol(empresaId, rolId);
	}

}
