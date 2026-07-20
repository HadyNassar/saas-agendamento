package com.hady.saas.application.controller;

import com.hady.saas.application.dto.CriarNotaInternaDTO;
import com.hady.saas.application.dto.LogAuditoriaDTO;
import com.hady.saas.application.dto.NotaInternaDTO;
import com.hady.saas.business.service.NotasAuditoriaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Notas internas e auditoria — protegido via
// .requestMatchers("/admin-master/**").hasRole("ADMIN_MASTER")
@RestController
@RequestMapping("/admin-master")
public class NotasAuditoriaController {

    private final NotasAuditoriaService notasAuditoriaService;

    public NotasAuditoriaController(NotasAuditoriaService notasAuditoriaService) {
        this.notasAuditoriaService = notasAuditoriaService;
    }

    // ── Notas internas ────────────────────────────────────────────────

    @PostMapping("/estabelecimentos/{id}/notas")
    public ResponseEntity<NotaInternaDTO> adicionarNota(@PathVariable Long id,
                                                        @Valid @RequestBody CriarNotaInternaDTO dto,
                                                        Authentication auth) {
        String adminEmail = auth.getName(); // subject do JWT = email do admin master
        NotaInternaDTO nota = notasAuditoriaService.adicionarNota(id, dto.getTexto(), adminEmail);

        notasAuditoriaService.registrar(adminEmail, "ADICIONAR_NOTA", id,
                "Nota adicionada: " + resumir(dto.getTexto()));

        return ResponseEntity.ok(nota);
    }

    @GetMapping("/estabelecimentos/{id}/notas")
    public List<NotaInternaDTO> listarNotas(@PathVariable Long id) {
        return notasAuditoriaService.listarNotas(id);
    }

    @DeleteMapping("/notas/{notaId}")
    public ResponseEntity<String> excluirNota(@PathVariable Long notaId, Authentication auth) {
        notasAuditoriaService.excluirNota(notaId);
        notasAuditoriaService.registrar(auth.getName(), "EXCLUIR_NOTA", null, "Nota id " + notaId + " excluída");
        return ResponseEntity.ok("Nota excluída.");
    }

    // ── Log de auditoria ──────────────────────────────────────────────

    // GET /admin-master/auditoria?estabelecimentoId=&acao=
    @GetMapping("/auditoria")
    public List<LogAuditoriaDTO> listarAuditoria(@RequestParam(required = false) Long estabelecimentoId,
                                                 @RequestParam(required = false) String acao) {
        return notasAuditoriaService.listarAuditoria(estabelecimentoId, acao);
    }

    private String resumir(String texto) {
        if (texto == null) return "";
        return texto.length() > 80 ? texto.substring(0, 80) + "..." : texto;
    }
}