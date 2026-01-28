package com.aqua.plus.api.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aqua.plus.api.service.impl.DocumentoServiceImpl;
import com.aqua.plus.commons.dtos.DocumentoDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author nicope
 * @version 1.0
 * 
 *          Controlador que expone los servicios para trabajar con objeto(s) de
 *          tipo (Documento).
 */

@RestController
@RequestMapping("/api/v1/documento")
@Tag(name = "Documento - Controller", description = "Controller encargado de gestionar las operaciones de los documentos")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
@Slf4j
public class DocumentoController {

	private final DocumentoServiceImpl documentoServiceImpl;

	@Operation(summary = "Subir documento (JSON/base64) a Azure y registrar en BD")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Documento guardado correctamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "400", description = "Petición inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error inesperado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@PostMapping(value = "/upload", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseDTO> uploadBase64(@RequestBody Map<String, Object> body) {
		String base64File = body.get("base64File") != null ? body.get("base64File").toString() : null;
		Integer idEmpresa = body.get("idEmpresa") != null ? Integer.valueOf(body.get("idEmpresa").toString()) : null;
		Integer idPersona = body.get("idPersona") != null ? Integer.valueOf(body.get("idPersona").toString()) : null;
		String nombreArchivo = body.get("nombreArchivo") != null ? body.get("nombreArchivo").toString() : null;
		String extension = body.get("extension") != null ? body.get("extension").toString() : null;
		String usuario = body.get("usuario") != null ? body.get("usuario").toString() : null;
		String categoriaCod = body.get("categoriaCodigo") != null ? body.get("categoriaCodigo").toString() : null;
		Integer idClienteNovedad = body.get("idClienteNovedad") != null
				? Integer.valueOf(body.get("idClienteNovedad").toString())
				: null;
		Boolean publico = body.get("publico") != null ? Boolean.valueOf(body.get("publico").toString()) : null;

		return documentoServiceImpl.saveDocumentoBase64(base64File, idEmpresa, idPersona, nombreArchivo, extension,
				usuario, categoriaCod, idClienteNovedad, publico);
	}

	@Operation(summary = "Actualizar documento por ruta (sobrescribe el blob en Azure)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Se ha actualizado satisfactoriamente", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@PostMapping("/actualizar")
	public ResponseEntity<ResponseDTO> actualizarPorRuta(@RequestBody DocumentoDTO documentoDTO) {
		return documentoServiceImpl.actualizarDocumentoPorRutaBase64(documentoDTO);
	}

	@Operation(summary = "Eliminar documento")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Se ha eliminado satisfactoriamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "404", description = "Documento no encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error inesperado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@DeleteMapping("/{idDocumento}")
	public ResponseEntity<ResponseDTO> delete(@PathVariable Integer idDocumento,
			@RequestParam(required = false) String usuario) {
		return documentoServiceImpl.deleteDocumento(idDocumento, usuario);
	}

	@Operation(summary = "Obtener documento en Base64 por ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Consulta exitosa", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "404", description = "Documento no encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error inesperado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@GetMapping("/{idDocumento}/base64")
	public ResponseEntity<ResponseDTO> getBase64(@PathVariable Integer idDocumento) {
		return documentoServiceImpl.getDocumentoBase64PorId(idDocumento);
	}

	@Operation(summary = "Listar logos (FOT) de todas las empresas con paginación")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Consulta exitosa", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
			@ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }), })
	@GetMapping("/carrucel")
	public ResponseEntity<ResponseDTO> listarLogosEmpresasPaginado(
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "size", required = false) Integer size) {

		return documentoServiceImpl.listarLogosEmpresaCarrucel(page, size);
	}

	@Operation(summary = "Listar documentos por persona")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Consulta exitosa", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error inesperado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@GetMapping("/persona/{idPersona}")
	public ResponseEntity<ResponseDTO> listarPorPersona(@PathVariable Integer idPersona) {
		return documentoServiceImpl.listarPorPersona(idPersona);
	}

	@Operation(summary = "Listar documentos por categoría (Base64), con idEmpresa opcional", description = "Si se envía idEmpresa, filtra por empresa y categoría. Si no, lista por categoría global.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Consulta exitosa", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "400", description = "Petición inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "500", description = "Error inesperado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))) })
	@GetMapping("/empresa/{idEmpresa}/categoria/{categoriaCodigo}")
	public ResponseEntity<ResponseDTO> listarPorEmpresaYCategoriaConBase64(
	        @PathVariable Integer idEmpresa,
	        @PathVariable String categoriaCodigo
	) {
	    return documentoServiceImpl.listarPorEmpresaYCategoriaCodigo(idEmpresa, categoriaCodigo);
	}

}
