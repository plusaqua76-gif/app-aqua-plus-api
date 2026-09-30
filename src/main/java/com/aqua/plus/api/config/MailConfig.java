package com.aqua.plus.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;


import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${mail.host}")
    private String host;
    
    @Value("${mail.port}")
    private Integer port;
    
    @Value("${mail.username}")
    private String user;
    
    @Value("${mail.password}")
    private String password;
    
    @Value("${mail.properties.mail.smtp.auth}")
    private String auth;
    
    @Value("${mail.properties.mail.smtp.starttls.enable}")
    private String enable;
    
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        mailSender.setHost(this.host);
        mailSender.setPort(this.port);
        mailSender.setUsername(this.user);
        
        // Asignación directa en texto plano
        mailSender.setPassword(this.password);
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", this.auth);
        props.put("mail.smtp.starttls.enable", this.enable);
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        
        return mailSender;
    }
}

