package com.hady.saas.application.controller;

import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.horario.HorarioFuncionamento;
import com.hady.saas.infrastructure.repository.AgendamentoRepository;
import com.hady.saas.infrastructure.repository.ClienteRepository;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.repository.HorarioFuncionamentoRepository;
import com.hady.saas.infrastructure.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/app/{slug}")
public class PerfilPublicoController {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final HorarioFuncionamentoRepository horarioRepository;
    private final ClienteRepository clienteRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final JwtUtil jwtUtil;

    public PerfilPublicoController(EstabelecimentoRepository estabelecimentoRepository,
                                   HorarioFuncionamentoRepository horarioRepository,
                                   ClienteRepository clienteRepository,
                                   AgendamentoRepository agendamentoRepository,
                                   JwtUtil jwtUtil) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.horarioRepository = horarioRepository;
        this.clienteRepository = clienteRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.jwtUtil = jwtUtil;
    }

    // GET /app/{slug}/perfil
    @GetMapping("/perfil")
    public ResponseEntity<Map<String, Object>> perfil(@PathVariable String slug) {
        Estabelecimento est = buscar(slug);
        Map<String, Object> res = new HashMap<>();
        res.put("nome",        est.getNome());
        res.put("telefone",    est.getTelefone());
        res.put("endereco",    est.getEndereco());
        res.put("fotoPerfil",  est.getFotoPerfil());
        res.put("instagram",   est.getInstagram());
        res.put("whatsapp",    est.getWhatsappContato());
        res.put("facebook",    est.getFacebook());
        return ResponseEntity.ok(res);
    }

    // GET /app/{slug}/detalhes
    @GetMapping("/detalhes")
    public ResponseEntity<Map<String, Object>> detalhes(@PathVariable String slug) {
        Estabelecimento est = buscar(slug);
        List<HorarioFuncionamento> horarios = horarioRepository.findByEstabelecimentoId(est.getId());

        List<Map<String, Object>> horariosDto = horarios.stream().map(h -> {
            Map<String, Object> m = new HashMap<>();
            m.put("diaSemana",    h.getDiaSemana());
            m.put("abertura",     h.getHoraAbertura() != null ? h.getHoraAbertura().toString().substring(0, 5) : null);
            m.put("fechamento",   h.getHoraFechamento() != null ? h.getHoraFechamento().toString().substring(0, 5) : null);
            m.put("aberto",       h.isAberto());
            m.put("inicioAlmoco", h.getInicioAlmoco() != null ? h.getInicioAlmoco().toString().substring(0, 5) : null);
            m.put("fimAlmoco",    h.getFimAlmoco() != null ? h.getFimAlmoco().toString().substring(0, 5) : null);
            return m;
        }).toList();

        Map<String, Object> res = new HashMap<>();
        res.put("horarios",        horariosDto);
        res.put("endereco",        est.getEndereco());
        res.put("comodidades",     est.getComodidades());
        res.put("formasPagamento", est.getFormasPagamento());
        return ResponseEntity.ok(res);
    }

    // POST /app/{slug}/auth-cliente
    @PostMapping("/auth-cliente")
    public ResponseEntity<?> authCliente(@PathVariable String slug,
                                         @RequestBody Map<String, String> body) {
        String telefone = body.getOrDefault("telefone", "").replaceAll("\\D", "");
        if (telefone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", "Telefone obrigatório."));
        }

        Estabelecimento est = buscar(slug);

        Cliente cliente = clienteRepository
                .findByTelefoneAndEstabelecimentoId(telefone, est.getId())
                .orElse(null);

        if (cliente == null) {
            return ResponseEntity.status(404).body(Map.of("mensagem", "Nenhum histórico encontrado para este telefone."));
        }

        String token = jwtUtil.gerarTokenCliente(cliente.getId(), slug);

        return ResponseEntity.ok(Map.of(
                "token",     token,
                "clienteId", cliente.getId(),
                "nome",      cliente.getNome()
        ));
    }

    // GET /app/{slug}/meus-agendamentos
    @GetMapping("/meus-agendamentos")
    public ResponseEntity<?> meusAgendamentos(
            @PathVariable String slug,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("mensagem", "Token obrigatório."));
        }

        String token = authHeader.substring(7);
        Long clienteId;
        String slugDoToken;

        try {
            String subject = jwtUtil.extrairSubjectCliente(token);
            String[] parts = subject.split(":");
            clienteId   = Long.parseLong(parts[1]);
            slugDoToken = parts[2];
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("mensagem", "Token inválido."));
        }

        if (!slug.equals(slugDoToken)) {
            return ResponseEntity.status(403).body(Map.of("mensagem", "Token não pertence a este estabelecimento."));
        }

        Estabelecimento est = buscar(slug);

        List<Agendamento> agendamentos = agendamentoRepository
                .findTop20ByClienteIdOrderByDataHoraInicioDesc(clienteId, est.getId());

        List<Map<String, Object>> dto = agendamentos.stream().map(ag -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",           ag.getId());
            m.put("servico",      ag.getServico().getNome());
            m.put("profissional", ag.getProfissional().getNome());
            m.put("status",       ag.getStatus().name().toLowerCase());
            String[] ymd = ag.getDataHoraInicio().toLocalDate().toString().split("-");
            m.put("dataHora",     ymd[2] + "/" + ymd[1] + "/" + ymd[0] + " às " +
                    ag.getDataHoraInicio().toLocalTime().toString().substring(0, 5));
            return m;
        }).toList();

        return ResponseEntity.ok(dto);
    }

    private Estabelecimento buscar(String slug) {
        return estabelecimentoRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
    }
}
