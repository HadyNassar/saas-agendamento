package com.hady.saas.application.controller;

import com.hady.saas.application.dto.AlterarEmailDTO;
import com.hady.saas.application.dto.AlterarSenhaDTO;
import com.hady.saas.application.dto.EditarEstabelecimentoDTO;
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

    // GET /painel/configuracoes/estabelecimento — dados do estabelecimento
    @GetMapping("/estabelecimento")
    public ResponseEntity<Estabelecimento> buscarEstabelecimento(
            @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        Estabelecimento estabelecimento = configuracaoService
                .buscarEstabelecimento(usuario.getEstabelecimento().getId());
        return ResponseEntity.ok(estabelecimento);
    }

    // PUT /painel/configuracoes/estabelecimento — edita dados do estabelecimento
    // Apenas ADMIN pode editar
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
                dto.getEndereco()
        );
        return ResponseEntity.ok("Dados do estabelecimento atualizados!");
    }

    // PUT /painel/configuracoes/email — altera email do usuário logado
    // Disponível para todos os usuários
    @PutMapping("/email")
    public ResponseEntity<String> alterarEmail(
            @Valid @RequestBody AlterarEmailDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        configuracaoService.alterarEmail(usuario.getId(), dto.getNovoEmail(), dto.getSenhaAtual());
        return ResponseEntity.ok("Email alterado com sucesso! Faça login novamente com o novo email.");
    }

    // PUT /painel/configuracoes/senha — altera senha do usuário logado
    // Disponível para todos os usuários
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
