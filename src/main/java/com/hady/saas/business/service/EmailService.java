package com.hady.saas.business.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Envia o código de verificação 2FA para o login do admin master.
    // Reaproveita a config SMTP (spring.mail) já existente no application.yml.
    public void enviarCodigo2FA(String destinatario, String codigo) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(destinatario);
        mensagem.setSubject("Seu código de acesso — Agendara Admin");
        mensagem.setText(
                "Seu código de verificação é: " + codigo + "\n\n" +
                        "Esse código expira em 5 minutos.\n" +
                        "Se você não tentou fazer login, ignore este email."
        );
        mailSender.send(mensagem);
    }
}