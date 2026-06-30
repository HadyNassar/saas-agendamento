package com.hady.saas.application.controller;

import com.hady.saas.application.dto.AlterarEmailDTO;
import com.hady.saas.application.dto.AlterarSenhaDTO;
import com.hady.saas.application.dto.EditarEstabelecimentoDTO;
import com.hady.saas.application.dto.EstabelecimentoResponseDTO;
import com.hady.saas.business.service.ConfiguracaoService;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.security.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/painel/configuracoes")
public class ConfiguracaoController {

    private final ConfiguracaoService configuracaoService;
    private final AuthUtil authUtil;

    public ConfiguracaoController(ConfiguracaoService configuracaoService, AuthUtil authUtil) {
        this.configuracaoService = configuracaoService;
        this.authUtil = authUtil;
    }

    @GetMapping("/estabelecimento")
    public ResponseEntity<EstabelecimentoResponseDTO> buscarEstabelecimento(Authentication auth) {
        Estabelecimento est = configuracaoService.buscarEstabelecimento(authUtil.getEstabelecimentoId(auth));
        EstabelecimentoResponseDTO dto = new EstabelecimentoResponseDTO(
                est.getId(), est.getNome(), est.getSlug(),
                est.getTelefone(), est.getEndereco(),
                est.getMesesAntecipacao(), est.isWhatsappConectado(),
                est.getPlano() != null ? est.getPlanoEfetivo().name() : null,
                est.isMpConectado(),
                est.isAceitaPagamento(),
                est.isExigirSinal(),
                est.getTipoSinal(),
                est.getValorSinal()
        );
        return ResponseEntity.ok(dto);
    }

    // Aba "Estabelecimento" — atualiza nome, telefone, endereço e logo.
    // Mantém @Valid pois nome é obrigatório nesta aba.
    @PutMapping("/estabelecimento")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> editarEstabelecimento(@Valid @RequestBody EditarEstabelecimentoDTO dto,
                                                        Authentication auth) {
        configuracaoService.editarEstabelecimento(
                authUtil.getEstabelecimentoId(auth),
                dto.getNome(),
                dto.getTelefone(),
                dto.getEndereco(),
                dto.getMesesAntecipacao(),
                dto.getFotoPerfil(),
                dto.getInstagram(),
                dto.getWhatsappContato(),
                dto.getFacebook(),
                dto.getComodidades(),
                dto.getFormasPagamento()
        );
        return ResponseEntity.ok("Dados do estabelecimento atualizados!");
    }

    // NOVO — Aba "Detalhes & Redes": atualiza apenas foto, redes sociais,
    // comodidades e formas de pagamento. Nome não é exigido nem alterado.
    @PatchMapping("/detalhes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> editarDetalhes(@RequestBody EditarEstabelecimentoDTO dto,
                                                 Authentication auth) {
        configuracaoService.editarEstabelecimento(
                authUtil.getEstabelecimentoId(auth),
                null,                       // nome — preserva o valor existente no banco
                null,                       // telefone — preserva
                null,                       // endereco — preserva
                null,                       // mesesAntecipacao — preserva
                dto.getFotoPerfil(),
                dto.getInstagram(),
                dto.getWhatsappContato(),
                dto.getFacebook(),
                dto.getComodidades(),
                dto.getFormasPagamento()
        );
        return ResponseEntity.ok("Detalhes atualizados!");
    }

    @PatchMapping("/sinal")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> atualizarSinal(@RequestBody Map<String, Object> body,
                                                 Authentication auth) {
        Boolean exigirSinal = body.get("exigirSinal") != null ? (Boolean) body.get("exigirSinal") : false;
        String tipoSinal    = body.get("tipoSinal") != null ? (String) body.get("tipoSinal") : null;
        BigDecimal valorSinal = body.get("valorSinal") != null
                ? new BigDecimal(body.get("valorSinal").toString())
                : null;

        configuracaoService.atualizarSinal(authUtil.getEstabelecimentoId(auth), exigirSinal, tipoSinal, valorSinal);
        return ResponseEntity.ok("Configuração de sinal salva com sucesso!");
    }

    @PutMapping("/email")
    public ResponseEntity<String> alterarEmail(@Valid @RequestBody AlterarEmailDTO dto, Authentication auth) {
        configuracaoService.alterarEmail(authUtil.getUsuarioId(auth), dto.getNovoEmail(), dto.getSenhaAtual());
        return ResponseEntity.ok("Email alterado com sucesso! Faça login novamente com o novo email.");
    }

    @PutMapping("/senha")
    public ResponseEntity<String> alterarSenha(@Valid @RequestBody AlterarSenhaDTO dto, Authentication auth) {
        configuracaoService.alterarSenha(
                authUtil.getUsuarioId(auth),
                dto.getSenhaAtual(), dto.getNovaSenha(), dto.getConfirmarNovaSenha()
        );
        return ResponseEntity.ok("Senha alterada com sucesso! Faça login novamente.");
    }
}