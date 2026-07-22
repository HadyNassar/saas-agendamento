package com.hady.saas.application.controller;

import com.hady.saas.application.dto.ServicoDTO;
import com.hady.saas.business.service.ServicoService;
import com.hady.saas.domain.servico.Servico;
import com.hady.saas.infrastructure.security.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/painel/servicos")
public class ServicoController {

    private final ServicoService servicoService;
    private final AuthUtil authUtil;

    public ServicoController(ServicoService servicoService, AuthUtil authUtil) {
        this.servicoService = servicoService;
        this.authUtil = authUtil;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<List<Servico>> listar(Authentication auth) {
        return ResponseEntity.ok(servicoService.listarAtivos(authUtil.getEstabelecimentoId(auth)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Servico> criar(@Valid @RequestBody ServicoDTO dto, Authentication auth) {
        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        Servico servico = montarServico(dto);
        // estabelecimento é resolvido no service via id
        // Retorna o serviço criado (com id) para o front poder vincular
        // profissionais logo em seguida, sem precisar recarregar a lista.
        Servico salvo = servicoService.criarComEstabelecimentoId(servico, dto, estabelecimentoId);
        return ResponseEntity.ok(salvo);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<String> atualizar(@PathVariable Long id,
                                            @Valid @RequestBody ServicoDTO dto,
                                            Authentication auth) {
        Servico dados = montarServico(dto);
        servicoService.atualizar(id, dados, authUtil.getEstabelecimentoId(auth));
        return ResponseEntity.ok("Serviço atualizado!");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<String> desativar(@PathVariable Long id, Authentication auth) {
        servicoService.desativar(id, authUtil.getEstabelecimentoId(auth));
        return ResponseEntity.ok("Serviço desativado!");
    }

    private Servico montarServico(ServicoDTO dto) {
        Servico s = new Servico();
        s.setNome(dto.getNome());
        s.setDescricao(dto.getDescricao());
        s.setDuracaoMinutos(dto.getDuracaoMinutos());
        s.setPreco(dto.getPreco());
        s.setCategoria(dto.getCategoria());
        s.setCustoMaterialFixo(dto.getCustoMaterialFixo() != null ? dto.getCustoMaterialFixo() : BigDecimal.ZERO);
        s.setCustoMaterialPercent(dto.getCustoMaterialPercent() != null ? dto.getCustoMaterialPercent() : BigDecimal.ZERO);
        return s;
    }
}