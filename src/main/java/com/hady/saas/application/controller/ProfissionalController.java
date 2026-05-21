    package com.hady.saas.application.controller;
    
    import com.hady.saas.application.dto.CadastrarProfissionalDTO;
    import com.hady.saas.business.service.ProfissionalService;
    import com.hady.saas.business.service.UsuarioService;
    import com.hady.saas.domain.estabelecimento.Estabelecimento;
    import com.hady.saas.domain.profissional.Especialidade;
    import com.hady.saas.domain.profissional.Profissional;
    import com.hady.saas.domain.usuario.Usuario;
    import jakarta.validation.Valid;
    import org.springframework.http.ResponseEntity;
    import org.springframework.security.access.prepost.PreAuthorize;
    import org.springframework.security.core.annotation.AuthenticationPrincipal;
    import org.springframework.security.core.userdetails.UserDetails;
    import org.springframework.web.bind.annotation.*;
    
    import java.util.List;
    
    @RestController
    @RequestMapping("/painel/profissionais")
    public class ProfissionalController {
    
        private final ProfissionalService profissionalService;
        private final UsuarioService usuarioService;
    
        public ProfissionalController(ProfissionalService profissionalService,
                                      UsuarioService usuarioService) {
            this.profissionalService = profissionalService;
            this.usuarioService = usuarioService;
        }
    
        // GET /painel/profissionais — lista profissionais ativos do estabelecimento
        // ADMIN e FUNCIONARIO acessam | FUNCIONARIO_TOP não acessa
        @GetMapping
        @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
        public ResponseEntity<List<Profissional>> listar(@AuthenticationPrincipal UserDetails userDetails) {
            Usuario usuarioLogado = usuarioService.buscarPorEmail(userDetails.getUsername());
            Long estabelecimentoId = usuarioLogado.getEstabelecimento().getId();
            return ResponseEntity.ok(profissionalService.listarAtivos(estabelecimentoId));
        }
    
        // POST /painel/profissionais — cadastra profissional + usuário de login
        // Apenas ADMIN pode cadastrar
        @PostMapping
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<String> cadastrar(@Valid @RequestBody CadastrarProfissionalDTO dto,
                                                @AuthenticationPrincipal UserDetails userDetails) {
    
            Usuario adminLogado = usuarioService.buscarPorEmail(userDetails.getUsername());
            Estabelecimento estabelecimento = adminLogado.getEstabelecimento();
    
            // Monta o usuário de login do profissional
            Usuario usuarioLogin = new Usuario();
            usuarioLogin.setNome(dto.getNome());
            usuarioLogin.setEmail(dto.getEmail());
            usuarioLogin.setSenha(dto.getSenha());
            usuarioLogin.setRole(dto.getRole());
            usuarioLogin.setEstabelecimento(estabelecimento);
    
            // Monta o profissional
            Profissional profissional = new Profissional();
            profissional.setNome(dto.getNome());
            profissional.setTelefone(dto.getTelefone());
            profissional.setEstabelecimento(estabelecimento);
    
            profissionalService.cadastrar(profissional, usuarioLogin);
    
            // Atualiza especialidades se informadas
            if (dto.getEspecialidadeIds() != null && !dto.getEspecialidadeIds().isEmpty()) {
                profissionalService.atualizarEspecialidades(profissional.getId(), dto.getEspecialidadeIds(), estabelecimento.getId());
            }
    
            return ResponseEntity.ok("Profissional cadastrado com sucesso!");
        }
    
        // PATCH /painel/profissionais/{id}/desativar — desativa profissional
        // Apenas ADMIN pode desativar
        @PatchMapping("/{id}/desativar")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<String> desativar(@PathVariable Long id,
                                                @AuthenticationPrincipal UserDetails userDetails) {
            Usuario adminLogado = usuarioService.buscarPorEmail(userDetails.getUsername());
            profissionalService.alterarStatus(id, adminLogado.getEstabelecimento().getId(), false);
            return ResponseEntity.ok("Profissional desativado!");
        }
    
        // GET /painel/profissionais/especialidades — lista especialidades do estabelecimento
        @GetMapping("/especialidades")
        @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
        public ResponseEntity<List<Especialidade>> listarEspecialidades(@AuthenticationPrincipal UserDetails userDetails) {
            Usuario usuarioLogado = usuarioService.buscarPorEmail(userDetails.getUsername());
            return ResponseEntity.ok(profissionalService.listarEspecialidades(usuarioLogado.getEstabelecimento().getId()));
        }
    
        // POST /painel/profissionais/especialidades — cria especialidade nova
        // Apenas ADMIN pode criar — FUNCIONARIO_TOP não acessa
        @PostMapping("/especialidades")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<String> criarEspecialidade(@RequestBody String nome,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
            Usuario adminLogado = usuarioService.buscarPorEmail(userDetails.getUsername());
            profissionalService.criarEspecialidade(nome, adminLogado.getEstabelecimento());
            return ResponseEntity.ok("Especialidade criada!");
        }
    }
