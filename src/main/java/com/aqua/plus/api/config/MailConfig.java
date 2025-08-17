package com.aqua.plus.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.aqua.plus.api.utils.EncriptarDesencriptar;

import lombok.RequiredArgsConstructor;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
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
	
	private final EncriptarDesencriptar encriptarDesencriptar;
	
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        mailSender.setHost(this.host);
        mailSender.setPort(this.port);

        mailSender.setUsername(this.user);
        mailSender.setPassword(this.encriptarDesencriptar.desencriptar(password));
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", this.auth);
        props.put("mail.smtp.starttls.enable", this.enable);
        
        return mailSender;
    }
}

