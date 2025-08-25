package com.aqua.plus.api.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IEmpresaService;
import com.aqua.plus.api.utils.EncriptarDesencriptar;
import com.aqua.plus.commons.dtos.EmpresaDTO;
import com.aqua.plus.commons.dtos.EmpresaResponseDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.CiudadEntity;
import com.aqua.plus.commons.entities.DepartamentoEntity;
import com.aqua.plus.commons.entities.EmpresaEntity;
import com.aqua.plus.commons.maps.EmpresaMapper;
import com.aqua.plus.commons.repositories.CiudadRepository;
import com.aqua.plus.commons.repositories.DepartamentoRepository;
import com.aqua.plus.commons.repositories.EmpresaRepository;
import com.aqua.plus.commons.utils.Constantes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmpresaServiceImpl implements IEmpresaService {
	
	private final EmpresaRepository empresaRepository;
	private final EmpresaMapper empresaMapper;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;
    private final EncriptarDesencriptar encriptarDesencriptar;
    private final NotificacionServiceImpl notificacionServiceImpl;
    private final DepartamentoRepository departamentoRepository;
    private final CiudadRepository ciudadRepository;
    
    @Value("${mail.username}")
    private String correoAquaPlus;
	
	@Override
	@Transactional
    public ResponseEntity<ResponseDTO> save(EmpresaDTO empresaDTO) {
        log.info("Creando Empresa");
        try {
            EmpresaEntity entity = empresaMapper.dtoToEntity(empresaDTO);
            entity.setFechaCreacion(new Date());
            entity.setUsuarioCreacion(empresaDTO.getUsuarioCreacion());
            entity.setActivo(true);

            EmpresaEntity saved = empresaRepository.save(entity);
            EmpresaDTO savedDTO = empresaMapper.entityToDto(saved);

            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.SAVED_SUCCESSFULLY)
                    .code(HttpStatus.CREATED.value())
                    .response(savedDTO)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        } catch (Exception e) {
            log.error("Error creando la Empresa", e);
            ResponseDTO errorResponse = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.SAVE_ERROR)
                    .code(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
	
	@Transactional
	public Map<String, Object> updateEmpresaDireccion(Map<String, Object> jsonParams) {
	    try {
	        String jsonString = objectMapper.writeValueAsString(jsonParams);
	        String sql = "SELECT * FROM public.actualizar_empresa_direccion(CAST(:jsonData AS jsonb))";
	        MapSqlParameterSource parameters = new MapSqlParameterSource();
	        parameters.addValue("jsonData", jsonString);
	        Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);
	        Object wrappedValue = rawResult.get("actualizar_empresa_direccion");
	        if (wrappedValue instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {
	            String jsonValue = pgObject.getValue();
	            return objectMapper.readValue(jsonValue, new TypeReference<Map<String, Object>>() {});
	        }
	        return Map.of("error", "El resultado no pudo ser procesado correctamente.");
	    } catch (JsonProcessingException e) {
	        log.error("Error de procesamiento JSON", e);
	        return Map.of("error", "Error de procesamiento JSON: " + e.getMessage());
	    } catch (Exception e) {
	        log.error("Error inesperado en actualizarEmpresaDireccion", e);
	        return Map.of("error", "Error inesperado: " + e.getMessage());
	    }
	}

	@Transactional
    public Map<String, Object> registrarEmpresa(Map<String, Object> jsonParams) {
        try {
            String plainPassword = (String) jsonParams.get("password");

            if (plainPassword != null) {
                String encodedPassword = encriptarDesencriptar.encriptar(plainPassword);
                jsonParams.put("password", encodedPassword);
            }

            String jsonString = objectMapper.writeValueAsString(jsonParams);

            String sql = "SELECT * FROM public.crear_o_actualizar_empresa(CAST(:jsonData AS jsonb))"; 
            
            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue("jsonData", jsonString);

            Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

            Object wrappedValue = rawResult.get("crear_o_actualizar_empresa"); 
            if (wrappedValue instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {
            	String jsonValue = pgObject.getValue();
                Map<String, Object> response = objectMapper.readValue(jsonValue, new TypeReference<>() {});
            
	            Object statusCode = response.get("statusCode");
	            if("201".equals(String.valueOf(statusCode))) {
	            	
	            	Integer idDepartamento = Integer.valueOf(jsonParams.get("idDepartamento").toString());
	            	Integer idCiudad = Integer.valueOf(jsonParams.get("idCiudad").toString());
	            	
	            	String nombreDepartamento = departamentoRepository.findById(idDepartamento)
	            			.map(DepartamentoEntity::getNombre)
	            			.orElse("Desconocido");
	            	
	            	String nombreCiudad = ciudadRepository.findById(idCiudad)
	            			.map(CiudadEntity::getNombre)
	            			.orElse("Desconocido");
	            			
	            	String nombreEmpresa = (String) jsonParams.get("nombreEmpresa");
	            	String nit = (String) jsonParams.get("nit");
	            	String correoEmpresa = (String) jsonParams.get("correo");
	            	String telefono = (String) jsonParams.get("telefono");
	            	
	            	if (correoAquaPlus != null && !correoAquaPlus.isBlank()) {
	                    Map<String, Object> data = new HashMap<>();
	                    data.put(Constantes.PARAMETRO_NAME_ENTERPRISE, nombreEmpresa);
	                    data.put(Constantes.PARAMETRO_NIT, nit);
	                    data.put(Constantes.PARAMETRO_EMAIL, correoEmpresa);
	                    data.put(Constantes.PARAMETRO_PHONE, telefono);
	                    data.put(Constantes.PARAMETRO_DEPARTAMENT, nombreDepartamento);
	                    data.put(Constantes.PARAMETRO_CITY, nombreCiudad);
	                    
	                    log.info("Info data notificacion {}", data);

	                    String codigoPlantilla = Constantes.INFO_ACTIVATE;

	                    notificacionServiceImpl.enviarNotificacion(correoAquaPlus, codigoPlantilla, data);
	                }

	            }
            }

            return Map.of(Constantes.ERROR_KEY, "El resultado no pudo ser procesado correctamente.");

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Collections.singletonMap(Constantes.ERROR_KEY, "Error de procesamiento JSON: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.singletonMap(Constantes.ERROR_KEY, "Error inesperado: " + e.getMessage());
        }
    }
	
	@Transactional
    public Map<String, Object> updateEnterpise(Map<String, Object> jsonParams) {
        try {
            
            String jsonString = objectMapper.writeValueAsString(jsonParams);

           
            String sql = "SELECT * FROM public.actualizar_estado_empresa(CAST(:jsonData AS jsonb))";

          
            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue("jsonData", jsonString);

          
            Map<String, Object> rawResult = namedParameterJdbcTemplate.queryForMap(sql, parameters);

         
            Object wrappedValue = rawResult.get("actualizar_estado_empresa");
            if (wrappedValue instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {
                String jsonValue = pgObject.getValue();
                return objectMapper.readValue(jsonValue, new TypeReference<Map<String, Object>>() {});
            }

            return Map.of(Constantes.ERROR_KEY, "El resultado no pudo ser procesado correctamente.");

        } catch (JsonProcessingException e) {
            log.error("Error de procesamiento JSON", e);
            return Map.of(Constantes.ERROR_KEY, "Error de procesamiento JSON: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado en updateEnterpise", e);
            return Map.of(Constantes.ERROR_KEY, "Error inesperado: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public ResponseEntity<ResponseDTO> update(EmpresaDTO empresaDTO) {
        log.info("Actualizando Empresa");
        try {
            if (empresaDTO.getId() == null || !empresaRepository.existsById(empresaDTO.getId())) {
                throw new IllegalArgumentException(Constantes.EMP_NOT_FOUND);
            }

            EmpresaEntity entity = empresaRepository.findById(empresaDTO.getId()).orElseThrow();
            empresaMapper.updateEntityFromDto(empresaDTO, entity); 
            entity.setFechaModificacion(new Date());
            entity.setUsuarioModificacion(empresaDTO.getUsuarioModificacion());

            EmpresaEntity updated = empresaRepository.save(entity);
            EmpresaDTO updatedDTO = empresaMapper.entityToDto(updated);

            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.UPDATED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .response(updatedDTO)
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
        } catch (Exception e) {
            log.error("Error actualizando la Empresa", e);
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
    public ResponseEntity<ResponseDTO> getAllEnterpriseResponseId() {
        log.info("Consultar todas las empresas y se muestra en el response solo los id de los registros:");
        try {
            List<EmpresaEntity> list = empresaRepository.findAll();

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
            log.error("Error al consultar por id de empresa", e);

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
        log.info("Buscar Empresa por id: {}", id);
        try {
            Optional<EmpresaEntity> empresa = empresaRepository.findById(id);
            if (empresa.isPresent()) {
                EmpresaResponseDTO dto = empresaMapper.entityToResumenDto(empresa.get());
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
                        .message("No se encontró la empresa con el ID especificado")
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
        } catch (Exception e) {
            log.error("Error al buscar Empresa por id: {}", id, e);
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
	public ResponseEntity<ResponseDTO> findByUsuarioId(Integer idUsuario) {
	    log.info("Buscar Empresa por id de usuario: {}", idUsuario);
	    try {
	        Optional<EmpresaEntity> empresa = empresaRepository.findByUsuario_Id(idUsuario);

	        if (empresa.isPresent()) {
	            Map<String, Object> responseMap = new HashMap<>();
	            responseMap.put("idEmpresa", empresa.get().getId());

	            ResponseDTO responseDTO = ResponseDTO.builder()
	                    .success(true)
	                    .message(Constantes.CONSULTED_SUCCESSFULLY)
	                    .code(HttpStatus.OK.value())
	                    .response(responseMap)
	                    .build();

	            return ResponseEntity.ok(responseDTO);
	        } else {
	            ResponseDTO responseDTO = ResponseDTO.builder()
	                    .success(false)
	                    .message("No se encontró una empresa asociada al usuario")
	                    .code(HttpStatus.NOT_FOUND.value())
	                    .build();
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
	        }

	    } catch (Exception e) {
	        log.error("Error al buscar empresa por id de usuario: {}", idUsuario, e);
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
	    log.info("Listar todas las empresas");
	    try {
	        var list = empresaRepository.findAll();
	        var dtoList = empresaMapper.listEntityToResumenDtoList(list); 
	        ResponseDTO responseDTO = ResponseDTO.builder()
	                .success(true)
	                .message(Constantes.CONSULTED_SUCCESSFULLY)
	                .code(HttpStatus.OK.value())
	                .response(dtoList)
	                .build();
	        return ResponseEntity.ok(responseDTO);
	    } catch (Exception e) {
	        log.error("Error al listar las empresas", e);
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
        log.info("Inicio método para eliminar empresa por id: {}", id);
        try {
            if (!empresaRepository.existsById(id)) {
                ResponseDTO responseDTO = ResponseDTO.builder()
                        .success(false)
                        .message(Constantes.RECORD_NOT_FOUND)
                        .code(HttpStatus.NOT_FOUND.value())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO);
            }
            empresaRepository.deleteById(id);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(true)
                    .message(Constantes.DELETED_SUCCESSFULLY)
                    .code(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error al eliminar empresa con id: {}", id, e);
            ResponseDTO responseDTO = ResponseDTO.builder()
                    .success(false)
                    .message(Constantes.DELETE_ERROR)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
    }
}
