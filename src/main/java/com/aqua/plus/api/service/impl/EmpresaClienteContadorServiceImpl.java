package com.aqua.plus.api.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.configs.security.utils.JwtUtil;
import com.aqua.plus.api.service.IEmpresaClienteContadorService;
import com.aqua.plus.api.service.impl.specification.EccSpecification;
import com.aqua.plus.api.service.impl.specification.PersonaSpecification;
import com.aqua.plus.commons.dtos.AforoDTO;
import com.aqua.plus.commons.dtos.ClienteDetalleDTO;
import com.aqua.plus.commons.dtos.ContadorDTO;
import com.aqua.plus.commons.dtos.ContadorFiltroDTO;
import com.aqua.plus.commons.dtos.EccDetalleDTO;
import com.aqua.plus.commons.dtos.EmpresaClienteContadorDTO;
import com.aqua.plus.commons.dtos.PersonaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.dtos.TarifaContadorDTO;
import com.aqua.plus.commons.dtos.TipoTarifaDTO;
import com.aqua.plus.commons.dtos.TipoUsoDTO;
import com.aqua.plus.commons.entities.CorreoGeneralEntity;
import com.aqua.plus.commons.entities.EmpleadoEmpresaEntity;
import com.aqua.plus.commons.entities.EmpresaClienteContadorEntity;
import com.aqua.plus.commons.entities.PersonaEntity;
import com.aqua.plus.commons.entities.TarifaContadorEntity;
import com.aqua.plus.commons.entities.TelefonoGeneralEntity;
import com.aqua.plus.commons.entities.TipoUsoEntity;
import com.aqua.plus.commons.maps.ContadorMapper;
import com.aqua.plus.commons.maps.EmpresaClienteContadorMapper;
import com.aqua.plus.commons.maps.PersonaMapper;
import com.aqua.plus.commons.maps.TarifaContadorMapper;
import com.aqua.plus.commons.maps.TipoTarifaMapper;
import com.aqua.plus.commons.repositories.AforoContadorRepository;
import com.aqua.plus.commons.repositories.CorreoGeneralRepository;
import com.aqua.plus.commons.repositories.EmpresaClienteContadorRepository;
import com.aqua.plus.commons.repositories.RutaEmpleadoRepository;
import com.aqua.plus.commons.repositories.TarifaContadorRepository;
import com.aqua.plus.commons.repositories.TelefonoGeneralRepository;
import com.aqua.plus.commons.repositories.TipoTarifaRepository;
import com.aqua.plus.commons.utils.Constantes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmpresaClienteContadorServiceImpl implements IEmpresaClienteContadorService {

    @Value("${link.recover}")
    private String linkRecover;

    private final EmpresaClienteContadorRepository empresaClienteContadorRepository;
    private final EmpresaClienteContadorMapper empresaClienteContadorMapper;
    private final PersonaMapper personaMapper;
    private final ContadorMapper contadorMapper;
    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final NotificacionServiceImpl notificacionServiceImpl;
    private final TelefonoGeneralRepository telefonoGeneralRepository;
    private final CorreoGeneralRepository correoGeneralRepository;
    private final RutaEmpleadoRepository rutaEmpleadoRepository;
    private final JwtUtil jwtUtil;
    private final TarifaContadorRepository tarifaContadorRepository;
    private final TarifaContadorMapper tarifaContadorMapper;
    private final TipoTarifaRepository tipoTarifaRepository;
    private final TipoTarifaMapper tipoTarifaMapper;
    private final AforoContadorRepository aforoContadorRepository;

    @Override
    @Transactional
    public ResponseEntity<ResponseDTO> save(EmpresaClienteContadorDTO empresaClienteContadorDTO) {
        log.info("Creando Empresa Cliente Contador");
        try {
            Integer empresaId = empresaClienteContadorDTO.getEmpresa() != null
                    ? empresaClienteContadorDTO.getEmpresa().getId()
                    : null;
            Integer contadorId = empresaClienteContadorDTO.getContador() != null
                    ? empresaClienteContadorDTO.getContador().getId()
                    : null;
            Integer clienteId = (empresaClienteContadorDTO.getCliente() != null)
                    ? empresaClienteContadorDTO.getCliente().getId()
                    : null;

            if (empresaId == null || contadorId == null) {
                return ResponseEntity.badRequest().body(ResponseDTO.builder().success(false)
                        .message("Debe indicar empresa y contador").code(HttpStatus.BAD_REQUEST.value()).build());
            }

            boolean existe;
            if (clienteId != null) {
                existe = empresaClienteContadorRepository.existsByEmpresaIdAndClienteIdAndContadorId(empresaId,
                        clienteId, contadorId);
            } else {
                existe = empresaClienteContadorRepository.existsByEmpresaIdAndContadorId(empresaId, contadorId);
            }

            if (existe) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.builder().success(false)
                        .message(Constantes.EMCL_EXISTS).code(HttpStatus.CONFLICT.value()).build());
            }

            EmpresaClienteContadorEntity entity = empresaClienteContadorMapper.dtoToEntity(empresaClienteContadorDTO);

            entity.setFechaCreacion(new Date());
            entity.setUsuarioCreacion(empresaClienteContadorDTO.getUsuarioCreacion());
            entity.setActivo(true);

            EmpresaClienteContadorEntity saved = empresaClienteContadorRepository.save(entity);
            EmpresaClienteContadorDTO savedDTO = empresaClienteContadorMapper.entityToDto(saved);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseDTO.builder().success(true).message(Constantes.SAVED_SUCCESSFULLY)
                            .code(HttpStatus.CREATED.value()).response(savedDTO).build());

        } catch (Exception e) {
            log.error("Error creando la Empresa Cliente Contador", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
                    .message(Constantes.SAVE_ERROR).code(HttpStatus.BAD_REQUEST.value()).build());
        }
    }

    @Transactional
    public Map<String, Object> saveClient(Map<String, Object> jsonParams) {
        try {
            String jsonString = objectMapper.writeValueAsString(jsonParams);

            String sql = "SELECT * FROM public.guardar_cliente_completo(CAST(:jsonData AS jsonb))";
            MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("jsonData", jsonString);

            Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);
            Object wrappedValue = rawResult.get("guardar_cliente_completo");

            if (wrappedValue instanceof org.postgresql.util.PGobject pgObject && "jsonb".equals(pgObject.getType())) {
                String jsonValue = pgObject.getValue();
                Map<String, Object> response = objectMapper.readValue(jsonValue,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                        });

                Object statusCode = response.get("statusCode");

                String statusStr = String.valueOf(statusCode);

                if (!"200".equals(statusStr)) {
                    HttpStatus httpStatus = switch (statusStr) {
                        case "409" -> HttpStatus.CONFLICT;
                        case "404" -> HttpStatus.NOT_FOUND;
                        case "400" -> HttpStatus.BAD_REQUEST;
                        case "500" -> HttpStatus.INTERNAL_SERVER_ERROR;
                        default -> HttpStatus.BAD_REQUEST;
                    };
                    response.put("_httpStatus", httpStatus.value());
                    return response;
                }

                if ("200".equals(String.valueOf(statusCode))) {

                    String primerNombre = (String) jsonParams.get("primerNombre");
                    String segundoNombre = (String) jsonParams.get("segundoNombre");
                    String primerApellido = (String) jsonParams.get("primerApellido");
                    String segundoApellido = (String) jsonParams.get("segundoApellido");
                    String correo = (String) jsonParams.get("correo");
                    String usuario = (String) jsonParams.get("usuario");

                    String nombre = String
                            .join(" ", java.util.Optional.ofNullable(primerNombre).orElse(""),
                                    java.util.Optional.ofNullable(segundoNombre).orElse(""),
                                    java.util.Optional.ofNullable(primerApellido).orElse(""),
                                    java.util.Optional.ofNullable(segundoApellido).orElse(""))
                            .replaceAll("\\s+", " ").trim();

                    if (correo != null && !correo.isBlank() && usuario != null && !usuario.isBlank()) {
                        String tiempoLegible = notificacionServiceImpl
                                .obtenerTiempoVigenciaLegible(Constantes.TIEMPO_VIGENCIA_EXTERNO);

                        String token = jwtUtil.generateToken(usuario, Constantes.KEY_TOKEN_EXTERNO,
                                Constantes.TIEMPO_VIGENCIA_EXTERNO);

                        String encodedToken = java.net.URLEncoder.encode(token,
                                java.nio.charset.StandardCharsets.UTF_8);

                        String baseRecover = (this.linkRecover == null) ? "" : this.linkRecover;

                        String recoverLink;
                        if (baseRecover.endsWith("?") || baseRecover.endsWith("&")) {
                            recoverLink = baseRecover + encodedToken;
                        } else if (baseRecover.contains("?")) {
                            recoverLink = baseRecover + "&" + encodedToken;
                        } else {
                            recoverLink = baseRecover + "?" + encodedToken;
                        }

                        String recoverLinkMasked = recoverLink.replaceAll("([?&])[^#]*", "$1***");
                        log.info(
                                "Info data notificacion (saveClient): [nameUser={}, user={}, linkRecover={}, hours={}]",
                                nombre, usuario, recoverLinkMasked, tiempoLegible);

                        Map<String, Object> data = new HashMap<>();
                        data.put(Constantes.PARAMETRO_NAME_USER, nombre);
                        data.put(Constantes.PARAMETRO_USER, usuario);
                        data.put(Constantes.PARAMETRO_LINK_RECOVER, recoverLink);
                        data.put(Constantes.PARAMETRO_HOURS, tiempoLegible);

                        try {
                            String codigoPlantilla = Constantes.CREATE_PASSWORD;
                            notificacionServiceImpl.enviarNotificacion(correo, codigoPlantilla, data);
                            response.put("emailSent", true);
                            response.put("emailTo", correo);
                        } catch (Exception mailEx) {
                            log.error("Fallo enviando notificación a {}", correo, mailEx);
                            response.put("emailSent", false);
                            response.put("emailError", mailEx.getMessage());
                        }
                    } else if (correo == null || correo.isBlank()) {
                        response.put("notice", "El cliente no tiene un correo válido; no se envió notificación.");
                    } else {
                        response.put("notice",
                                "'usuario' es requerido para generar el token de recuperación; no se envió notificación.");
                    }
                }

                return response;
            }

            return Map.of("error", "El resultado no pudo ser procesado correctamente.");

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Error de procesamiento JSON", e);
            return Map.of("error", "Error de procesamiento JSON: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado en saveClient", e);
            return Map.of("error", "Error inesperado: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> updateClient(Map<String, Object> jsonParams) {
        try {

            String jsonString = objectMapper.writeValueAsString(jsonParams);

            String sql = "SELECT * FROM public.actualizar_cliente_basico(CAST(:jsonData AS jsonb))";

            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue("jsonData", jsonString);

            Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

            Object wrappedValue = rawResult.get("actualizar_cliente_basico");
            if (wrappedValue instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {
                String jsonValue = pgObject.getValue();
                return objectMapper.readValue(jsonValue, new TypeReference<Map<String, Object>>() {
                });
            }

            return Map.of("error", "El resultado no pudo ser procesado correctamente.");

        } catch (JsonProcessingException e) {
            log.error("Error de procesamiento JSON", e);
            return Map.of("error", "Error de procesamiento JSON: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado en updateClient", e);
            return Map.of("error", "Error inesperado: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> deleteClient(Integer idPersona) {
        try {
            String sql = "SELECT * FROM public.eliminar_cliente_completo(:idPersona)";

            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue("idPersona", idPersona);

            Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

            Object wrappedValue = rawResult.get("eliminar_cliente_completo");
            if (wrappedValue instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {
                String jsonValue = pgObject.getValue();
                return objectMapper.readValue(jsonValue, new TypeReference<Map<String, Object>>() {
                });
            }

            return Map.of("error", "El resultado no pudo ser procesado correctamente.");

        } catch (JsonProcessingException e) {
            log.error("Error de procesamiento JSON", e);
            return Map.of("error", "Error de procesamiento JSON: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado en deleteClient", e);
            return Map.of("error", "Error inesperado: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> actualizarEstado(Map<String, Object> jsonParams) {
        try {
            String jsonString = objectMapper.writeValueAsString(jsonParams);

            String sql = "SELECT * FROM public.actualizar_estado(CAST(:jsonData AS jsonb))";

            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue("jsonData", jsonString);

            Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

            Object wrappedValue = rawResult.get("actualizar_estado");
            if (wrappedValue instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {
                String jsonValue = pgObject.getValue();
                return objectMapper.readValue(jsonValue, new TypeReference<Map<String, Object>>() {
                });
            }

            return Map.of("error", "El resultado no pudo ser procesado correctamente.");

        } catch (JsonProcessingException e) {
            log.error("Error de procesamiento JSON", e);
            return Map.of("error", "Error de procesamiento JSON: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado en actualizarEstadoPersona", e);
            return Map.of("error", "Error inesperado: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDTO> update(EmpresaClienteContadorDTO empresaClienteContadorDTO) {
        log.info("Actualizando Empresa Cliente Contador");
        try {
            if (empresaClienteContadorDTO.getId() == null
                    || !empresaClienteContadorRepository.existsById(empresaClienteContadorDTO.getId())) {
                throw new IllegalArgumentException(Constantes.ECC_NOT_FOUND);
            }
            EmpresaClienteContadorEntity entity = empresaClienteContadorRepository
                    .findById(empresaClienteContadorDTO.getId()).orElseThrow();
            empresaClienteContadorMapper.updateEntityFromDto(empresaClienteContadorDTO, entity);
            entity.setFechaModificacion(new Date());
            entity.setUsuarioModificacion(empresaClienteContadorDTO.getUsuarioModificacion());

            EmpresaClienteContadorEntity updated = empresaClienteContadorRepository.save(entity);
            EmpresaClienteContadorDTO updatedDTO = empresaClienteContadorMapper.entityToDto(updated);

            ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.UPDATED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value()).response(updatedDTO).build();

            return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
        } catch (Exception e) {
            log.error("Error actualizando la Empresa Cliente Contador", e);
            ResponseDTO errorResponse = ResponseDTO.builder().success(false).message(Constantes.UPDATE_ERROR)
                    .code(HttpStatus.BAD_REQUEST.value()).build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findByEmpresaId(Integer idEmpresa) {
        log.info("Buscar Empresa Cliente Contador por id de empresa: {}", idEmpresa);
        try {
            var list = empresaClienteContadorRepository.findByEmpresa_Id(idEmpresa);
            var dtoList = empresaClienteContadorMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value()).response(dtoList).build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al consultar por id de empresa: {}", idEmpresa, e);
            ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }

    private record ClienteFilters(
            String nombre, String cedula, String codigo,
            String departamento, String ciudad, String corregimiento,
            String telefono, String correo, String tipoDocumentoNombre,
            String direccion, Boolean estado, Integer nuid) {
    }


    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findClientesByEmpresaId(
            Integer idEmpresa, Pageable pageable,
            String nombre, String cedula, String codigo,
            String departamento, String ciudad, String corregimiento,
            String telefono, String correo, String tipoDocumentoNombre,
            String direccion, Boolean estado, Integer nuid) {

        if (idEmpresa == null) {
            return ResponseEntity.badRequest().body(ResponseDTO.builder()
                    .success(false).message("Parámetro requerido: idEmpresa")
                    .code(HttpStatus.BAD_REQUEST.value()).build());
        }
        Objects.requireNonNull(pageable, "pageable must not be null");

        log.info("findClientesByEmpresaId empresa={} page={}/{} filtros=[nombre={}, cedula={}, estado={}, nuid={}]",
                idEmpresa, pageable.getPageNumber(), pageable.getPageSize(), nombre, cedula, estado, nuid);

        try {
            var filters = new ClienteFilters(nombre, cedula, codigo, departamento, ciudad,
                    corregimiento, telefono, correo, tipoDocumentoNombre, direccion, estado, nuid);

            var spec = buildClienteSpec(idEmpresa, filters);
            var page = empresaClienteContadorRepository.findAll(spec, pageable);
            var rows = buildRows(page.getContent());

            return buildPageResponse(rows, page);

        } catch (Exception e) {
            log.error("Error consultando clientes empresa={}", idEmpresa, e);
            return ResponseEntity.internalServerError().body(ResponseDTO.builder()
                    .success(false).message("Error interno al consultar clientes")
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // Specification — DISTINCT elimina los duplicados que antes se deduplicaban
    // manualmente con el chunking. Si aparece la advertencia HHH90003004 en los logs
    // revisa si algún Specification hace JOIN FETCH sobre una colección @OneToMany.
    // ═══════════════════════════════════════════════════════════════════════════════
    private Specification<EmpresaClienteContadorEntity> buildClienteSpec(
            Integer idEmpresa, ClienteFilters f) {

        Specification<EmpresaClienteContadorEntity> distinct =
                (root, query, cb) -> {
                    assert query != null;
                    Subquery<Integer> sub = query.subquery(Integer.class);
                    Root<EmpresaClienteContadorEntity> subRoot = sub.from(EmpresaClienteContadorEntity.class);

                    sub.select(cb.min(subRoot.get("id")))
                            .where(cb.and(
                                    cb.equal(subRoot.get("cliente"), root.get("cliente")),
                                    cb.isTrue(subRoot.get("activo"))   // ← esto faltaba
                            ));

                    return cb.and(
                            cb.isTrue(root.get("activo")),
                            cb.equal(root.get("id"), sub)
                    );
                };

        return Specification.allOf(
                distinct,
                PersonaSpecification.empresaId(idEmpresa),
                PersonaSpecification.clienteNombreLike(f.nombre()),
                PersonaSpecification.clienteCedulaIgual(f.cedula()),
                PersonaSpecification.clienteCodigoIgual(f.codigo()),
                PersonaSpecification.direccionDepartamentoNombreLike(f.departamento()),
                PersonaSpecification.direccionCiudadNombreLike(f.ciudad()),
                PersonaSpecification.direccionCorregimientoNombreLike(f.corregimiento()),
                PersonaSpecification.clienteTelefonoLike(f.telefono()),
                PersonaSpecification.clienteCorreoLike(f.correo()),
                PersonaSpecification.clienteTipoDocumentoNombreLike(f.tipoDocumentoNombre()),
                PersonaSpecification.direccionDescripcionLike(f.direccion()),
                PersonaSpecification.clienteEstado(f.estado()),
                PersonaSpecification.contadorNuid(f.nuid())
        );
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // Construcción de filas — Spring Data ya entrega la página correcta
    // ═══════════════════════════════════════════════════════════════════════════════
    private List<Map<String, Object>> buildRows(List<EmpresaClienteContadorEntity> content) {
        if (content.isEmpty()) return List.of();

        List<Integer> personaIds = content.stream()
                .map(EmpresaClienteContadorEntity::getCliente)
                .filter(Objects::nonNull)
                .map(PersonaEntity::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Integer, String> correos = fetchCorreos(personaIds);
        Map<Integer, String> telefonos = fetchTelefonos(personaIds);

        return content.stream()
                .map(ecc -> mapRow(ecc, correos, telefonos))
                .sorted(Comparator
                        .comparing((Map<String, Object> m) -> Boolean.FALSE.equals(m.get("activo")))
                        .thenComparing(m -> (String) m.getOrDefault("nombreCompleto", ""),
                                String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private Map<Integer, String> fetchCorreos(List<Integer> personaIds) {
        if (personaIds.isEmpty()) return Map.of();
        return correoGeneralRepository.findLatestByPersonaIds(personaIds).stream()
                .filter(cg -> cg.getPersona() != null)
                .collect(Collectors.toMap(
                        cg -> cg.getPersona().getId(),
                        CorreoGeneralEntity::getCorreo,
                        (a, b) -> a));
    }

    private Map<Integer, String> fetchTelefonos(List<Integer> personaIds) {
        if (personaIds.isEmpty()) return Map.of();
        return telefonoGeneralRepository.findLatestByPersonaIds(personaIds).stream()
                .filter(tg -> tg.getPersona() != null)
                .collect(Collectors.toMap(
                        tg -> tg.getPersona().getId(),
                        TelefonoGeneralEntity::getNumero,
                        (a, b) -> a));
    }

    private Map<String, Object> mapRow(EmpresaClienteContadorEntity ecc,
                                       Map<Integer, String> correos, Map<Integer, String> telefonos) {

        var p = ecc.getCliente();
        Integer personaId = p != null ? p.getId() : null;

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("empresaClienteContadorId", ecc.getId());
        row.put("id", personaId);
        row.put("numeroCedula", p != null ? p.getNumeroCedula() : null);
        row.put("nombreCompleto", buildNombreCompleto(p));
        row.put("codigo", p != null ? p.getCodigo() : null);
        row.put("activo", p != null ? p.getActivo() : null);
        row.put("tipoDocumentoId", p != null && p.getTipoDocumento() != null ? p.getTipoDocumento().getId() : null);
        row.put("tipoDocumentoNombre", p != null && p.getTipoDocumento() != null ? p.getTipoDocumento().getNombre() : null);
        putDireccionEntries(row, p);
        row.put("correo", personaId != null ? correos.get(personaId) : null);
        row.put("telefono", personaId != null ? telefonos.get(personaId) : null);
        return row;
    }

    // Reemplaza PersonaEntity con el tipo real que retorna ecc.getCliente()
    private String buildNombreCompleto(PersonaEntity p) {
        if (p == null) return null;
        return Stream.of(p.getNombre(), p.getSegundoNombre(), p.getApellido(), p.getSegundoApellido())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "));
    }

    // Reemplaza PersonaEntity con el tipo real que retorna ecc.getCliente()
    private void putDireccionEntries(Map<String, Object> row, PersonaEntity p) {
        if (p == null || p.getDireccion() == null) {
            Stream.of("direccionId", "direccionDescripcion", "departamentoNombre",
                            "departamentoId", "ciudadNombre", "ciudadId", "corregimientoNombre", "corregimientoId")
                    .forEach(k -> row.put(k, null));
            return;
        }
        var d = p.getDireccion();
        row.put("direccionId", d.getId());
        row.put("direccionDescripcion", d.getDescripcion());
        row.put("departamentoNombre", d.getDepartamento() != null ? d.getDepartamento().getNombre() : null);
        row.put("departamentoId", d.getDepartamento() != null ? d.getDepartamento().getId() : null);
        row.put("ciudadNombre", d.getCiudad() != null ? d.getCiudad().getNombre() : null);
        row.put("ciudadId", d.getCiudad() != null ? d.getCiudad().getId() : null);
        row.put("corregimientoNombre", d.getCorregimiento() != null ? d.getCorregimiento().getNombre() : null);
        row.put("corregimientoId", d.getCorregimiento() != null ? d.getCorregimiento().getId() : null);
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // Respuesta — usa directamente los metadatos del Page de Spring Data
    // ═══════════════════════════════════════════════════════════════════════════════
    private ResponseEntity<ResponseDTO> buildPageResponse(
            List<Map<String, Object>> rows, Page<EmpresaClienteContadorEntity> page) {

        if (rows.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.builder()
                    .success(false).message("No se encontraron clientes para los filtros dados")
                    .code(HttpStatus.NOT_FOUND.value()).response(List.of())
                    .totalCount(page.getTotalElements())
                    .pageSize(page.getSize())
                    .currentPage(page.getNumber())
                    .totalPages(page.getTotalPages())
                    .build());
        }

        return ResponseEntity.ok(ResponseDTO.builder()
                .success(true).message("Consulta exitosa")
                .code(HttpStatus.OK.value()).response(rows)
                .totalCount(page.getTotalElements())
                .pageSize(page.getSize())
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findByEmpresaIdResponseId(Integer idEmpresa) {
        log.info("Buscar Empresa Cliente Contador por id de empresa: {}", idEmpresa);
        try {
            var list = empresaClienteContadorRepository.findByEmpresa_Id(idEmpresa);

            if (list.isEmpty()) {
                ResponseDTO responseDTO = ResponseDTO.builder().success(false)
                        .message("No se encontraron registros para la empresa con id: " + idEmpresa)
                        .code(HttpStatus.NOT_FOUND.value()).build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }

            List<Map<String, Object>> idList = list.stream().map(e -> Map.<String, Object>of("id", e.getId())).toList();

            ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value()).response(idList).build();

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al consultar por id de empresa: {}", idEmpresa, e);
            ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findById(Integer id) {
        log.info("Buscar Empresa Cliente Contador por id: {}", id);

        try {
            var opt = empresaClienteContadorRepository.findById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseDTO.builder().success(false)
                                .message("No se encontró Empresa-Cliente-Contador con id " + id)
                                .code(HttpStatus.NOT_FOUND.value()).response(null).build());
            }

            var ecc = opt.get();

            // ================== EMPRESA ID ==================
            Integer empresaId = (ecc.getEmpresa() != null ? ecc.getEmpresa().getId() : null);

            // ================== CLIENTE / PERSONA ==================
            var personaEntity = ecc.getCliente();
            Integer personaId = (personaEntity != null ? personaEntity.getId() : null);
            PersonaDTO personaDTO = (personaEntity != null ? personaMapper.entityToDto(personaEntity) : null);

            // ================== CONTADORES CON ID DE RELACIÓN ==================
            List<ContadorDTO> contadoresDTO = Collections.emptyList();
            if (personaId != null) {
                List<EmpresaClienteContadorEntity> eccsPersona = empresaClienteContadorRepository
                        .findAllByCliente_Id(personaId);

                contadoresDTO = eccsPersona.stream()
                        .filter(eccRel -> eccRel.getContador() != null && Boolean.TRUE.equals(eccRel.getActivo()))
                        .map(eccRel -> {
                            ContadorDTO dto = contadorMapper.entityToDto(eccRel.getContador());
                            dto.setIdEmpresaClienteContador(eccRel.getId());

                            // Tipo uso
                            TipoUsoEntity tipoUso = eccRel.getContador().getTipoUso();
                            if (tipoUso != null) {
                                dto.setTipoUso(
                                        TipoUsoDTO.builder().id(tipoUso.getId()).nombre(tipoUso.getNombre()).build());
                            }

                            // Aforos
                            List<AforoDTO> aforos = aforoContadorRepository
                                    .findByContadorConAforo(eccRel.getContador().getId()).stream()
                                    .filter(ac -> ac.getAforo() != null)
                                    .map(ac -> AforoDTO.builder().id(ac.getAforo().getId()).idAforoContador(ac.getId())
                                            .nombre(ac.getAforo().getNombre()).tarifaBase(ac.getAforo().getTarifaBase())
                                            .build())
                                    .toList();
                            dto.setAforoContador(aforos);

                            // Empleado asignado a este contador
                            rutaEmpleadoRepository.findByEmpresaClienteContador_Id(eccRel.getId()).ifPresent(ruta -> {
                                EmpleadoEmpresaEntity emp = ruta.getEmpleadoEmpresa();
                                if (emp != null) {
                                    dto.setEmpleadoEmpresaId(emp.getId());
                                    dto.setEmpleadoNombre(resolveEmpleadoNombreSeguro(emp));
                                }
                            });

                            // Tarifas de este contador
                            List<TarifaContadorEntity> tarifasContador = Collections.emptyList();
                            List<TarifaContadorDTO> tarifasContadorDTO = Collections.emptyList();
                            try {
                                tarifasContador = tarifaContadorRepository
                                        .findByEmpresaClienteContador_Id(eccRel.getId());
                                tarifasContadorDTO = (tarifasContador == null || tarifasContador.isEmpty())
                                        ? Collections.emptyList()
                                        : tarifasContador.stream().map(tarifaContadorMapper::entityToDto).toList();
                            } catch (Exception ex) {
                                log.warn("No se pudieron cargar tarifas para eccId {}: {}", eccRel.getId(),
                                        ex.getMessage());
                            }
                            dto.setTarifasContadores(tarifasContadorDTO);

                            // Tipos tarifa faltantes para este contador
                            List<TipoTarifaDTO> tiposFaltantesDTO = Collections.emptyList();
                            if (empresaId != null) {
                                final List<TarifaContadorEntity> tarifasRef = tarifasContador;
                                Set<Integer> tiposUsados = tarifasRef.stream()
                                        .filter(t -> t.getTipoTarifa() != null && t.getTipoTarifa().getId() != null)
                                        .map(t -> t.getTipoTarifa().getId()).collect(Collectors.toSet());

                                tiposFaltantesDTO = tipoTarifaRepository.findByEmpresa_Id(empresaId).stream()
                                        .filter(tt -> tt.getId() != null && !tiposUsados.contains(tt.getId()))
                                        .map(tipoTarifaMapper::entityToDto).toList();
                            }
                            dto.setTiposTarifaFaltantes(tiposFaltantesDTO);

                            return dto;
                        }).toList();
            }

            // ================== CONTACTO (CORREO / TELÉFONO) ==================
            String correoVal = (personaId != null)
                    ? correoGeneralRepository.findTop1ByPersonaIdAndActivoTrueOrderByIdDesc(personaId)
                    .map(CorreoGeneralEntity::getCorreo).orElse(null)
                    : null;

            String telVal = (personaId != null)
                    ? telefonoGeneralRepository.findTop1ByPersonaIdAndActivoTrueOrderByIdDesc(personaId)
                    .map(TelefonoGeneralEntity::getNumero).orElse(null)
                    : null;

            // ================== PAYLOAD ==================
            EccDetalleDTO payload = EccDetalleDTO.builder().persona(personaDTO).contadores(contadoresDTO)
                    .correo(correoVal).telefono(telVal).build();

            return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value()).response(payload).build());

        } catch (Exception e) {
            log.error("Error al buscar Empresa Cliente Contador por id: {}", id, e);

            Map<String, Object> errorPayload = new HashMap<>();
            errorPayload.put("exception", e.getClass().getSimpleName());
            errorPayload.put("errorMessage", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
                            .code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(errorPayload).build());
        }
    }

    private String resolveEmpleadoNombreSeguro(EmpleadoEmpresaEntity ee) {
        if (ee == null)
            return null;
        try {
            Object emp = ee.getPersona();
            if (emp instanceof PersonaEntity persona) {
                return nombreCompleto(persona.getNombre(), persona.getSegundoNombre(), persona.getApellido(),
                        persona.getSegundoApellido());
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private String nombreCompleto(String n, String sn, String a, String sa) {
        StringBuilder sb = new StringBuilder();
        if (n != null && !n.isBlank())
            sb.append(n).append(' ');
        if (sn != null && !sn.isBlank())
            sb.append(sn).append(' ');
        if (a != null && !a.isBlank())
            sb.append(a).append(' ');
        if (sa != null && !sa.isBlank())
            sb.append(sa);
        return sb.toString().trim().replaceAll("\\s+", " ");
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findAll() {
        log.info("Listar todos las Empresa Cliente Contador");
        try {
            var list = empresaClienteContadorRepository.findAll();
            var dtoList = empresaClienteContadorMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value()).response(dtoList).build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar las Empresa Cliente Contador", e);
            ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.CONSULTING_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(null).build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDTO> deleteById(Integer id) {
        log.info("Inicio método para eliminar Empresa Cliente Contador por id: {}", id);
        try {
            if (!empresaClienteContadorRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value()).build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            empresaClienteContadorRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder().success(true).message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value()).build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar Empresa Cliente Contador con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder().success(false).message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> clientesEmpresaMes(Integer idEmpresa, Integer anio, Integer mes, String rangoPor,
                                                  Boolean exclusivo) {
        try {
            StringBuilder sql = new StringBuilder("SELECT public.fn_clientes_empresa_mes(:idEmpresa, :anio, :mes");

            MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idEmpresa", idEmpresa)
                    .addValue("anio", anio).addValue("mes", mes);

            if (rangoPor != null && exclusivo != null) {
                sql.append(", :rangoPor, :exclusivo)");
                parameters.addValue("rangoPor", rangoPor);
                parameters.addValue("exclusivo", exclusivo);
            } else if (rangoPor != null) {
                sql.append(", :rangoPor)");
                parameters.addValue("rangoPor", rangoPor);
            } else if (exclusivo != null) {
                sql.append(", :rangoPor, :exclusivo)");
                parameters.addValue("rangoPor", "emision");
                parameters.addValue("exclusivo", exclusivo);
            } else {
                sql.append(")");
            }

            Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql.toString(), parameters);

            Object wrappedValue = rawResult.get("fn_clientes_empresa_mes");

            if (wrappedValue instanceof org.postgresql.util.PGobject pg
                    && ("jsonb".equals(pg.getType()) || "json".equals(pg.getType()))) {
                String jsonValue = pg.getValue();
                return objectMapper.readValue(jsonValue,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                        });
            }
            if (wrappedValue instanceof String s) {
                return objectMapper.readValue(s,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                        });
            }

            return Map.of(Constantes.ERROR_KEY, Constantes.RESULT_COULD_NOT_PROCESSED);

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            e.printStackTrace();
            return Collections.singletonMap(Constantes.ERROR_KEY, Constantes.PROCCESSING_ERROR + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.singletonMap(Constantes.ERROR_KEY, Constantes.UNEXPECTED_ERROR + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findByEmpresaAndPersona(Integer idEmpresa, Integer idPersona) {
        log.info("Buscar EmpresaClienteContador por empresa={} y persona={}", idEmpresa, idPersona);
        try {
            if (idEmpresa == null || idPersona == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDTO.builder().success(false)
                        .message("idEmpresa e idPersona son requeridos").code(HttpStatus.BAD_REQUEST.value()).build());
            }

            List<EmpresaClienteContadorEntity> entities = empresaClienteContadorRepository
                    .findByEmpresaIdAndClienteId(idEmpresa, idPersona);

            if (entities.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseDTO.builder().success(false)
                                .message("No se encontraron registros para la empresa/persona indicadas")
                                .code(HttpStatus.NOT_FOUND.value()).response(List.of()).build());
            }

            List<EmpresaClienteContadorDTO> dtos = entities.stream().map(empresaClienteContadorMapper::entityToDto)
                    .toList();

            return ResponseEntity.ok(ResponseDTO.builder().success(true).message("Consulta exitosa")
                    .code(HttpStatus.OK.value()).response(dtos).build());

        } catch (Exception e) {
            log.error("Error consultando EmpresaClienteContador por empresa/persona: {}, {}", idEmpresa, idPersona, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().success(false)
                    .message("Error consultando").code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findContadoresByEccId(Integer eccId, ContadorFiltroDTO filtro,
                                                             Pageable pageable) {

        log.info("Buscar contadores para ECC id: {}, filtro: {}", eccId, filtro);

        try {
            empresaClienteContadorRepository.flush();
            Optional<Integer> optPersonaId = empresaClienteContadorRepository.findPersonaIdByEccId(eccId);
            if (optPersonaId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseDTO.builder().success(false)
                                .message("No se encontró EmpresaClienteContador con id " + eccId)
                                .code(HttpStatus.NOT_FOUND.value()).response(null).build());
            }

            Integer personaId = optPersonaId.get();
            Integer empresaId = empresaClienteContadorRepository.findEmpresaIdByEccId(eccId).orElse(null);

            log.info("ECC {} → personaId resuelto: {}, empresaId: {}", eccId, personaId, empresaId);

            if (personaId == null) {
                return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
                        .code(HttpStatus.OK.value()).response(Collections.emptyList()).build());
            }

            Specification<EmpresaClienteContadorEntity> spec = EccSpecification.build(personaId, filtro);

            Page<EmpresaClienteContadorEntity> pageResult = empresaClienteContadorRepository.findAll(spec, pageable);

            log.info("Total contadores encontrados para personaId {}: {}", personaId, pageResult.getTotalElements());

            final Integer empId = empresaId;
            List<ContadorDTO> contadoresDTO = pageResult.getContent().stream()
                    .map(eccRel -> mapToContadorDTO(eccRel, empId)).toList();

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("contadores", contadoresDTO);
            payload.put("totalElements", pageResult.getTotalElements());
            payload.put("totalPages", pageResult.getTotalPages());
            payload.put("pageNumber", pageResult.getNumber());
            payload.put("pageSize", pageResult.getSize());

            return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value()).response(payload).build());

        } catch (Exception e) {
            log.error("Error al buscar contadores para ECC id: {}", eccId, e);
            return buildErrorResponse(e);
        }
    }

    private ContadorDTO mapToContadorDTO(EmpresaClienteContadorEntity eccRel, Integer empresaId) {

        ContadorDTO dto = contadorMapper.entityToDto(eccRel.getContador());
        dto.setIdEmpresaClienteContador(eccRel.getId());

        TipoUsoEntity tipoUso = eccRel.getContador().getTipoUso();
        if (tipoUso != null) {
            dto.setTipoUso(TipoUsoDTO.builder().id(tipoUso.getId()).nombre(tipoUso.getNombre()).build());
        }

        List<AforoDTO> aforos = aforoContadorRepository.findByContadorConAforo(eccRel.getContador().getId()).stream()
                .filter(ac -> ac.getAforo() != null)
                .map(ac -> AforoDTO.builder().id(ac.getAforo().getId()).idAforoContador(ac.getId())
                        .nombre(ac.getAforo().getNombre()).tarifaBase(ac.getAforo().getTarifaBase()).build())
                .toList();
        dto.setAforoContador(aforos);

        rutaEmpleadoRepository.findByEmpresaClienteContador_Id(eccRel.getId()).ifPresent(ruta -> {
            EmpleadoEmpresaEntity emp = ruta.getEmpleadoEmpresa();
            if (emp != null) {
                dto.setEmpleadoEmpresaId(emp.getId());
                dto.setEmpleadoNombre(resolveEmpleadoNombreSeguro(emp));
            }
        });

        List<TarifaContadorEntity> tarifasContador = Collections.emptyList();
        List<TarifaContadorDTO> tarifasContadorDTO = Collections.emptyList();
        try {
            tarifasContador = tarifaContadorRepository.findByEmpresaClienteContador_Id(eccRel.getId());
            tarifasContadorDTO = tarifasContador.isEmpty() ? Collections.emptyList()
                    : tarifasContador.stream().map(tarifaContadorMapper::entityToDto).toList();
        } catch (Exception ex) {
            log.warn("No se pudieron cargar tarifas para eccId {}: {}", eccRel.getId(), ex.getMessage());
        }
        dto.setTarifasContadores(tarifasContadorDTO);

        if (empresaId != null) {
            final List<TarifaContadorEntity> tarifasRef = tarifasContador;
            Set<Integer> tiposUsados = tarifasRef.stream()
                    .filter(t -> t.getTipoTarifa() != null && t.getTipoTarifa().getId() != null)
                    .map(t -> t.getTipoTarifa().getId()).collect(Collectors.toSet());

            List<TipoTarifaDTO> tiposFaltantesDTO = tipoTarifaRepository.findByEmpresa_Id(empresaId).stream()
                    .filter(tt -> tt.getId() != null && !tiposUsados.contains(tt.getId()))
                    .map(tipoTarifaMapper::entityToDto).toList();

            dto.setTiposTarifaFaltantes(tiposFaltantesDTO);
        }

        return dto;
    }

    private ResponseEntity<ResponseDTO> buildErrorResponse(Exception e) {
        Map<String, Object> errorPayload = new HashMap<>();
        errorPayload.put("exception", e.getClass().getSimpleName());
        errorPayload.put("errorMessage", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseDTO.builder().success(false).message(Constantes.ERROR_QUERY_RECORD_BY_ID)
                        .code(HttpStatus.INTERNAL_SERVER_ERROR.value()).response(errorPayload).build());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findClienteByEccId(Integer eccId) {
        log.info("Buscar cliente por ECC id: {}", eccId);

        try {
            var opt = empresaClienteContadorRepository.findById(eccId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseDTO.builder().success(false)
                                .message("No se encontró EmpresaClienteContador con id " + eccId)
                                .code(HttpStatus.NOT_FOUND.value()).response(null).build());
            }

            var ecc = opt.get();
            var persona = ecc.getCliente();
            Integer personaId = persona != null ? persona.getId() : null;

            PersonaDTO personaDTO = persona != null ? personaMapper.entityToDto(persona) : null;

            String correo = personaId != null
                    ? correoGeneralRepository.findTop1ByPersonaIdAndActivoTrueOrderByIdDesc(personaId)
                    .map(CorreoGeneralEntity::getCorreo).orElse(null)
                    : null;

            String telefono = personaId != null
                    ? telefonoGeneralRepository.findTop1ByPersonaIdAndActivoTrueOrderByIdDesc(personaId)
                    .map(TelefonoGeneralEntity::getNumero).orElse(null)
                    : null;

            var payload = ClienteDetalleDTO.builder().persona(personaDTO).correo(correo).telefono(telefono).build();

            return ResponseEntity.ok(ResponseDTO.builder().success(true).message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value()).response(payload).build());

        } catch (Exception e) {
            log.error("Error al buscar cliente por ECC id: {}", eccId, e);
            return buildErrorResponse(e);
        }
    }

}
