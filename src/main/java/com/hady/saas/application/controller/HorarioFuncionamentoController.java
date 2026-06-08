package com.hady.saas.application.controller;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.horario.HorarioFuncionamento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.repository.HorarioFuncionamentoRepository;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/painel/horarios")
public class HorarioFuncionamentoController {

    private final HorarioFuncionamentoRepository horarioRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final AuthUtil authUtil;

    public HorarioFuncionamentoController(HorarioFuncionamentoRepository horarioRepository,
                                          EstabelecimentoRepository estabelecimentoRepository,
                                          AuthUtil authUtil) {
        this.horarioRepository = horarioRepository;
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.authUtil = authUtil;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<List<HorarioFuncionamento>> listar(Authentication auth) {
        return ResponseEntity.ok(horarioRepository.findByEstabelecimentoId(authUtil.getEstabelecimentoId(auth)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> salvar(@RequestBody HorarioFuncionamento horario, Authentication auth) {
        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        Estabelecimento estab = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
        horario.setEstabelecimento(estab);
        horarioRepository
                .findByEstabelecimentoIdAndDiaSemana(estabelecimentoId, horario.getDiaSemana())
                .ifPresent(existente -> horario.setId(existente.getId()));
        horarioRepository.save(horario);
        return ResponseEntity.ok("Horário salvo!");
    }
}
