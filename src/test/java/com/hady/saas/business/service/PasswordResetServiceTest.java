package com.hady.saas.business.service;

import com.hady.saas.domain.usuario.PasswordResetToken;
import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.repository.PasswordResetTokenRepository;
import com.hady.saas.infrastructure.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock PasswordResetTokenRepository tokenRepository;
    @Mock JavaMailSender mailSender;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks PasswordResetService service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(service, "remetente", "noreply@agendara.com.br");

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Teste");
        usuario.setEmail("teste@email.com");
        usuario.setSenha("senhaHash");
    }

    // ── solicitarReset ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Não deve revelar se o e-mail existe (sempre silencioso)")
    void solicitarReset_emailInexistente_naoLancaExcecao() {
        when(usuarioRepository.findByEmail("inexistente@email.com")).thenReturn(Optional.empty());

        assertThatCode(() -> service.solicitarReset("inexistente@email.com"))
                .doesNotThrowAnyException();

        verify(tokenRepository, never()).save(any());
        verify(mailSender, never()).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve gerar token e enviar e-mail quando usuário existe")
    void solicitarReset_emailExistente_geraTokenEEnviaEmail() {
        when(usuarioRepository.findByEmail("teste@email.com")).thenReturn(Optional.of(usuario));
        doNothing().when(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));

        service.solicitarReset("teste@email.com");

        verify(tokenRepository).deleteByUsuarioId(1L);
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    // ── validarToken ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve lançar exceção para token inexistente")
    void validarToken_tokenInexistente_lancaExcecao() {
        when(tokenRepository.findByToken("token-invalido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validarToken("token-invalido"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("inválido ou expirado");
    }

    @Test
    @DisplayName("Deve lançar exceção para token já utilizado")
    void validarToken_tokenUsado_lancaExcecao() {
        PasswordResetToken token = new PasswordResetToken(
                "uuid", usuario, LocalDateTime.now().plusHours(1));
        token.setUsado(true);
        when(tokenRepository.findByToken("uuid")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.validarToken("uuid"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("já foi utilizado");
    }

    @Test
    @DisplayName("Deve lançar exceção para token expirado")
    void validarToken_tokenExpirado_lancaExcecao() {
        PasswordResetToken token = new PasswordResetToken(
                "uuid", usuario, LocalDateTime.now().minusHours(2)); // expirado
        when(tokenRepository.findByToken("uuid")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.validarToken("uuid"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    @DisplayName("Deve passar validação para token válido")
    void validarToken_tokenValido_naoLancaExcecao() {
        PasswordResetToken token = new PasswordResetToken(
                "uuid", usuario, LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByToken("uuid")).thenReturn(Optional.of(token));

        assertThatCode(() -> service.validarToken("uuid"))
                .doesNotThrowAnyException();
    }

    // ── confirmarReset ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve lançar exceção quando senhas não coincidem")
    void confirmarReset_senhasDiferentes_lancaExcecao() {
        assertThatThrownBy(() ->
                service.confirmarReset("uuid", "senha123", "senha456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("não coincidem");
    }

    @Test
    @DisplayName("Deve lançar exceção quando senha tem menos de 6 caracteres")
    void confirmarReset_senhaCurta_lancaExcecao() {
        assertThatThrownBy(() ->
                service.confirmarReset("uuid", "abc", "abc"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("mínimo 6 caracteres");
    }

    @Test
    @DisplayName("Deve redefinir senha e invalidar token com sucesso")
    void confirmarReset_sucesso_atualizaSenhaEInvalidaToken() {
        PasswordResetToken token = new PasswordResetToken(
                "uuid", usuario, LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByToken("uuid")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("novaSenha123")).thenReturn("hashNovo");

        service.confirmarReset("uuid", "novaSenha123", "novaSenha123");

        verify(usuarioRepository).save(usuario);
        verify(tokenRepository).save(token);
        assertThat(token.isUsado()).isTrue();
        assertThat(usuario.getSenha()).isEqualTo("hashNovo");
    }
}
