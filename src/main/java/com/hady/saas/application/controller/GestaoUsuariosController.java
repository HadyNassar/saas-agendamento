package com.hady.saas.application.controller;

import com.hady.saas.application.dto.EditarEstabelecimentoAdminDTO;
import com.hady.saas.application.dto.EstabelecimentoAdminDTO;
import com.hady.saas.application.dto.EstabelecimentoListaAdminDTO;
import com.hady.saas.application.dto.RedefinirSenhaAdminDTO;
import com.hady.saas.application.dto.UsuarioAdminDTO;
import com.hady.saas.business.service.GestaoUsuariosService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// Gestão completa de estabelecimentos pelo admin master. Protegido em
// SecurityConfig via .requestMatchers("/admin-master/**").hasRole("ADMIN_MASTER")
@RestController
@RequestMapping("/admin-master/estabelecimentos")
public class GestaoUsuariosController {

    private final GestaoUsuariosService gestaoUsuariosService;

    public GestaoUsuariosController(GestaoUsuariosService gestaoUsuariosService) {
        this.gestaoUsuariosService = gestaoUsuariosService;
    }

    // Lista com filtros combináveis — todos opcionais.
    // GET /admin-master/estabelecimentos?busca=studio&plano=ELITE&cidade=Campo Grande&status=ATIVO
    @GetMapping
    public List<EstabelecimentoListaAdminDTO> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String plano,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String status) {
        return gestaoUsuariosService.listar(busca, plano, cidade, status);
    }

    // Detalhe completo de um estabelecimento.
    @GetMapping("/{id}")
    public EstabelecimentoAdminDTO buscarDetalhe(@PathVariable Long id) {
        return gestaoUsuariosService.buscarDetalhe(id);
    }

    // Lista os usuários vinculados a este estabelecimento — usado no
    // seletor de "Impersonar conta" para evitar digitar um ID no escuro.
    // GET /admin-master/estabelecimentos/{id}/usuarios
    @GetMapping("/{id}/usuarios")
    public List<UsuarioAdminDTO> listarUsuarios(@PathVariable Long id) {
        return gestaoUsuariosService.listarUsuarios(id);
    }

    // Define uma nova senha para um usuário do estabelecimento (cliente que
    // esqueceu a senha). Invalida as sessões antigas e registra auditoria.
    // POST /admin-master/estabelecimentos/{id}/usuarios/{usuarioId}/senha
    @PostMapping("/{id}/usuarios/{usuarioId}/senha")
    public ResponseEntity<String> redefinirSenha(@PathVariable Long id,
                                                 @PathVariable Long usuarioId,
                                                 @Valid @RequestBody RedefinirSenhaAdminDTO dto,
                                                 Authentication auth) {
        gestaoUsuariosService.redefinirSenhaUsuario(id, usuarioId, dto.getNovaSenha(), auth.getName());
        return ResponseEntity.ok("Senha redefinida com sucesso.");
    }

    // Edição administrativa — qualquer campo, incluindo endereço completo.
    @PutMapping("/{id}")
    public ResponseEntity<String> editar(@PathVariable Long id,
                                         @Valid @RequestBody EditarEstabelecimentoAdminDTO dto) {
        gestaoUsuariosService.editar(id, dto);
        return ResponseEntity.ok("Estabelecimento atualizado com sucesso.");
    }

    // Ativa ou desativa a conta imediatamente.
    @PatchMapping("/{id}/status")
    public ResponseEntity<String> ativarDesativar(@PathVariable Long id,
                                                  @RequestBody Map<String, Boolean> body) {
        Boolean ativo = body.get("ativo");
        if (ativo == null) {
            return ResponseEntity.badRequest().body("Campo 'ativo' é obrigatório.");
        }
        gestaoUsuariosService.ativarDesativar(id, ativo);
        return ResponseEntity.ok(ativo ? "Conta ativada." : "Conta desativada.");
    }

    // Troca o plano manualmente (ex: upgrade cortesia, correção de cobrança).
    @PatchMapping("/{id}/plano")
    public ResponseEntity<String> trocarPlano(@PathVariable Long id,
                                              @RequestBody Map<String, String> body) {
        String plano = body.get("plano");
        String validoAteStr = body.get("validoAte"); // formato "yyyy-MM-dd"

        if (plano == null || validoAteStr == null) {
            return ResponseEntity.badRequest().body("Campos 'plano' e 'validoAte' são obrigatórios.");
        }

        LocalDate validoAte = LocalDate.parse(validoAteStr);
        gestaoUsuariosService.trocarPlano(id, plano, validoAte);
        return ResponseEntity.ok("Plano atualizado.");
    }

    // Adiciona dias extras de trial (bônus, cortesia, resolução de suporte).
    @PatchMapping("/{id}/trial")
    public ResponseEntity<String> adicionarDiasTrial(@PathVariable Long id,
                                                     @RequestBody Map<String, Integer> body) {
        Integer dias = body.get("dias");
        if (dias == null || dias <= 0) {
            return ResponseEntity.badRequest().body("Campo 'dias' deve ser um número positivo.");
        }
        gestaoUsuariosService.adicionarDiasTrial(id, dias);
        return ResponseEntity.ok(dias + " dias adicionados ao trial.");
    }
}