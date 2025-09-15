package com.aqua.plus.api.service.impl;

import java.time.Year;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.commons.dtos.DocumentoDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
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
public class DocumentoServiceImpl {

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

	/*
	 * ====================================================== Helpers Azure / rutas
	 * / headers ======================================================
	 */

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
			String nombreArchivo, String extension, String usuario, String categoriaCodigo) {
		log.info("Subiendo documento (base64): empresa={}, persona={}, nombre={}, ext={}, categoriaCodigo={}",
				idEmpresa, idPersona, nombreArchivo, extension, categoriaCodigo);

		try {
			/* ---- Validaciones básicas ---- */
			if (base64File == null || base64File.isBlank()) {
				return bad(HttpStatus.BAD_REQUEST, "Archivo (base64) vacío");
			}
			if ((idEmpresa == null && idPersona == null) || (idEmpresa != null && idPersona != null)) {
				return bad(HttpStatus.BAD_REQUEST, "Debe enviar idEmpresa o idPersona (exclusivo)");
			}

			/* ---- Categoría dinámica ---- */
			final String catCod = (categoriaCodigo == null || categoriaCodigo.isBlank()) ? "FOT"
					: categoriaCodigo.trim().toUpperCase();

			var categoria = categoriaDocumentoRepository.findFirstByCodigoAndActivoTrue(catCod).orElseGet(
					() -> (categoriaCodigo == null) ? categoriaDocumentoRepository.findById(1).orElse(null) : null);

			if (categoria == null) {
				return bad(HttpStatus.BAD_REQUEST, "La categoría de documento no existe: " + catCod);
			}

			/* ---- Decodificar base64 ---- */
			byte[] bytes;
			try {
				bytes = decodeBase64Lenient(base64File);
			} catch (IllegalArgumentException ex) {
				return bad(HttpStatus.BAD_REQUEST, "Base64 inválido");
			}
			if (bytes.length == 0) {
				return bad(HttpStatus.BAD_REQUEST, "Archivo (base64) vacío");
			}

			/* ---- Nombre/ext por defecto ---- */
			if (nombreArchivo == null || nombreArchivo.isBlank())
				nombreArchivo = "documento";
			if (extension == null || extension.isBlank()) {
				extension = extensionFromContentType(guessContentType(bytes));
			}

			/*
			 * ---- Resolver tipo para ruta (CC persona / NI empresa) + identificador ----
			 */
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

			/* ---- Construir ruta (año actual) ---- */
			String prefix = buildPrefix(idTipoRuta, identificador, null);
			String blobPath = buildBlobName(prefix, nombreArchivo, extension);

			/* ---- Subir a Azure ---- */
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
				try {
					blob.setTags(tags);
				} catch (Exception ignore) {
				}
			}

			/* ---- Persistir en BD ---- */
			DocumentoEntity entity = new DocumentoEntity();
			entity.setCategoriaDocumento(categoria);
			if (idEmpresa != null)
				entity.setEmpresa(em.getReference(EmpresaEntity.class, idEmpresa));
			if (idPersona != null)
				entity.setPersona(em.getReference(PersonaEntity.class, idPersona));
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

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> listarPorEmpresaConBase64(Integer idEmpresa) {
		log.info("Listar documentos (con base64) por empresaId={}", idEmpresa);
		try {
			var docs = documentoRepository.findByEmpresa_Id(idEmpresa);

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
			log.error("Error listando/descargando documentos por empresa {}", idEmpresa, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
					.message("Error consultando documentos").code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
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
}
