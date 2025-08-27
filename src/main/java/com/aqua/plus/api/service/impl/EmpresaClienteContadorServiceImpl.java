package com.aqua.plus.api.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IEmpresaClienteContadorService;
import com.aqua.plus.commons.dtos.ContadorDTO;
import com.aqua.plus.commons.dtos.EmpresaClienteContadorDTO;
import com.aqua.plus.commons.dtos.PersonaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.ContadorEntity;
import com.aqua.plus.commons.entities.EmpresaClienteContadorEntity;
import com.aqua.plus.commons.entities.PersonaEntity;
import com.aqua.plus.commons.maps.ContadorMapper;
import com.aqua.plus.commons.maps.EmpresaClienteContadorMapper;
import com.aqua.plus.commons.maps.PersonaMapper;
import com.aqua.plus.commons.repositories.EmpresaClienteContadorRepository;
import com.aqua.plus.commons.utils.Constantes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmpresaClienteContadorServiceImpl implements IEmpresaClienteContadorService{
	
	@Value("${link.recover}")
	private String linkRecover;
	
	private final EmpresaClienteContadorRepository empresaClienteContadorRepository;
	private final EmpresaClienteContadorMapper empresaClienteContadorMapper;
    private final ObjectMapper objectMapper;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final NotificacionServiceImpl notificacionServiceImpl;
	private final PersonaMapper personaMapper;
	private final ContadorMapper contadorMapper;
	
	
	@Override
	@Transactional
	public ResponseEntity<ResponseDTO> save(EmpresaClienteContadorDTO empresaClienteContadorDTO) {
	    log.info("Creando Empresa Cliente Contador");
	    try {
	        boolean existe = empresaClienteContadorRepository.existsByEmpresaIdAndClienteIdAndContadorId(
	                empresaClienteContadorDTO.getEmpresa().getId(),
	                empresaClienteContadorDTO.getCliente().getId(),
	                empresaClienteContadorDTO.getContador().getId()
	        );

	        if (existe) {
	            return ResponseEntity.status(HttpStatus.CONFLICT).body(
	                    ResponseDTO.builder()
	                            .success(false)
	                            .message(Constantes.EMCL_EXISTS)
	                            .code(HttpStatus.CONFLICT.value())
	                            .build()
	            );
	        }
	        EmpresaClienteContadorEntity entity = empresaClienteContadorMapper.dtoToEntity(empresaClienteContadorDTO);
	        entity.setFechaCreacion(new Date());
	        entity.setUsuarioCreacion(empresaClienteContadorDTO.getUsuarioCreacion());
	        entity.setActivo(true);

	        EmpresaClienteContadorEntity saved = empresaClienteContadorRepository.save(entity);
	        EmpresaClienteContadorDTO savedDTO = empresaClienteContadorMapper.entityToDto(saved);

	        return ResponseEntity.status(HttpStatus.CREATED).body(
	                ResponseDTO.builder()
	                        .success(true)
	                        .message(Constantes.SAVED_SUCCESSFULLY)
	                        .code(HttpStatus.CREATED.value())
	                        .response(savedDTO)
	                        .build()
	        );

	    } catch (Exception e) {
	        log.error("Error creando la Empresa Cliente Contador", e);
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
	                ResponseDTO.builder()
	                        .success(false)
	                        .message(Constantes.SAVE_ERROR)
	                        .code(HttpStatus.BAD_REQUEST.value())
	                        .build()
	        );
	    }
	}
	
    @Transactional
	public Map<String, Object> saveClient(Map<String, Object> jsonParams) {
		try {
			String jsonString = objectMapper.writeValueAsString(jsonParams);

			String sql = "SELECT * FROM public.guardar_cliente_completo(CAST(:jsonData AS jsonb))";

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("jsonData", jsonString);

			Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

			Object wrappedValue = rawResult.get("guardar_cliente_completo");
			
			if (wrappedValue instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {
	            String jsonValue = pgObject.getValue();
	            Map<String, Object> response = objectMapper.readValue(jsonValue, new TypeReference<>() {});

	            Object statusCode = response.get("statusCode");
	            if ("200".equals(String.valueOf(statusCode))) {
	            	String primerNombre = (String) jsonParams.get("primer_nombre");
	            	String segundoNombre = (String) jsonParams.get("segundo_nombre");
	            	String primerApellido = (String) jsonParams.get("primer_apellido");
	            	String segundoApellido = (String) jsonParams.get("segundo_apellido");
	            	
	                String correo = (String) jsonParams.get("correo");
	                String nombre = String.join(" ",
	                	    Optional.ofNullable(primerNombre).orElse(""),
	                	    Optional.ofNullable(segundoNombre).orElse(""),
	                	    Optional.ofNullable(primerApellido).orElse(""),
	                	    Optional.ofNullable(segundoApellido).orElse("")
	                	).replaceAll("\\s+", " ").trim();
	                String usuario = (String) jsonParams.get("usuario");
	                String tiempoLegible = notificacionServiceImpl.obtenerTiempoVigenciaLegible(Constantes.TIEMPO_VIGENCIA_EXTERNO);

	                if (correo != null && !correo.isBlank()) {
	                    Map<String, Object> data = new HashMap<>();
	                    data.put(Constantes.PARAMETRO_NAME_USER, nombre);
	                    data.put(Constantes.PARAMETRO_USER, usuario);
	                    data.put(Constantes.PARAMETRO_LINK_RECOVER, this.linkRecover);
	                    data.put(Constantes.PARAMETRO_HOURS, tiempoLegible);
	                    
	                    log.info("Info data notificacion {}", data);

	                    String codigoPlantilla = Constantes.CREATE_PASSWORD;

	                    notificacionServiceImpl.enviarNotificacion(correo, codigoPlantilla, data);
	                }
	            }

	            return response;
	        }

			return Map.of("error", "El resultado no pudo ser procesado correctamente.");

		} catch (JsonProcessingException e) {
			log.error("Error de procesamiento JSON", e);
			return Map.of("error", "Error de procesamiento JSON: " + e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado en actualizarEmpleado", e);
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
                return objectMapper.readValue(jsonValue, new TypeReference<Map<String, Object>>() {});
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
    			return objectMapper.readValue(jsonValue, new TypeReference<Map<String, Object>>() {});
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
            if (empresaClienteContadorDTO.getId() == null || !empresaClienteContadorRepository.existsById(empresaClienteContadorDTO.getId())) {
                throw new IllegalArgumentException(Constantes.ECC_NOT_FOUND);
            }
            EmpresaClienteContadorEntity entity = empresaClienteContadorRepository.findById(empresaClienteContadorDTO.getId()).orElseThrow();
            empresaClienteContadorMapper.updateEntityFromDto(empresaClienteContadorDTO, entity); 
            entity.setFechaModificacion(new Date());
            entity.setUsuarioModificacion(empresaClienteContadorDTO.getUsuarioModificacion());

            EmpresaClienteContadorEntity updated = empresaClienteContadorRepository.save(entity);
            EmpresaClienteContadorDTO updatedDTO = empresaClienteContadorMapper.entityToDto(updated);

            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.UPDATED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(updatedDTO)
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
        } catch (Exception e) {
            log.error("Error actualizando la Empresa Cliente Contador", e);
            ResponseDTO errorResponse = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.UPDATE_ERROR)
                    .code(HttpStatus.BAD_REQUEST.value())
                    .build();
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
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al consultar por id de empresa: {}", idEmpresa, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.CONSULTING_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findClientesByEmpresaId(Integer idEmpresa, Pageable pageable) {
    	log.info("Ingresa método buscar Cliente por id de empresa: {}", idEmpresa);
        try {
            List<PersonaEntity> clientes;
            if (pageable.isPaged()) {
                Page<PersonaEntity> page = empresaClienteContadorRepository.findAllClientesByEmpresaIdPaged(idEmpresa, pageable);
                clientes = page.getContent();
            } else {
                clientes = empresaClienteContadorRepository.findAllClientesByEmpresaId(idEmpresa);
            }

            List<PersonaDTO> dtoList = personaMapper.listEntityToDtoList(clientes);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message("Consulta exitosa")
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            log.error("Error al consultar clientes por id de empresa: {}", idEmpresa, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message("Error consultando")
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .response(null)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }



    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findContadoresByEmpresaId(Integer idEmpresa, Pageable pageable) {
        log.info("Buscar contadores por id de empresa: {}", idEmpresa);
        try {
        	List<ContadorEntity> contadores;
            if(pageable.isPaged()) {
            	Page<ContadorEntity> page = empresaClienteContadorRepository.findAllContadoresByEmpresaIdPaged(idEmpresa, pageable);
                contadores = page.getContent();
            } else {
            	contadores = empresaClienteContadorRepository.findAllContadoresByEmpresaId(idEmpresa);
            }
            
            
            List<ContadorDTO> dtoList = contadorMapper.listEntityToDtoList(contadores);

            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al consultar contadores por id de empresa: {}", idEmpresa, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.CONSULTING_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .response(null)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }


    
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findByEmpresaIdResponseId(Integer idEmpresa) {
        log.info("Buscar Empresa Cliente Contador por id de empresa: {}", idEmpresa);
        try {
        	var list = empresaClienteContadorRepository.findByEmpresa_Id(idEmpresa);

            if (list.isEmpty()) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message("No se encontraron registros para la empresa con id: " + idEmpresa)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }

            List<Map<String, Object>> idList = list.stream()
            	    .map(e -> Map.<String, Object>of("id", e.getId()))
            	    .toList();

            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(idList)
                    .build();

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
        	log.error("Error al consultar por id de empresa: {}", idEmpresa, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.CONSULTING_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseDTO> findById(Integer id) {
	    log.info("Buscar Empresa Cliente Contador por id: {}", id);
	    try {
	        Optional<EmpresaClienteContadorEntity> empresaClienteContador = empresaClienteContadorRepository.findById(id);
	        if (empresaClienteContador.isPresent()) {
	        	EmpresaClienteContadorDTO dto = empresaClienteContadorMapper.entityToDto(empresaClienteContador.get());
	            ResponseDTO responseDTO = ResponseDTO.builder()
	                    .success(true)
	                    .message(Constantes.CONSULTED_SUCCESSFULLY)
	                    .code(HttpStatus.OK.value())
	                    .response(dto)
	                    .build();
	            return ResponseEntity.ok(responseDTO);
	        } else {
	            ResponseDTO responseDTO = ResponseDTO.builder()
	                    .success(false)
	                    .message(Constantes.CONSULTING_ERROR)
	                    .code(HttpStatus.NOT_FOUND.value())
	                    .build();
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
	        }
	    } catch (Exception e) {
	        log.error("Error al buscar  Empresa Cliente Contador por id: {}", id, e);
	        ResponseDTO responseDTO = ResponseDTO.builder()
	                .success(false)
	                .message(Constantes.ERROR_QUERY_RECORD_BY_ID)
	                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
	                .build();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
	    }
	}

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDTO> findAll() {
        log.info("Listar todos las Empresa Cliente Contador");
        try {
            var list = empresaClienteContadorRepository.findAll();
            var dtoList = empresaClienteContadorMapper.listEntityToDtoList(list);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.CONSULTED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(dtoList)
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al listar las Empresa Cliente Contador", e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.CONSULTING_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .response(null)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDTO> deleteById(Integer id) {
        log.info("Inicio método para eliminar Empresa Cliente Contador por id: {}", id);
        try {
            if (!empresaClienteContadorRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            empresaClienteContadorRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar Empresa Cliente Contador con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}
