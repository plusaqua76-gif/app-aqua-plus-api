package com.acua.plus.api.service;

import org.springframework.http.ResponseEntity;

import com.acua.plus.commons.dtos.ResponseDTO;
import com.acua.plus.commons.dtos.UsuarioDTO;

/**
 * @author nicope
 * @version 1.0
 * 
 *          Esta interfaz es la capa intermedia entre la capa de presentación y
 *          la capa de acceso a datos. Esta oculta los detalles de
 *          implementación de la capa de acceso a datos.
 * 
 */

public interface IUsuarioService {

	ResponseEntity<ResponseDTO> save(UsuarioDTO usuarioDTO);
	ResponseEntity<ResponseDTO> updateImage(Integer id, byte[] nuevaImagen, String usuarioModificacion);
    ResponseEntity<ResponseDTO> findById(Integer id);
    ResponseEntity<ResponseDTO> findAll();
    ResponseEntity<ResponseDTO> deleteById(Integer id);
    ResponseEntity<ResponseDTO> updatePassword(Integer idUsuario, String nuevaContrasena, String usuarioModificacion);
    
}
