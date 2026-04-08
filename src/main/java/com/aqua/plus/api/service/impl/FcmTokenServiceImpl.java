package com.aqua.plus.api.service.impl;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aqua.plus.api.service.IFcmTokenService;
import com.aqua.plus.commons.dtos.ResponseDTO;
import com.aqua.plus.commons.entities.FcmTokenEntity;
import com.aqua.plus.commons.entities.UsuarioEntity;
import com.aqua.plus.commons.repositories.FcmTokenRepository;
import com.aqua.plus.commons.repositories.UsuarioRepository;
import com.aqua.plus.commons.utils.Constantes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmTokenServiceImpl implements IFcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public ResponseEntity<ResponseDTO> guardarToken(Long usuarioId, String token, String dispositivo, String usuarioCreacion) {
        log.info("Inicio metodo guardarToken - usuarioId: {}, dispositivo: {}", usuarioId, dispositivo);
        try {
            Optional<FcmTokenEntity> existente = fcmTokenRepository.findByToken(token);

            if (existente.isPresent()) {
                FcmTokenEntity fcmToken = existente.get();
                fcmToken.setActivo(true);
                fcmToken.setDispositivo(dispositivo);
                fcmTokenRepository.save(fcmToken);
                log.info("Token FCM actualizado exitosamente");
                return new ResponseEntity<>(
                        ResponseDTO.builder()
                                .success(true)
                                .message(Constantes.UPDATED_SUCCESSFULLY)
                                .code(HttpStatus.OK.value())
                                .build(),
                        HttpStatus.OK);
            } else {
                UsuarioEntity usuario = usuarioRepository.findById(usuarioId.intValue())
                        .orElseThrow(() -> new RuntimeException(Constantes.USER_NOT_FOUND));

                FcmTokenEntity nuevo = new FcmTokenEntity();
                nuevo.setUsuario(usuario);
                nuevo.setToken(token);
                nuevo.setDispositivo(dispositivo);
                nuevo.setActivo(true);
                nuevo.setUsuarioCreacion(usuarioCreacion);
                fcmTokenRepository.save(nuevo);
                log.info("Token FCM guardado exitosamente");
                return new ResponseEntity<>(
                        ResponseDTO.builder()
                                .success(true)
                                .message(Constantes.SAVED_SUCCESSFULLY)
                                .code(HttpStatus.CREATED.value())
                                .build(),
                        HttpStatus.CREATED);
            }
        } catch (Exception e) {
            log.error("Error en guardarToken: {}", e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseDTO.builder()
                            .success(false)
                            .message(Constantes.SAVE_ERROR)
                            .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
