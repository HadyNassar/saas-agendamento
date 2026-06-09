package com.hady.saas.application.controller;

import com.hady.saas.application.dto.FichaClienteDTO;
import com.hady.saas.application.dto.FotoClienteDTO;
import com.hady.saas.application.dto.FotoProfissionalDTO;
import com.hady.saas.business.service.FotoService;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/painel")
public class FotoPainelController {

    private final FotoService fotoService;
    private final AuthUtil authUtil;

    public FotoPainelController(FotoService fotoService, AuthUtil authUtil) {
        this.fotoService = fotoService;
        this.authUtil = authUtil;
    }

    // ─── SERVE ARQUIVO AUTENTICADO — FOTO DO CLIENTE ─────────────────────────

    @GetMapping("/fotos/clientes/{fotoId}/serve")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<Resource> serveArquivoCliente(
            @PathVariable Long fotoId,
            Authentication auth) throws IOException {

        Resource resource = fotoService.serveArquivoCliente(fotoId, authUtil.getEstabelecimentoId(auth));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    // ─── SERVE ARQUIVO AUTENTICADO — FOTO DO PROFISSIONAL (painel) ───────────

    @GetMapping("/fotos/profissionais/{fotoId}/serve")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<Resource> serveArquivoProfissional(
            @PathVariable Long fotoId,
            Authentication auth) throws IOException {

        Resource resource = fotoService.serveArquivoProfissional(fotoId, authUtil.getEstabelecimentoId(auth));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    // ─── FICHA COMPLETA DO CLIENTE ────────────────────────────────────────────

    @GetMapping("/clientes/{clienteId}/ficha")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<FichaClienteDTO> fichaCliente(
            @PathVariable Long clienteId,
            Authentication auth) {
        return ResponseEntity.ok(
                fotoService.montarFichaCliente(clienteId, authUtil.getEstabelecimentoId(auth)));
    }

    // ─── FOTOS DO CLIENTE ─────────────────────────────────────────────────────

    @GetMapping("/clientes/{clienteId}/fotos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<List<FotoClienteDTO>> listarFotosCliente(
            @PathVariable Long clienteId,
            Authentication auth) {
        return ResponseEntity.ok(
                fotoService.listarFotosCliente(clienteId, authUtil.getEstabelecimentoId(auth)));
    }

    @PostMapping("/clientes/{clienteId}/fotos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<FotoClienteDTO> adicionarFotoCliente(
            @PathVariable Long clienteId,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam("tipo") String tipo,
            Authentication auth) throws IOException {
        return ResponseEntity.ok(
                fotoService.adicionarFotoCliente(
                        clienteId, authUtil.getEstabelecimentoId(auth), arquivo, tipo));
    }

    @DeleteMapping("/clientes/{clienteId}/fotos/{fotoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<Void> removerFotoCliente(
            @PathVariable Long clienteId,
            @PathVariable Long fotoId,
            Authentication auth) throws IOException {
        fotoService.removerFotoCliente(fotoId, authUtil.getEstabelecimentoId(auth));
        return ResponseEntity.noContent().build();
    }

    // ─── FOTOS DO PROFISSIONAL (painel) ──────────────────────────────────────

    @GetMapping("/profissionais/{profissionalId}/fotos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<List<FotoProfissionalDTO>> listarFotosProfissional(
            @PathVariable Long profissionalId,
            Authentication auth) {
        return ResponseEntity.ok(
                fotoService.listarFotosProfissional(
                        profissionalId, authUtil.getEstabelecimentoId(auth)));
    }

    @PostMapping("/profissionais/{profissionalId}/fotos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<FotoProfissionalDTO> adicionarFotoProfissional(
            @PathVariable Long profissionalId,
            @RequestParam("arquivo") MultipartFile arquivo,
            Authentication auth) throws IOException {
        return ResponseEntity.ok(
                fotoService.adicionarFotoProfissional(
                        profissionalId, authUtil.getEstabelecimentoId(auth), arquivo));
    }

    @DeleteMapping("/profissionais/{profissionalId}/fotos/{fotoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<Void> removerFotoProfissional(
            @PathVariable Long profissionalId,
            @PathVariable Long fotoId,
            Authentication auth) throws IOException {
        fotoService.removerFotoProfissional(fotoId, authUtil.getEstabelecimentoId(auth));
        return ResponseEntity.noContent().build();
    }
}
