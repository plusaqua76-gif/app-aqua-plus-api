package com.aqua.plus.api.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.aqua.plus.api.service.impl.EmpresaServiceImpl;
import com.aqua.plus.commons.dtos.EmpresaDTO;
import com.aqua.plus.commons.dtos.ResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/empresa")
@Tag(name = "Empresa - Controller", description = "Controller encargado de gestionar las operaciones de la empresa")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
		RequestMethod.PUT })
@RequiredArgsConstructor
public class EmpresaController {
	
    private final EmpresaServiceImpl empresaServiceImpl;
    
    
	@Operation(summary = "Guardar  Empresa")
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
	                @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
	})
    @PostMapping
    public ResponseEntity<ResponseDTO> save(@RequestBody EmpresaDTO empresaDTO) {
        return empresaServiceImpl.save(empresaDTO);
    }
	
	@Operation(summary = "Registrar Nueva Empresa y Usuario SP")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Empresa y usuario registrados exitosamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (ej. ID de dirección/ciudad/departamento no existe)", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "409", description = "Conflicto: Usuario o Empresa ya existen", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
    })
    @PostMapping("/registrar")
	public ResponseEntity<Map<String, Object>> registrarEmpresa(@RequestBody Map<String, Object> body) {
	    Map<String, Object> resp = empresaServiceImpl.registrarEmpresa(body);
	    Integer code = null;
	    Object c = resp.get("code");
	    if (c != null) {
	        code = Integer.valueOf(String.valueOf(c));
	    }

	    if (code != null) {
	        return ResponseEntity.status(code).body(resp);
	    }
	    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
	}

    @Operation(summary = "Buscar Empresa por id de usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Se ha guardado satisfactoriamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
    })
    @GetMapping("/usuario/{id}")
    public ResponseEntity<ResponseDTO> getUserById(@PathVariable Integer id) {
        return empresaServiceImpl.findByUsuarioId(id);
    }
    
    @Operation(summary = "Buscar Empresa por id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Se ha guardado satisfactoriamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
    })
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getById(@PathVariable Integer id) {
        return empresaServiceImpl.findById(id);
    }

    @Operation(summary = "Listar todos los empresa")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Se ha guardado satisfactoriamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
    })
    @GetMapping("/all")
    public ResponseEntity<ResponseDTO> getAll() {
        return empresaServiceImpl.findAll();
    }
    
    @Operation(summary = "Listar todos los empresa con response id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Se ha guardado satisfactoriamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "La petición no puede ser entendida por el servidor debido a errores de sintaxis", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "El recurso solicitado no puede ser encontrado", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Se presentó una condición inesperada que impidió completar la petición", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
    })
    @GetMapping("/all/responseId")
    public ResponseEntity<ResponseDTO> getAllResponseId() {
        return empresaServiceImpl.getAllEnterpriseResponseId();
    }

    @Operation(summary = "Eliminar empresa por id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rol eliminado correctamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deleteById(@PathVariable Integer id) {
        return empresaServiceImpl.deleteById(id);
    }
    
    @Operation(summary = "actualizar Empresa")
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
	                @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
	})   
    @PutMapping   
    public ResponseEntity<ResponseDTO> update(@RequestBody EmpresaDTO empresaDTO) {
        return empresaServiceImpl.update(empresaDTO);
    }
    

    @Operation(summary = "Actualizar empresa y su dirección")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Empresa actualizada correctamente", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))
        }),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))
        }),
        @ApiResponse(responseCode = "400", description = "Error de solicitud", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))
        }),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))
        }),
    })
    @PostMapping("/actualizar-direccion")
    public ResponseEntity<Map<String, Object>> actualizarEmpresaDireccion(
        @RequestBody Map<String, Object> jsonParams) {

        try {
            Map<String, Object> resultado = empresaServiceImpl.updateEmpresaDireccion(jsonParams);

            if (resultado.containsKey("statusCode")) {
                int status = (int) resultado.get("statusCode");

                return switch (status) {
                    case 404 -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(resultado);
                    case 500 -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resultado);
                    default -> ResponseEntity.ok(resultado);
                };
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Respuesta inesperada del servidor"));
        } catch (Exception e) {
           
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor"));
        }
    }


    @Operation(summary = "Activar empresa completa")
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
                                    @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)) }),
    })
    @PostMapping("/actualizar")
    public ResponseEntity<Map<String, Object>> updateEnterpiseStatus(@RequestBody Map<String, Object> enterpriseData) {
        Map<String, Object> resultado = empresaServiceImpl.updateEnterprise(enterpriseData);

        if (resultado.containsKey("error")) {
            return ResponseEntity.badRequest().body(resultado);
        } else {
            return ResponseEntity.ok(resultado);
        }
    }

}
