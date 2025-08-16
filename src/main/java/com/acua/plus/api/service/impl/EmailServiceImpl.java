package com.acua.plus.api.service.impl;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailServiceImpl {
	
	private final JavaMailSender mailSender;

	public EmailServiceImpl(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	public void sendEmail(String to, String subject, String body) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(body, true);
			mailSender.send(message);
			log.info("Correo enviado a {}", to);
		} catch (MessagingException e) {
			log.error("Error enviando correo a {}", to, e);
		}
	}
}