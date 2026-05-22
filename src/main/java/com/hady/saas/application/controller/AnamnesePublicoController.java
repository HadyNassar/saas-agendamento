package com.hady.saas.application.controller;

import com.hady.saas.application.dto.PreencherAnamneseDTO;
import com.hady.saas.business.service.AnamneseService;
import com.hady.saas.business.service.ClienteService;
import com.hady.saas.domain.anamnese.PerguntaAnamnese;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/app/{slug}/anamnese")
public class AnamnesePublicoController {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final ClienteService clienteService;
    private final AnamneseService anamneseService;

    public AnamnesePublicoController(EstabelecimentoRepository estabelecimentoRepository,
                                      ClienteService clienteService,
                                      AnamneseService anamneseService) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.clienteService = clienteService;
        this.anamneseService = anamneseService;
    }

    // GET /app/{slug}/anamnese/perguntas — lista perguntas ativas para o cliente responder
    @GetMapping("/perguntas")
    public ResponseEntity<List<PerguntaAnamnese>> listarPerguntas(@PathVariable String slug) {
        Estabelecimento estabelecimento = buscarEstabelecimento(slug);
        return ResponseEntity.ok(anamneseService.listarPerguntasAtivas(estabelecimento.getId()));
    }

    // GET /app/{slug}/anamnese/status/{clienteId} — verifica se cliente já preencheu
    @GetMapping("/status/{clienteId}")
    public ResponseEntity<Boolean> verificarStatus(@PathVariable String slug,
                                                    @PathVariable Long clienteId) {
        buscarEstabelecimento(slug); // valida o estabelecimento
        return ResponseEntity.ok(anamneseService.clienteJaPreencheu(clienteId));
    }

    // POST /app/{slug}/anamnese/{clienteId} — cliente preenche anamnese (só uma vez)
    @PostMapping("/{clienteId}")
    public ResponseEntity<String> preencher(@PathVariable String slug,
                                             @PathVariable Long clienteId,
                                             @RequestBody PreencherAnamneseDTO dto) {

        Estabelecimento estabelecimento = buscarEstabelecimento(slug);
        Cliente cliente = clienteService.buscarPorId(clienteId, estabelecimento.getId());

        // Converte DTO para Map que o service espera
        Map<Long, AnamneseService.RespostaDTO> respostas = new HashMap<>();
        dto.getRespostas().forEach((perguntaId, item) -> {
            AnamneseService.RespostaDTO respostaDTO = new AnamneseService.RespostaDTO();
            respostaDTO.setResposta(item.getResposta());
            respostaDTO.setDetalhe(item.getDetalhe());
            respostas.put(perguntaId, respostaDTO);
        });

        anamneseService.preencherAnamnese(cliente, respostas);
        return ResponseEntity.ok("Anamnese preenchida com sucesso!");
    }

    private Estabelecimento buscarEstabelecimento(String slug) {
        return estabelecimentoRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
    }
}
