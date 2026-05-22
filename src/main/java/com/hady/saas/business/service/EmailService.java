package com.hady.saas.business.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${agendara.email.remetente}")
    private String remetente;

    @Value("${agendara.email.nome-remetente}")
    private String nomeRemetente;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Envia email HTML
    public void enviar(String destinatario, String assunto, String corpoHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(remetente, nomeRemetente);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(corpoHtml, true); // true = HTML

            mailSender.send(message);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Erro ao enviar email: " + e.getMessage());
        }
    }

    // Template de lembrete de agendamento
    public String templateLembrete(String nomeCliente, String nomeProfissional,
                                    String nomeServico, String nomeEstabelecimento,
                                    String dataHora) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background-color: #6C63FF; padding: 30px; text-align: center;">
                        <h1 style="color: white; margin: 0;">Agendara</h1>
                    </div>
                    <div style="padding: 30px; background-color: #f9f9f9;">
                        <h2 style="color: #333;">Olá, %s! 👋</h2>
                        <p style="color: #555; font-size: 16px;">
                            Este é um lembrete do seu agendamento:
                        </p>
                        <div style="background-color: white; border-radius: 8px; padding: 20px; margin: 20px 0; border-left: 4px solid #6C63FF;">
                            <p style="margin: 8px 0;"><strong>📍 Estabelecimento:</strong> %s</p>
                            <p style="margin: 8px 0;"><strong>💇 Serviço:</strong> %s</p>
                            <p style="margin: 8px 0;"><strong>👤 Profissional:</strong> %s</p>
                            <p style="margin: 8px 0;"><strong>📅 Data e hora:</strong> %s</p>
                        </div>
                        <p style="color: #555;">
                            Caso precise cancelar ou remarcar, entre em contato com o estabelecimento.
                        </p>
                    </div>
                    <div style="background-color: #eee; padding: 15px; text-align: center;">
                        <p style="color: #999; font-size: 12px; margin: 0;">
                            Enviado por Agendara • Sistema de agendamento online
                        </p>
                    </div>
                </div>
                """.formatted(nomeCliente, nomeEstabelecimento, nomeServico, nomeProfissional, dataHora);
    }

    // Template de confirmação de agendamento
    public String templateConfirmacao(String nomeCliente, String nomeProfissional,
                                       String nomeServico, String nomeEstabelecimento,
                                       String dataHora) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background-color: #6C63FF; padding: 30px; text-align: center;">
                        <h1 style="color: white; margin: 0;">Agendara</h1>
                    </div>
                    <div style="padding: 30px; background-color: #f9f9f9;">
                        <h2 style="color: #333;">Agendamento confirmado! ✅</h2>
                        <p style="color: #555; font-size: 16px;">
                            Olá, <strong>%s</strong>! Seu agendamento foi realizado com sucesso.
                        </p>
                        <div style="background-color: white; border-radius: 8px; padding: 20px; margin: 20px 0; border-left: 4px solid #28a745;">
                            <p style="margin: 8px 0;"><strong>📍 Estabelecimento:</strong> %s</p>
                            <p style="margin: 8px 0;"><strong>💇 Serviço:</strong> %s</p>
                            <p style="margin: 8px 0;"><strong>👤 Profissional:</strong> %s</p>
                            <p style="margin: 8px 0;"><strong>📅 Data e hora:</strong> %s</p>
                        </div>
                        <p style="color: #555;">
                            Te vemos em breve! 😊
                        </p>
                    </div>
                    <div style="background-color: #eee; padding: 15px; text-align: center;">
                        <p style="color: #999; font-size: 12px; margin: 0;">
                            Enviado por Agendara • Sistema de agendamento online
                        </p>
                    </div>
                </div>
                """.formatted(nomeCliente, nomeEstabelecimento, nomeServico, nomeProfissional, dataHora);
    }
}
