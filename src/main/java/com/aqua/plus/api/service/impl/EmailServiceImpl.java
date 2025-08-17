package com.aqua.plus.api.service.impl;

import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.aqua.plus.api.config.MailConfig;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl {
	
	private final MailConfig mailSender;


	public void sendEmail(String to, String subject, String body) {
		try {
			MimeMessage message = mailSender.javaMailSender().createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(body, true);
			mailSender.javaMailSender().send(message);
			log.info("Correo enviado a {}", to);
		} catch (MessagingException e) {
			log.error("Error enviando correo a {}", to, e);
		}
	}
}