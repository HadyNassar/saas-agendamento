package com.hady.saas.application.controller;

import com.hady.saas.application.dto.PerguntaAnamneseDTO;
import com.hady.saas.application.dto.PerguntaAnamneseResposta;
import com.hady.saas.application.dto.PreencherAnamneseDTO;
import com.hady.saas.business.service.AnamneseService;
import com.hady.saas.business.service.ClienteService;
import com.hady.saas.domain.anamnese.RespostaAnamnese;
import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.security.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/painel/anamnese")
public class AnamneseController {

    private final AnamneseService anamneseService;
    private final ClienteService clienteService;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final AuthUtil authUtil;

    public AnamneseController(AnamneseService anamneseService,
                              ClienteService clienteService,
                              EstabelecimentoRepository estabelecimentoRepository,
                              AuthUtil authUtil) {
        this.anamneseService = anamneseService;
        this.clienteService = clienteService;
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.authUtil = authUtil;
    }

    @GetMapping("/perguntas")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<List<PerguntaAnamneseResposta>> listarPerguntas(Authentication auth) {
        List<PerguntaAnamneseResposta> perguntas = anamneseService
                .listarTodasPerguntas(authUtil.getEstabelecimentoId(auth))
                .stream()
                .map(PerguntaAnamneseResposta::new)
                .toList();
        return ResponseEntity.ok(perguntas);
    }

    @PostMapping("/perguntas")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<PerguntaAnamneseResposta> criarPergunta(@Valid @RequestBody PerguntaAnamneseDTO dto,
                                                                  Authentication auth) {
        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        validarPermissaoEdicao(auth, estabelecimentoId);
        Estabelecimento estab = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
        PerguntaAnamneseResposta criada = new PerguntaAnamneseResposta(
                anamneseService.criarPergunta(dto.getTexto(), dto.getTipo(), dto.getOrdem(), estab)
        );
        return ResponseEntity.ok(criada);
    }

    @PutMapping("/perguntas/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<PerguntaAnamneseResposta> editarPergunta(@PathVariable Long id,
                                                                   @Valid @RequestBody PerguntaAnamneseDTO dto,
                                                                   Authentication auth) {
        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        validarPermissaoEdicao(auth, estabelecimentoId);
        PerguntaAnamneseResposta atualizada = new PerguntaAnamneseResposta(
                anamneseService.editarPergunta(id, dto.getTexto(), dto.getTipo(), dto.getOrdem(), estabelecimentoId)
        );
        return ResponseEntity.ok(atualizada);
    }

    @PatchMapping("/perguntas/{id}/desativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<String> desativarPergunta(@PathVariable Long id, Authentication auth) {
        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        validarPermissaoEdicao(auth, estabelecimentoId);
        anamneseService.desativarPergunta(id, estabelecimentoId);
        return ResponseEntity.ok("Pergunta desativada!");
    }

    @PatchMapping("/perguntas/{id}/ativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<String> ativarPergunta(@PathVariable Long id, Authentication auth) {
        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        validarPermissaoEdicao(auth, estabelecimentoId);
        anamneseService.ativarPergunta(id, estabelecimentoId);
        return ResponseEntity.ok("Pergunta ativada!");
    }

    @DeleteMapping("/perguntas/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<String> excluirPergunta(@PathVariable Long id, Authentication auth) {
        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        validarPermissaoEdicao(auth, estabelecimentoId);
        anamneseService.excluirPergunta(id, estabelecimentoId);
        return ResponseEntity.ok("Pergunta excluída!");
    }

    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'PROFISSIONAL')")
    public ResponseEntity<List<RespostaAnamnese>> verAnamnese(@PathVariable Long clienteId,
                                                              Authentication auth) {
        Long estabelecimentoId = authUtil.getEstabelecimentoId(auth);
        clienteService.buscarPorId(clienteId, estabelecimentoId);
        return ResponseEntity.ok(anamneseService.buscarRespostasCliente(clienteId));
    }

    @PutMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<String> editarAnamnese(@PathVariable Long clienteId,
                                                 @RequestBody PreencherAnamneseDTO dto,
                                                 Authentication auth) {
        Map<Long, AnamneseService.RespostaDTO> respostas = new HashMap<>();
        dto.getRespostas().forEach((perguntaId, item) -> {
            AnamneseService.RespostaDTO respostaDTO = new AnamneseService.RespostaDTO();
            respostaDTO.setResposta(item.getResposta());
            respostaDTO.setDetalhe(item.getDetalhe());
            respostas.put(perguntaId, respostaDTO);
        });
        anamneseService.editarRespostas(clienteId, respostas, authUtil.getEstabelecimentoId(auth));
        return ResponseEntity.ok("Anamnese atualizada!");
    }

    private void validarPermissaoEdicao(Authentication auth, Long estabelecimentoId) {
        Estabelecimento estab = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
        boolean isPlatinum = estab.getPlano() == Plano.PLATINUM;
        boolean isFuncionario = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GERENTE") ||
                        a.getAuthority().equals("ROLE_PROFISSIONAL"));
        if (isPlatinum && isFuncionario) {
            throw new RuntimeException("No plano Platinum apenas o ADMIN pode editar a anamnese");
        }
    }
}