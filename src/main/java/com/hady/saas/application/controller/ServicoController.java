package com.hady.saas.application.controller;

import com.hady.saas.application.dto.ServicoDTO;
import com.hady.saas.business.service.ServicoService;
import com.hady.saas.business.service.UsuarioService;
import com.hady.saas.domain.servico.Servico;
import com.hady.saas.domain.usuario.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/painel/servicos")
public class ServicoController {

    private final ServicoService servicoService;
    private final UsuarioService usuarioService;

    public ServicoController(ServicoService servicoService, UsuarioService usuarioService) {
        this.servicoService = servicoService;
        this.usuarioService = usuarioService;
    }

    // GET /painel/servicos — lista serviços ativos
    // Todos os usuários logados acessam
    @GetMapping
    public ResponseEntity<List<Servico>> listar(@AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuarioLogado = usuarioService.buscarPorEmail(userDetails.getUsername());
        return ResponseEntity.ok(servicoService.listarAtivos(usuarioLogado.getEstabelecimento().getId()));
    }

    // POST /painel/servicos — cria novo serviço
    // Apenas ADMIN — FUNCIONARIO_TOP não acessa
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<String> criar(@Valid @RequestBody ServicoDTO dto,
                                        @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuarioLogado = usuarioService.buscarPorEmail(userDetails.getUsername());

        Servico servico = new Servico();
        servico.setNome(dto.getNome());
        servico.setDescricao(dto.getDescricao());
        servico.setDuracaoMinutos(dto.getDuracaoMinutos());
        servico.setPreco(dto.getPreco());
        servico.setCategoria(dto.getCategoria());
        servico.setEstabelecimento(usuarioLogado.getEstabelecimento());

        servicoService.criar(servico);
        return ResponseEntity.ok("Serviço criado com sucesso!");
    }

    // PUT /painel/servicos/{id} — atualiza serviço
    // Apenas ADMIN — FUNCIONARIO_TOP não acessa
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<String> atualizar(@PathVariable Long id,
                                            @Valid @RequestBody ServicoDTO dto,
                                            @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuarioLogado = usuarioService.buscarPorEmail(userDetails.getUsername());

        Servico dados = new Servico();
        dados.setNome(dto.getNome());
        dados.setDescricao(dto.getDescricao());
        dados.setDuracaoMinutos(dto.getDuracaoMinutos());
        dados.setPreco(dto.getPreco());
        dados.setCategoria(dto.getCategoria());

        servicoService.atualizar(id, dados, usuarioLogado.getEstabelecimento().getId());
        return ResponseEntity.ok("Serviço atualizado!");
    }

    // DELETE /painel/servicos/{id} — desativa serviço
    // Apenas ADMIN — FUNCIONARIO_TOP não acessa
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<String> desativar(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuarioLogado = usuarioService.buscarPorEmail(userDetails.getUsername());
        servicoService.desativar(id, usuarioLogado.getEstabelecimento().getId());
        return ResponseEntity.ok("Serviço desativado!");
    }
}
