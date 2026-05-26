package com.hady.saas.application.controller;

import com.hady.saas.application.dto.AlterarEmailDTO;
import com.hady.saas.application.dto.AlterarSenhaDTO;
import com.hady.saas.application.dto.EditarEstabelecimentoDTO;
import com.hady.saas.application.dto.EstabelecimentoResponseDTO;
import com.hady.saas.business.service.ConfiguracaoService;
import com.hady.saas.business.service.UsuarioService;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.usuario.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/painel/configuracoes")
public class ConfiguracaoController {

    private final ConfiguracaoService configuracaoService;
    private final UsuarioService usuarioService;

    public ConfiguracaoController(ConfiguracaoService configuracaoService,
                                   UsuarioService usuarioService) {
        this.configuracaoService = configuracaoService;
        this.usuarioService = usuarioService;
    }

    // GET /painel/configuracoes/estabelecimento
    @GetMapping("/estabelecimento")
    public ResponseEntity<EstabelecimentoResponseDTO> buscarEstabelecimento(
            @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        Estabelecimento est = configuracaoService.buscarEstabelecimento(usuario.getEstabelecimento().getId());

        EstabelecimentoResponseDTO dto = new EstabelecimentoResponseDTO(
                est.getId(), est.getNome(), est.getSlug(),
                est.getTelefone(), est.getEndereco(),
                est.getMesesAntecipacao(), est.isWhatsappConectado()
        );
        return ResponseEntity.ok(dto);
    }

    // PUT /painel/configuracoes/estabelecimento — apenas ADMIN
    @PutMapping("/estabelecimento")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> editarEstabelecimento(
            @Valid @RequestBody EditarEstabelecimentoDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        configuracaoService.editarEstabelecimento(
                usuario.getEstabelecimento().getId(),
                dto.getNome(),
                dto.getTelefone(),
                dto.getEndereco(),
                dto.getMesesAntecipacao()   // ← novo campo
        );
        return ResponseEntity.ok("Dados do estabelecimento atualizados!");
    }

    // PUT /painel/configuracoes/email
    @PutMapping("/email")
    public ResponseEntity<String> alterarEmail(
            @Valid @RequestBody AlterarEmailDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        configuracaoService.alterarEmail(usuario.getId(), dto.getNovoEmail(), dto.getSenhaAtual());
        return ResponseEntity.ok("Email alterado com sucesso! Faça login novamente com o novo email.");
    }

    // PUT /painel/configuracoes/senha
    @PutMapping("/senha")
    public ResponseEntity<String> alterarSenha(
            @Valid @RequestBody AlterarSenhaDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        configuracaoService.alterarSenha(
                usuario.getId(),
                dto.getSenhaAtual(),
                dto.getNovaSenha(),
                dto.getConfirmarNovaSenha()
        );
        return ResponseEntity.ok("Senha alterada com sucesso! Faça login novamente.");
    }
}
