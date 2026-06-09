package com.hady.saas.business.service;

import com.hady.saas.domain.usuario.PasswordResetToken;
import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.repository.PasswordResetTokenRepository;
import com.hady.saas.infrastructure.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    // FIX: usado para publicar evento de e-mail após o commit da transação
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String remetente;

    public PasswordResetService(UsuarioRepository usuarioRepository,
                                PasswordResetTokenRepository tokenRepository,
                                JavaMailSender mailSender,
                                PasswordEncoder passwordEncoder,
                                ApplicationEventPublisher eventPublisher) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    // ─── 1. Solicitar reset ───────────────────────────────────────────────────
    // Sempre retorna a mesma mensagem — não revela se o e-mail existe ou não.
    @Transactional
    public void solicitarReset(String email) {
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {

            // Remove tokens anteriores do usuário
            tokenRepository.deleteByUsuarioId(usuario.getId());

            // Gera token único com validade de 1 hora
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken(
                    token, usuario, LocalDateTime.now().plusHours(1));
            tokenRepository.save(resetToken);

            // FIX: publica evento para enviar o e-mail SOMENTE após o commit.
            // Sem isso, o e-mail era disparado dentro da transação — se o commit
            // falhasse depois, o link chegaria ao usuário mas o token não existiria no banco.
            eventPublisher.publishEvent(new PasswordResetEmailEvent(usuario, token));
        });
    }

    // ─── Evento disparado após commit para envio de e-mail ───────────────────
    public record PasswordResetEmailEvent(Usuario usuario, String token) {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetEmailEvent(PasswordResetEmailEvent event) {
        enviarEmail(event.usuario(), event.token());
    }

    // ─── 2. Validar token (uso opcional no frontend antes de exibir o form) ───
    public void validarToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Link de recuperação inválido ou expirado."));

        if (resetToken.isUsado()) {
            throw new RuntimeException("Este link já foi utilizado. Solicite um novo.");
        }
        if (resetToken.estaExpirado()) {
            throw new RuntimeException("Link de recuperação expirado. Solicite um novo.");
        }
    }

    // ─── 3. Confirmar nova senha ──────────────────────────────────────────────
    @Transactional
    public void confirmarReset(String token, String novaSenha, String confirmarSenha) {
        if (!novaSenha.equals(confirmarSenha)) {
            throw new RuntimeException("As senhas não coincidem");
        }
        if (novaSenha.length() < 8) {
            throw new RuntimeException("A senha deve ter no mínimo 8 caracteres.");
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Link de recuperação inválido ou expirado."));

        if (resetToken.isUsado()) {
            throw new RuntimeException("Este link já foi utilizado. Solicite um novo.");
        }
        if (resetToken.estaExpirado()) {
            throw new RuntimeException("Link de recuperação expirado. Solicite um novo.");
        }

        // Atualiza a senha e marca o instante da troca —
        // invalida todos os tokens emitidos antes deste momento.
        Usuario usuario = resetToken.getUsuario();
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuario.setSenhaAtualizadaEm(java.time.Instant.now());
        usuarioRepository.save(usuario);

        // Invalida o token
        resetToken.setUsado(true);
        tokenRepository.save(resetToken);

        log.info("[RESET-SENHA] Senha redefinida para usuário id={}", usuario.getId());
    }

    // ─── Envio do e-mail ──────────────────────────────────────────────────────
    private void enviarEmail(Usuario usuario, String token) {
        String link = baseUrl + "/redefinir-senha?token=" + token;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(remetente);
        msg.setTo(usuario.getEmail());
        msg.setSubject("Agendara — Redefinição de senha");
        msg.setText(
                "Olá, " + usuario.getNome() + "!\n\n" +
                        "Recebemos uma solicitação para redefinir sua senha.\n\n" +
                        "Clique no link abaixo para criar uma nova senha:\n" +
                        link + "\n\n" +
                        "Este link é válido por 1 hora.\n\n" +
                        "Se você não fez essa solicitação, ignore este e-mail — sua senha permanece a mesma.\n\n" +
                        "Equipe Agendara"
        );

        try {
            mailSender.send(msg);
            log.info("[RESET-SENHA] E-mail enviado para {}", usuario.getEmail());
        } catch (Exception e) {
            log.error("[RESET-SENHA] Falha ao enviar e-mail para {}: {}", usuario.getEmail(), e.getMessage());
            // Não propaga — o usuário não fica sabendo se o e-mail existe ou não
        }
    }
}