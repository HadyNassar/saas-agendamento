package com.hady.saas.application.controller;

import com.hady.saas.application.dto.AtualizarComissaoDTO;
import com.hady.saas.application.dto.CadastrarProfissionalDTO;
import com.hady.saas.application.dto.ProfissionalResponseDTO;
import com.hady.saas.business.service.ProfissionalService;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.profissional.Especialidade;
import com.hady.saas.domain.profissional.Profissional;
import com.hady.saas.domain.servico.Servico;
import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.security.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/painel/profissionais")
public class ProfissionalController {

    private final ProfissionalService profissionalService;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final AuthUtil authUtil;

    public ProfissionalController(ProfissionalService profissionalService,
                                  EstabelecimentoRepository estabelecimentoRepository,
                                  AuthUtil authUtil) {
        this.profissionalService = profissionalService;
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.authUtil = authUtil;
    }

    private Estabelecimento getEstab(Authentication auth) {
        return estabelecimentoRepository.findById(authUtil.getEstabelecimentoId(auth))
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<List<ProfissionalResponseDTO>> listar(Authentication auth) {
        return ResponseEntity.ok(
            profissionalService.listarAtivos(authUtil.getEstabelecimentoId(auth))
                .stream()
                .map(ProfissionalResponseDTO::new)
                .toList()
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> cadastrar(@Valid @RequestBody CadastrarProfissionalDTO dto, Authentication auth) {
        Estabelecimento estabelecimento = getEstab(auth);

        Usuario usuarioLogin = new Usuario();
        usuarioLogin.setNome(dto.getNome());
        usuarioLogin.setEmail(dto.getEmail());
        usuarioLogin.setSenha(dto.getSenha());
        usuarioLogin.setRole(dto.getRole());
        usuarioLogin.setEstabelecimento(estabelecimento);

        Profissional profissional = new Profissional();
        profissional.setNome(dto.getNome());
        profissional.setTelefone(dto.getTelefone());
        profissional.setEstabelecimento(estabelecimento);

        profissionalService.cadastrar(profissional, usuarioLogin);

        if (dto.getEspecialidadeIds() != null && !dto.getEspecialidadeIds().isEmpty()) {
            profissionalService.atualizarEspecialidades(profissional.getId(), dto.getEspecialidadeIds(), estabelecimento.getId());
        }
        if (dto.getServicosIds() != null && !dto.getServicosIds().isEmpty()) {
            profissionalService.atualizarServicos(profissional.getId(), dto.getServicosIds(), estabelecimento.getId());
        }
        return ResponseEntity.ok("Profissional cadastrado com sucesso!");
    }

    @PatchMapping("/{id}/comissao")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> atualizarComissao(@PathVariable Long id,
                                                     @RequestBody AtualizarComissaoDTO dto,
                                                     Authentication auth) {
        profissionalService.atualizarComissao(id, authUtil.getEstabelecimentoId(auth), dto.getComissaoPercent());
        return ResponseEntity.ok("Comissão atualizada!");
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> desativar(@PathVariable Long id, Authentication auth) {
        profissionalService.alterarStatus(id, authUtil.getEstabelecimentoId(auth), false);
        return ResponseEntity.ok("Profissional desativado!");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> excluir(@PathVariable Long id, Authentication auth) {
        profissionalService.excluir(id, authUtil.getEstabelecimentoId(auth));
        return ResponseEntity.ok("Profissional excluído com sucesso!");
    }

    @GetMapping("/especialidades")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<List<Especialidade>> listarEspecialidades(Authentication auth) {
        return ResponseEntity.ok(profissionalService.listarEspecialidades(authUtil.getEstabelecimentoId(auth)));
    }

    @PostMapping("/especialidades")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> criarEspecialidade(@RequestBody String nome, Authentication auth) {
        profissionalService.criarEspecialidade(nome, getEstab(auth));
        return ResponseEntity.ok("Especialidade criada!");
    }

    /**
     * GET /painel/profissionais/{id}/servicos
     *
     * Retorna os serviços vinculados a um profissional.
     * Usado pelo painel para preencher os checkboxes de vínculo.
     * Como a relação é N×N, o mesmo serviço pode aparecer em vários profissionais.
     */
    @GetMapping("/{id}/servicos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<List<Servico>> listarServicosDoProfissional(@PathVariable Long id,
                                                                       Authentication auth) {
        return ResponseEntity.ok(
                profissionalService.listarServicosDoProfissional(id, authUtil.getEstabelecimentoId(auth))
        );
    }

    /**
     * PUT /painel/profissionais/{id}/servicos
     *
     * Vincula a lista de servicoIds ao profissional (substitui completamente o vínculo atual).
     * Relação N×N: múltiplos profissionais podem ter o mesmo serviço vinculado.
     * Ex: "Corte feminino" vinculado a Ana e a João ao mesmo tempo.
     * Apenas ADMIN pode alterar vínculos.
     */
    @PutMapping("/{id}/servicos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> atualizarServicos(@PathVariable Long id,
                                                     @RequestBody List<Long> servicoIds,
                                                     Authentication auth) {
        profissionalService.atualizarServicos(id, servicoIds, authUtil.getEstabelecimentoId(auth));
        return ResponseEntity.ok("Serviços vinculados com sucesso!");
    }
}
