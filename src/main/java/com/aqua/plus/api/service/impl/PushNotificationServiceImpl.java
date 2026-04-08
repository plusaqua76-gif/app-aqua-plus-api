package com.aqua.plus.api.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.aqua.plus.commons.entities.FcmTokenEntity;
import com.aqua.plus.commons.repositories.FcmTokenRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author andreschavarro
 * @version 1.0
 *
 *          Servicio encargado de enviar notificaciones push a través de
 *          Firebase Cloud Messaging (FCM). Las notificaciones se envían de
 *          forma asíncrona para no bloquear el flujo principal de negocio.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationServiceImpl {

    private final FcmTokenRepository fcmTokenRepository;

    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;

    /**
     * Envía una notificación push a todos los tokens FCM activos del usuario.
     * Se ejecuta de forma asíncrona en el executor 'notificacionExecutor'.
     *
     * @param usuarioId ID del usuario destino
     * @param titulo    Título de la notificación
     * @param cuerpo    Cuerpo del mensaje
     */
    @Async("notificacionExecutor")
    public void enviarNotificacionFactura(Integer usuarioId, String titulo, String cuerpo) {
        log.info("[PUSH-ASYNC] Iniciando envío push - usuarioId={}, firebaseEnabled={}", usuarioId, firebaseEnabled);
        if (!firebaseEnabled) {
            log.warn("[PUSH-ASYNC] Firebase deshabilitado (firebase.enabled=false). Notificación omitida para usuarioId={}", usuarioId);
            return;
        }

        if (usuarioId == null) {
            log.warn("[PUSH-ASYNC] usuarioId es null, notificación omitida.");
            return;
        }

        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("[PUSH-ASYNC] FirebaseApp no está inicializado. Notificación omitida para usuarioId={}", usuarioId);
            return;
        }

        List<FcmTokenEntity> tokens = fcmTokenRepository.findByUsuarioIdAndActivoTrue(usuarioId.longValue());

        if (tokens.isEmpty()) {
            log.info("No hay tokens FCM activos para usuarioId={}", usuarioId);
            return;
        }

        log.info("Enviando notificación push a usuarioId={}, tokens activos={}", usuarioId, tokens.size());

        for (FcmTokenEntity tokenEntity : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(tokenEntity.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(titulo)
                                .setBody(cuerpo)
                                .build())
                        .build();

                String messageId = FirebaseMessaging.getInstance().send(message);
                log.info("Push enviado exitosamente. usuarioId={}, dispositivo={}, messageId={}",
                        usuarioId, tokenEntity.getDispositivo(), messageId);

            } catch (FirebaseMessagingException e) {
                if (MessagingErrorCode.UNREGISTERED.equals(e.getMessagingErrorCode())) {
                    log.warn("Token FCM no registrado (expirado/revocado). Desactivando token para usuarioId={}, dispositivo={}",
                            usuarioId, tokenEntity.getDispositivo());
                    tokenEntity.setActivo(false);
                    fcmTokenRepository.save(tokenEntity);
                } else {
                    log.error("Error enviando push a usuarioId={}, dispositivo={}: {}",
                            usuarioId, tokenEntity.getDispositivo(), e.getMessage());
                }
            }
        }
    }
}
