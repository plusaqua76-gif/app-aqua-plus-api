package com.aqua.plus.api.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

private static final String SPRING_APPLICATION_NAME = "spring.application.name";
    
    private final Environment env;

    @Bean
    OpenAPI customOpenAPI() {
        String appName = env.getProperty(SPRING_APPLICATION_NAME);
        return new OpenAPI().info(new Info()
            .title(appName + " Service API")
            .version("1.0")
            .description(appName + " API Description")
            .termsOfService("https://multi-acueductos.com.co")
            .contact(new Contact()
                .name("MULTI ACUEDUCTOS")
                .email("admongestionplus360@gmail.com"))
            .license(new License()
                .name("LICENSE")
                .url("LICENSE URL")));
    }

    @Bean
    GroupedOpenApi groupPermitted() {
        return GroupedOpenApi.builder()
            .group(env.getProperty(SPRING_APPLICATION_NAME))
            .packagesToScan("com.codemakers.api.controller")
            .packagesToExclude("com.codemakers.commons.dtos", "com.codemakers.commons.entities")
            .build();
    }
}
