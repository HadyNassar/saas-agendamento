package com.hady.saas.application.controller;

import com.hady.saas.application.dto.PerguntaAnamneseDTO;
import com.hady.saas.application.dto.PreencherAnamneseDTO;
import com.hady.saas.business.service.AnamneseService;
import com.hady.saas.business.service.UsuarioService;
import com.hady.saas.domain.anamnese.PerguntaAnamnese;
import com.hady.saas.domain.anamnese.RespostaAnamnese;
import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.usuario.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/painel/anamnese")
public class AnamneseController {

    private final AnamneseService anamneseService;
    private final UsuarioService usuarioService;

    public AnamneseController(AnamneseService anamneseService, UsuarioService usuarioService) {
        this.anamneseService = anamneseService;
        this.usuarioService = usuarioService;
    }

    // GET /painel/anamnese/perguntas — lista todas as perguntas
    @GetMapping("/perguntas")
    public ResponseEntity<List<PerguntaAnamnese>> listarPerguntas(
            @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        return ResponseEntity.ok(anamneseService.listarTodasPerguntas(
                usuario.getEstabelecimento().getId()));
    }

    // POST /painel/anamnese/perguntas — cria pergunta
    // Standard/Elite: ADMIN e FUNCIONARIO | Platinum: só ADMIN
    @PostMapping("/perguntas")
    public ResponseEntity<String> criarPergunta(@Valid @RequestBody PerguntaAnamneseDTO dto,
                                                 @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        validarPermissaoEdicao(usuario);

        anamneseService.criarPergunta(
                dto.getTexto(),
                dto.getTipo(),
                dto.getOrdem(),
                usuario.getEstabelecimento()
        );

        return ResponseEntity.ok("Pergunta criada!");
    }

    // PUT /painel/anamnese/perguntas/{id} — edita pergunta
    @PutMapping("/perguntas/{id}")
    public ResponseEntity<String> editarPergunta(@PathVariable Long id,
                                                  @Valid @RequestBody PerguntaAnamneseDTO dto,
                                                  @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        validarPermissaoEdicao(usuario);

        anamneseService.editarPergunta(
                id,
                dto.getTexto(),
                dto.getTipo(),
                dto.getOrdem(),
                usuario.getEstabelecimento().getId()
        );

        return ResponseEntity.ok("Pergunta atualizada!");
    }

    // DELETE /painel/anamnese/perguntas/{id} — desativa pergunta
    @DeleteMapping("/perguntas/{id}")
    public ResponseEntity<String> desativarPergunta(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        validarPermissaoEdicao(usuario);
        anamneseService.desativarPergunta(id, usuario.getEstabelecimento().getId());
        return ResponseEntity.ok("Pergunta desativada!");
    }

    // GET /painel/anamnese/cliente/{clienteId} — vê anamnese do cliente
    // Disponível para todos os usuários logados
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<RespostaAnamnese>> verAnamnese(@PathVariable Long clienteId,
                                                               @AuthenticationPrincipal UserDetails userDetails) {
        usuarioService.buscarPorEmail(userDetails.getUsername()); // valida autenticação
        return ResponseEntity.ok(anamneseService.buscarRespostasCliente(clienteId));
    }

    // PUT /painel/anamnese/cliente/{clienteId} — usuário edita respostas do cliente
    @PutMapping("/cliente/{clienteId}")
    public ResponseEntity<String> editarAnamnese(@PathVariable Long clienteId,
                                                  @RequestBody PreencherAnamneseDTO dto,
                                                  @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());

        // Converte DTO para Map que o service espera
        Map<Long, AnamneseService.RespostaDTO> respostas = new HashMap<>();
        dto.getRespostas().forEach((perguntaId, item) -> {
            AnamneseService.RespostaDTO respostaDTO = new AnamneseService.RespostaDTO();
            respostaDTO.setResposta(item.getResposta());
            respostaDTO.setDetalhe(item.getDetalhe());
            respostas.put(perguntaId, respostaDTO);
        });

        anamneseService.editarRespostas(clienteId, respostas, usuario.getEstabelecimento().getId());
        return ResponseEntity.ok("Anamnese atualizada!");
    }

    // Valida permissão de edição de perguntas baseado no plano
    // Platinum: só ADMIN | Standard/Elite: ADMIN e FUNCIONARIO
    private void validarPermissaoEdicao(Usuario usuario) {
        boolean isPlatinum = usuario.getEstabelecimento().getPlano() == Plano.PLATINUM;
        boolean isFuncionario = usuario.getRole().name().equals("FUNCIONARIO") ||
                                usuario.getRole().name().equals("FUNCIONARIO_TOP");

        if (isPlatinum && isFuncionario) {
            throw new RuntimeException("No plano Platinum apenas o ADMIN pode editar a anamnese");
        }
    }
}
