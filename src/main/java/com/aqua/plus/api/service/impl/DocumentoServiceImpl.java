package com.aqua.plus.api.service.impl;

import java.time.Year;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IDocumentoService;
import com.aqua.plus.commons.dtos.DocumentoDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ClienteNovedadEntity;
import com.aqua.plus.commons.entities.DocumentoEntity;
import com.aqua.plus.commons.entities.EmpresaEntity;
import com.aqua.plus.commons.entities.PersonaEntity;
import com.aqua.plus.commons.maps.DocumentoMapper;
import com.aqua.plus.commons.repositories.CategoriaDocumentoRepository;
import com.aqua.plus.commons.repositories.DocumentoRepository;
import com.aqua.plus.commons.repositories.EmpresaRepository;
import com.aqua.plus.commons.repositories.PersonaRepository;
import com.aqua.plus.commons.repositories.TipoDocumentoRepository;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentoServiceImpl implements IDocumentoService {

	@Value("${app.azure.storage.connection-string}")
	private String storageConnectionString;

	@Value("${app.azure.storage.container-name}")
	private String containerName;

	private final DocumentoRepository documentoRepository;
	private final CategoriaDocumentoRepository categoriaDocumentoRepository;
	private final TipoDocumentoRepository tipoDocumentoRepository;
	private final EmpresaRepository empresaRepository;
	private final PersonaRepository personaRepository;
	private final DocumentoMapper documentoMapper;

	@PersistenceContext
	private EntityManager em;

	private BlobContainerClient container() {
		BlobServiceClient svc = new BlobServiceClientBuilder().connectionString(storageConnectionString).buildClient();
		return svc.getBlobContainerClient(containerName);
	}

	/** Prefijo: ANEXOS/{yyyy}/{idTipo}-{identificador}/ */
	private String buildPrefix(Integer idTipoDocumento, String identificador, Integer year) {
		int y = (year != null ? year : Year.now().getValue());
		return String.format("ANEXOS/%d/%d-%s/", y, idTipoDocumento, identificador);
	}

	/** Nombre final del blob dentro del prefijo. */
	private String buildBlobName(String prefix, String nombre, String extension) {
		String base = (nombre == null || nombre.isBlank()) ? "documento" : nombre.trim();
		String ext = (extension == null) ? "" : extension.trim();
		if (!ext.isEmpty() && ext.startsWith("."))
			ext = ext.substring(1);
		String filename = ext.isEmpty() ? base : base + "." + ext.toLowerCase();
		filename = filename.replace(" ", "_");
		return prefix + filename;
	}

	private static byte[] decodeBase64Lenient(String input) {
		String s = input.trim();
		if (s.startsWith("data:")) {
			int comma = s.indexOf(',');
			if (comma >= 0)
				s = s.substring(comma + 1);
		}
		s = s.replaceAll("\\s", "");
		return java.util.Base64.getDecoder().decode(s);
	}

	private String guessContentType(byte[] bytes) {
		try (var is = new java.io.ByteArrayInputStream(bytes)) {
			String ct = java.net.URLConnection.guessContentTypeFromStream(is);
			return (ct != null) ? ct : "application/octet-stream";
		} catch (Exception e) {
			return "application/octet-stream";
		}
	}

	private String extensionFromContentType(String ct) {
		if (ct == null)
			return null;
		return switch (ct) {
		case "image/png" -> "png";
		case "image/jpeg" -> "jpg";
		case "image/gif" -> "gif";
		case "application/pdf" -> "pdf";
		default -> null;
		};
	}

	/**
	 * Sube documento a Azure y persiste fila en public.documento. Ruta:
	 * ANEXOS/{yyyy}/{idTipo}-{identificador}/archivo.ext (año actual). Recibe el
	 * archivo en Base64 (JSON), no usa Multipart.
	 */
	@Transactional
	public ResponseEntity<ResponseDTO> saveDocumentoBase64(String base64File, Integer idEmpresa, Integer idPersona,
			String nombreArchivo, String extension, String usuario, String categoriaCodigo, Integer idClienteNovedad) {
		log.info(
				"Subiendo documento (base64): empresa={}, persona={}, nombre={}, ext={}, categoriaCodigo={}, idClienteNovedad={}",
				idEmpresa, idPersona, nombreArchivo, extension, categoriaCodigo, idClienteNovedad);

		try {
			if (base64File == null || base64File.isBlank()) {
				return bad(HttpStatus.BAD_REQUEST, "Archivo (base64) vacío");
			}
			if ((idEmpresa == null && idPersona == null) || (idEmpresa != null && idPersona != null)) {
				return bad(HttpStatus.BAD_REQUEST, "Debe enviar idEmpresa o idPersona (exclusivo)");
			}

			final String catCod = (categoriaCodigo == null || categoriaCodigo.isBlank()) ? "FOT"
					: categoriaCodigo.trim().toUpperCase();

			var categoria = categoriaDocumentoRepository.findFirstByCodigoAndActivoTrue(catCod).orElseGet(
					() -> (categoriaCodigo == null) ? categoriaDocumentoRepository.findById(1).orElse(null) : null);

			if (categoria == null) {
				return bad(HttpStatus.BAD_REQUEST, "La categoría de documento no existe: " + catCod);
			}

			byte[] bytes;
			try {
				bytes = decodeBase64Lenient(base64File);
			} catch (IllegalArgumentException ex) {
				return bad(HttpStatus.BAD_REQUEST, "Base64 inválido");
			}
			if (bytes.length == 0) {
				return bad(HttpStatus.BAD_REQUEST, "Archivo (base64) vacío");
			}

			if (nombreArchivo == null || nombreArchivo.isBlank())
				nombreArchivo = "documento";
			if (extension == null || extension.isBlank()) {
				extension = extensionFromContentType(guessContentType(bytes));
			}

			String tipoCodigoRuta, identificador;
			if (idPersona != null) {
				tipoCodigoRuta = "CC";
				var persona = personaRepository.findById(idPersona).orElse(null);
				if (persona == null)
					return bad(HttpStatus.BAD_REQUEST, "Persona no existe");
				identificador = persona.getNumeroCedula();
				if (identificador == null || identificador.isBlank()) {
					return bad(HttpStatus.BAD_REQUEST, "La persona no tiene número de documento (cc)");
				}
			} else {
				tipoCodigoRuta = "NI";
				var empresa = empresaRepository.findById(idEmpresa).orElse(null);
				if (empresa == null)
					return bad(HttpStatus.BAD_REQUEST, "Empresa no existe");
				identificador = empresa.getNit();
				if (identificador == null || identificador.isBlank()) {
					return bad(HttpStatus.BAD_REQUEST, "La empresa no tiene NIT");
				}
			}

			var tipoRuta = tipoDocumentoRepository.findByCodigoAndActivoTrue(tipoCodigoRuta).orElse(null);
			Integer idTipoRuta = (tipoRuta != null ? tipoRuta.getId() : ("CC".equals(tipoCodigoRuta) ? 7 : 12));

			String prefix = buildPrefix(idTipoRuta, identificador, null);
			String blobPath = buildBlobName(prefix, nombreArchivo, extension);

			BlobClient blob = container().getBlobClient(blobPath);
			BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(guessContentType(bytes));
			try (var bais = new java.io.ByteArrayInputStream(bytes)) {
				blob.upload(bais, bytes.length, true);
				blob.setHttpHeaders(headers);
				var tags = new java.util.HashMap<String, String>();
				tags.put("categoriaDocumentoCodigo", categoria.getCodigo());
				tags.put("tipoRutaCodigo", tipoCodigoRuta);
				if (idEmpresa != null)
					tags.put("empresaId", String.valueOf(idEmpresa));
				if (idPersona != null)
					tags.put("personaId", String.valueOf(idPersona));
				if (idClienteNovedad != null)
					tags.put("clienteNovedadId", String.valueOf(idClienteNovedad));
				try {
					blob.setTags(tags);
				} catch (Exception ignore) {
				}
			}

			DocumentoEntity entity = new DocumentoEntity();
			entity.setCategoriaDocumento(categoria);
			if (idEmpresa != null)
				entity.setEmpresa(em.getReference(EmpresaEntity.class, idEmpresa));
			if (idPersona != null)
				entity.setPersona(em.getReference(PersonaEntity.class, idPersona));
			if (idClienteNovedad != null) {
				entity.setClienteNovedad(em.getReference(ClienteNovedadEntity.class, idClienteNovedad));
			}
			entity.setRuta(blobPath);
			entity.setNombre(nombreArchivo.trim());
			entity.setExtension(extension == null ? null : extension.trim().toLowerCase());
			entity.setActivo(Boolean.TRUE);
			entity.setUsuarioCreacion(usuario == null ? "system" : usuario);
			entity.setFechaCreacion(new java.util.Date());

			entity = documentoRepository.save(entity);
			DocumentoDTO dto = documentoMapper.entityToDto(entity);
			return ok("Documento guardado correctamente", dto);

		} catch (Exception e) {
			log.error("Error guardando documento (base64)", e);
			return err("Error guardando documento");
		}
	}

	@Transactional
	public ResponseEntity<ResponseDTO> actualizarDocumentoPorRutaBase64(DocumentoDTO dto) {
		log.info("Actualizar documento por ruta: {}", (dto != null ? dto.getRuta() : null));

		try {
			if (dto == null)
				return bad(HttpStatus.BAD_REQUEST, "Body requerido");
			if (dto.getRuta() == null || dto.getRuta().isBlank())
				return bad(HttpStatus.BAD_REQUEST, "ruta es obligatoria");
			if (dto.getImagen() == null || dto.getImagen().isBlank())
				return bad(HttpStatus.BAD_REQUEST, "imagen (base64) es obligatoria");

			DocumentoEntity doc = documentoRepository.findByRuta(dto.getRuta()).orElse(null);
			if (doc == null)
				return bad(HttpStatus.NOT_FOUND, "No existe documento con la ruta indicada");

			byte[] bytes;
			try {
				bytes = decodeBase64Lenient(dto.getImagen());
			} catch (IllegalArgumentException ex) {
				return bad(HttpStatus.BAD_REQUEST, "Base64 inválido");
			}
			if (bytes.length == 0)
				return bad(HttpStatus.BAD_REQUEST, "Archivo (base64) vacío");

			String ext = (dto.getExtension() == null || dto.getExtension().isBlank())
					? extensionFromContentType(guessContentType(bytes))
					: dto.getExtension();
			String nombreDestino = (dto.getNombre() != null && !dto.getNombre().isBlank()) ? dto.getNombre()
					: doc.getNombre();

			String oldPath = doc.getRuta();
			String oldFilename = filenameDeRuta(oldPath);
			String newFilename = buildFilename(nombreDestino, ext);
			String prefix = obtenerPrefijoDeRuta(oldPath);
			String newPath = prefix + newFilename;

			BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(guessContentType(bytes));

			if (!newFilename.equals(oldFilename)) {
				BlobClient newBlob = container().getBlobClient(newPath);
				try (var bais = new java.io.ByteArrayInputStream(bytes)) {
					newBlob.upload(bais, bytes.length, true);
					newBlob.setHttpHeaders(headers);

					try {
						var tags = new java.util.HashMap<String, String>();
						if (doc.getCategoriaDocumento() != null)
							tags.put("categoriaDocumentoCodigo", doc.getCategoriaDocumento().getCodigo());
						if (doc.getEmpresa() != null && doc.getEmpresa().getId() != null)
							tags.put("empresaId", String.valueOf(doc.getEmpresa().getId()));
						if (doc.getPersona() != null && doc.getPersona().getId() != null)
							tags.put("personaId", String.valueOf(doc.getPersona().getId()));
						newBlob.setTags(tags);
					} catch (Exception ignore) {
					}
				}

				try {
					BlobClient oldBlob = container().getBlobClient(oldPath);
					if (oldBlob != null)
						oldBlob.deleteIfExists();
				} catch (Exception ignore) {
				}

				doc.setRuta(newPath);
				doc.setNombre(nombreDestino.trim());
				if (ext != null && !ext.isBlank())
					doc.setExtension(ext.trim().toLowerCase());
			} else {
				BlobClient blob = container().getBlobClient(oldPath);
				try (var bais = new java.io.ByteArrayInputStream(bytes)) {
					blob.upload(bais, bytes.length, true);
					blob.setHttpHeaders(headers);
				}
				if (dto.getNombre() != null && !dto.getNombre().isBlank())
					doc.setNombre(dto.getNombre().trim());
				if (ext != null && !ext.isBlank())
					doc.setExtension(ext.trim().toLowerCase());
			}

			doc.setUsuarioModificacion(
					(dto.getUsuarioModificacion() == null || dto.getUsuarioModificacion().isBlank()) ? "system"
							: dto.getUsuarioModificacion().trim());
			doc.setFechaModificacion(new java.util.Date());
			documentoRepository.save(doc);

			DocumentoDTO out = documentoMapper.entityToDto(doc);
			return ok("Documento actualizado correctamente", out);

		} catch (Exception e) {
			log.error("Error actualizando documento por ruta", e);
			return err("Error actualizando documento");
		}
	}

	/**
	 * Devuelve el directorio (con slash final) de una ruta
	 * 'dir/subdir/archivo.ext'.
	 */
	private String obtenerPrefijoDeRuta(String ruta) {
		if (ruta == null)
			return "";
		int slash = Math.max(ruta.lastIndexOf('/'), ruta.lastIndexOf('\\'));
		return (slash >= 0) ? ruta.substring(0, slash + 1) : "";
	}

	/**
	 * Normaliza el nombre de archivo: quita espacios -> '_' y fuerza extensión en
	 * minúscula.
	 */
	private String buildFilename(String nombre, String extension) {
		String base = (nombre == null || nombre.isBlank()) ? "documento" : nombre.trim();
		String ext = (extension == null) ? "" : extension.trim();
		if (!ext.isEmpty() && ext.startsWith("."))
			ext = ext.substring(1);
		String filename = ext.isEmpty() ? base : base + "." + ext.toLowerCase();
		return filename.replace(" ", "_");
	}

	/** ¿El filename (parte final) de una ruta? */
	private String filenameDeRuta(String ruta) {
		if (ruta == null)
			return "";
		int slash = Math.max(ruta.lastIndexOf('/'), ruta.lastIndexOf('\\'));
		return (slash >= 0 && slash < ruta.length() - 1) ? ruta.substring(slash + 1) : ruta;
	}

	/** Soft delete en DB + intento de borrar en Azure. */
	@Transactional
	public ResponseEntity<ResponseDTO> deleteDocumento(Integer idDocumento, String usuario) {
		try {
			DocumentoEntity entity = documentoRepository.findById(idDocumento).orElse(null);
			if (entity == null) {
				return notFound("Documento no encontrado");
			}

			try {
				container().getBlobClient(entity.getRuta()).deleteIfExists();
			} catch (Exception ex) {
				log.warn("No se pudo borrar blob en Azure (continuando con DB): {}", ex.getMessage());
			}

			entity.setActivo(Boolean.FALSE);
			entity.setUsuarioModificacion(usuario == null ? "system" : usuario);
			entity.setFechaModificacion(new Date());
			documentoRepository.save(entity);

			return ok("Documento eliminado", null);

		} catch (Exception e) {
			log.error("Error eliminando documento", e);
			return err("Error eliminando documento");
		}
	}

	/** Devuelve el archivo en Base64 por ID (lee Azure). */
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> getDocumentoBase64PorId(Integer idDocumento) {
		try {
			DocumentoEntity entity = documentoRepository.findById(idDocumento).orElse(null);
			if (entity == null)
				return notFound("Documento no encontrado");

			BinaryData data = container().getBlobClient(entity.getRuta()).downloadContent();
			String b64 = Base64.getEncoder().encodeToString(data.toBytes());
			return ok("OK", Map.of("base64", b64, "ruta", entity.getRuta()));

		} catch (Exception e) {
			log.error("Error leyendo documento", e);
			return err("Error leyendo documento");
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> listarPorEmpresaConBase64(Integer idEmpresa) {
		log.info("Listar documentos (con base64) por empresaId={}", idEmpresa);

		try {
			if (idEmpresa == null) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("idEmpresa es obligatorio").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			var docs = documentoRepository.findByEmpresa_Id(idEmpresa);
			if (docs == null || docs.isEmpty()) {
				return ResponseEntity.ok(ResponseDTO.builder().success(true).message("Sin documentos")
						.code(HttpStatus.OK.value()).totalCount(0L).response(List.of()).build());
			}

			var items = new java.util.ArrayList<java.util.Map<String, Object>>(docs.size());

			for (var d : docs) {
				var row = new java.util.LinkedHashMap<String, Object>(8);
				row.put("id", d.getId());
				row.put("ruta", d.getRuta());
				row.put("nombre", d.getNombre());
				row.put("extension", d.getExtension());

				try {
					var client = container().getBlobClient(d.getRuta());
					if (client == null || !client.exists()) {
						log.warn("Blob no existe para ruta={}", d.getRuta());
						row.put("imagen", null);
						row.put("contentType", null);
						row.put("error", "Blob inexistente");
					} else {
						var data = client.downloadContent(); // BinaryData
						byte[] bytes = (data != null) ? data.toBytes() : null;

						if (bytes != null && bytes.length > 0) {
							String b64 = java.util.Base64.getEncoder().encodeToString(bytes);
							row.put("imagen", b64);

							// 1) intentar con tu helper
							String ct = guessContentType(bytes);
							// 2) si no se pudo, caer al content-type por extensión
							if (ct == null || ct.isBlank()) {
								ct = contentTypeFallback(d.getExtension());
							}
							row.put("contentType", ct);
						} else {
							row.put("imagen", null);
							row.put("contentType", null);
							row.put("error", "Blob vacío");
						}
					}
				} catch (Exception ex) {
					log.warn("No se pudo descargar blob {}: {}", d.getRuta(), ex.getMessage());
					row.put("imagen", null);
					row.put("contentType", null);
					row.put("error", ex.getMessage());
				}

				items.add(row);
			}

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message("Consulta exitosa")
					.code(HttpStatus.OK.value()).totalCount((long) items.size()).response(items).build());

		} catch (Exception e) {
			log.error("Error listando/descargando documentos por empresa {}", idEmpresa, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message("Error consultando documentos").code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	/**
	 * Fallback simple por extensión cuando guessContentType(bytes) retorna null.
	 */
	private String contentTypeFallback(String ext) {
		if (ext == null)
			return null;
		String e = ext.toLowerCase(java.util.Locale.ROOT).replace(".", "");
		return switch (e) {
		case "jpg", "jpeg" -> "image/jpeg";
		case "png" -> "image/png";
		case "gif" -> "image/gif";
		case "webp" -> "image/webp";
		case "svg" -> "image/svg+xml";
		case "pdf" -> "application/pdf";
		case "bmp" -> "image/bmp";
		case "tiff", "tif" -> "image/tiff";
		default -> null;
		};
	}

	/** Listar por persona (desde DB + Azure -> base64 por registro) */
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> listarPorPersona(Integer idPersona) {
		log.info("Listar documentos (con base64) por personaId={}", idPersona);
		try {
			var docs = documentoRepository.findByPersona_Id(idPersona);

			var items = new java.util.ArrayList<java.util.Map<String, Object>>(docs.size());

			for (var d : docs) {
				var row = new java.util.LinkedHashMap<String, Object>();
				row.put("id", d.getId());
				row.put("ruta", d.getRuta());
				row.put("nombre", d.getNombre());
				row.put("extension", d.getExtension());

				try {
					var data = container().getBlobClient(d.getRuta()).downloadContent();
					byte[] bytes = data.toBytes();
					String b64 = java.util.Base64.getEncoder().encodeToString(bytes);

					row.put("imagen", b64);
					String ct = guessContentType(bytes);
					if (ct != null)
						row.put("contentType", ct);
				} catch (Exception ex) {
					log.warn("No se pudo descargar blob {}: {}", d.getRuta(), ex.getMessage());
					row.put("imagen", null);
					row.put("contentType", null);
				}

				items.add(row);
			}

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message("Consulta exitosa")
					.code(HttpStatus.OK.value()).totalCount((long) items.size()).response(items).build());

		} catch (Exception e) {
			log.error("Error listando/descargando documentos por persona {}", idPersona, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message("Error consultando documentos").code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	/*
	 * ====================================================== Helpers ResponseDTO
	 * ======================================================
	 */

	private ResponseEntity<ResponseDTO> ok(String msg, Object body) {
		return ResponseEntity.ok(
				ResponseDTO.builder().success(true).message(msg).code(HttpStatus.OK.value()).response(body).build());
	}

	private ResponseEntity<ResponseDTO> notFound(String msg) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ResponseDTO.builder().success(false).message(msg).code(HttpStatus.NOT_FOUND.value()).build());
	}

	private ResponseEntity<ResponseDTO> bad(HttpStatus status, String msg) {
		return ResponseEntity.status(status)
				.body(ResponseDTO.builder().success(false).message(msg).code(status.value()).build());
	}

	private ResponseEntity<ResponseDTO> err(String msg) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
				.message(msg).code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> listarPorEmpresaYCategoriaCodigo(Integer idEmpresa, String categoriaCodigo) {
		log.info("Listar SOLO base64 por empresaId={} y categoriaCodigo={}", idEmpresa, categoriaCodigo);

		try {
			if (categoriaCodigo == null || categoriaCodigo.isBlank()) {
				return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
						.message("categoriaCodigo es obligatorio").code(HttpStatus.BAD_REQUEST.value()).build());
			}

			String cod = categoriaCodigo.trim().toUpperCase(java.util.Locale.ROOT);

			List<DocumentoEntity> docs;
			if (idEmpresa != null) {
				docs = documentoRepository
						.findByEmpresa_IdAndCategoriaDocumento_CodigoAndActivoTrueOrderByFechaCreacionDesc(idEmpresa,
								cod);
			} else {
				docs = documentoRepository.findByCategoriaDocumento_CodigoAndActivoTrueOrderByFechaCreacionDesc(cod);
			}

			if (docs == null || docs.isEmpty()) {
				return ResponseEntity.ok(ResponseDTO.builder().success(true).message("Sin documentos")
						.code(HttpStatus.OK.value()).totalCount(0L).response(java.util.List.of()).build());
			}

			var items = new ArrayList<DocumentoDTO>(docs.size());

			for (var d : docs) {
				try {
					var client = container().getBlobClient(d.getRuta());
					if (client != null && client.exists()) {
						var data = client.downloadContent();
						byte[] bytes = (data != null) ? data.toBytes() : null;

						if (bytes != null && bytes.length > 0) {
							String b64 = Base64.getEncoder().encodeToString(bytes);

							String nombre = (d.getNombre() != null && !d.getNombre().isBlank()) ? d.getNombre()
									: extraerNombreDeRuta(d.getRuta());

							DocumentoDTO dto = DocumentoDTO.builder().nombre(nombre).ruta(d.getRuta()).build();
							dto.setImagen(b64);

							items.add(dto);
						} else {
							log.warn("Blob vacío para ruta={}", d.getRuta());
						}
					} else {
						log.warn("Blob inexistente para ruta={}", d.getRuta());
					}
				} catch (Exception ex) {
					log.warn("No se pudo descargar blob {}: {}", d.getRuta(), ex.getMessage());
				}
			}

			return ResponseEntity.ok(ResponseDTO.builder().success(true).message("Consulta exitosa")
					.code(HttpStatus.OK.value()).totalCount((long) items.size()).response(items).build());

		} catch (Exception e) {
			log.error("Error listando (solo base64). empresaId={}, categoriaCodigo={}", idEmpresa, categoriaCodigo, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message("Error consultando documentos").code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	private String extraerNombreDeRuta(String ruta) {
		if (ruta == null || ruta.isBlank())
			return "sin_nombre";
		int slash = Math.max(ruta.lastIndexOf('/'), ruta.lastIndexOf('\\'));
		return (slash >= 0 && slash < ruta.length() - 1) ? ruta.substring(slash + 1) : ruta;
	}

}
