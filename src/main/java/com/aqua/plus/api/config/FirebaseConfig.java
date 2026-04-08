package com.aqua.plus.api.config;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import lombok.extern.slf4j.Slf4j;

/**
 * @author andreschavarro
 * @version 1.0
 *
 *          Configuración de Firebase Admin SDK.
 *          Requiere el archivo service-account.json descargado desde:
 *          Firebase Console → Configuración del proyecto → Cuentas de servicio.
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
    public FirebaseApp firebaseApp(
            @Value("${firebase.credentials}") Resource credentialsResource,
            @Value("${firebase.project-id}") String projectId) throws IOException {

        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("FirebaseApp ya inicializado, reutilizando instancia.");
            return FirebaseApp.getInstance();
        }

        try (InputStream credentialsStream = credentialsResource.getInputStream()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build();
            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("FirebaseApp inicializado correctamente para proyecto: {}", projectId);
            return app;
        } catch (IOException e) {
            log.error("No se pudo inicializar Firebase. Verifique el archivo de credenciales en: {}",
                    credentialsResource.getDescription(), e);
            throw e;
        }
    }
}
