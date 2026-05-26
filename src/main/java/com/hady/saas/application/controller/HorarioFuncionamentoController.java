package com.hady.saas.application.controller;

import com.hady.saas.business.service.UsuarioService;
import com.hady.saas.domain.horario.HorarioFuncionamento;
import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.repository.HorarioFuncionamentoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/painel/horarios")
public class HorarioFuncionamentoController {

    private final HorarioFuncionamentoRepository horarioRepository;
    private final UsuarioService usuarioService;

    public HorarioFuncionamentoController(HorarioFuncionamentoRepository horarioRepository,
                                          UsuarioService usuarioService) {
        this.horarioRepository = horarioRepository;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<HorarioFuncionamento>> listar(@AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        return ResponseEntity.ok(horarioRepository.findByEstabelecimentoId(usuario.getEstabelecimento().getId()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> salvar(@RequestBody HorarioFuncionamento horario,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        horario.setEstabelecimento(usuario.getEstabelecimento());

        horarioRepository
                .findByEstabelecimentoIdAndDiaSemana(
                        usuario.getEstabelecimento().getId(),
                        horario.getDiaSemana()
                )
                .ifPresent(existente -> horario.setId(existente.getId()));

        horarioRepository.save(horario);
        return ResponseEntity.ok("Horário salvo!");
    }
}