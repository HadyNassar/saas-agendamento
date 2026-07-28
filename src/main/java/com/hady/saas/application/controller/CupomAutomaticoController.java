package com.hady.saas.application.controller;

import com.hady.saas.application.dto.ConfigurarCupomAutomaticoDTO;
import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Configuração do cupom automático de reativação (item 8 da camada
 * PLATINUM). Desligado por padrão (migration V26) — o dono do salão precisa
 * ativar explicitamente e escolher o desconto antes de qualquer cupom sair
 * sozinho pra um cliente.
 *
 * Regra confirmada com o dono do produto:
 *   - Só dispara SE o dono ligar (não é automático por padrão pra ninguém).
 *   - Desconto é configurável pelo dono (percentual ou valor fixo).
 *   - Cupom vale 7 dias a partir da geração.
 *   - Pode gerar de novo pro mesmo cliente se ele ficar INATIVO outra vez
 *     no futuro (não há trava de "um cupom por cliente pra sempre").
 *   - O cliente recebe o link já com o cupom aplicado (não digita código).
 *
 * A geração em si acontece em CampanhaAutomaticaService, no momento em que
 * um cliente vira INATIVO — este controller só liga/desliga e guarda a
 * configuração do desconto.
 */
@RestController
@RequestMapping("/painel/whatsapp/cupom-automatico")
public class CupomAutomaticoController {

    private static final List<String> TIPOS_VALIDOS = List.of("PERCENTUAL", "VALOR_FIXO");

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final AuthUtil authUtil;

    public CupomAutomaticoController(EstabelecimentoRepository estabelecimentoRepository, AuthUtil authUtil) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.authUtil = authUtil;
    }

    private Estabelecimento getEstab(Authentication auth) {
        return estabelecimentoRepository.findById(authUtil.getEstabelecimentoId(auth))
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
    }

    private ResponseEntity<Map<String, Object>> bloqueioDePlanoOuNull(Estabelecimento est) {
        if (est.getPlanoEfetivo() == Plano.STANDARD) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(Map.of("erro", "Cupom automático de reativação está disponível a partir do plano Elite."));
        }
        return null;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> buscarConfiguracao(Authentication auth) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        return ResponseEntity.ok(Map.of(
                "ativo", est.isCupomReativacaoAutomaticoAtivo(),
                "tipoDesconto", est.getCupomReativacaoTipoDesconto() == null ? "" : est.getCupomReativacaoTipoDesconto(),
                "valorDesconto", est.getCupomReativacaoValorDesconto() == null ? BigDecimal.ZERO : est.getCupomReativacaoValorDesconto(),
                "validadeDias", 7 // fixo por regra confirmada — não configurável nesta etapa
        ));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> configurar(Authentication auth, @RequestBody ConfigurarCupomAutomaticoDTO dto) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        if (dto == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Corpo da requisição é obrigatório."));
        }

        if (dto.isAtivo()) {
            if (dto.getTipoDesconto() == null || !TIPOS_VALIDOS.contains(dto.getTipoDesconto())) {
                return ResponseEntity.unprocessableEntity()
                        .body(Map.of("erro", "Campo 'tipoDesconto' deve ser PERCENTUAL ou VALOR_FIXO."));
            }
            if (dto.getValorDesconto() == null || dto.getValorDesconto().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.unprocessableEntity()
                        .body(Map.of("erro", "Campo 'valorDesconto' deve ser maior que zero."));
            }
            if ("PERCENTUAL".equals(dto.getTipoDesconto()) && dto.getValorDesconto().compareTo(BigDecimal.valueOf(100)) > 0) {
                return ResponseEntity.unprocessableEntity()
                        .body(Map.of("erro", "Desconto percentual não pode ser maior que 100."));
            }
            est.setCupomReativacaoAutomaticoAtivo(true);
            est.setCupomReativacaoTipoDesconto(dto.getTipoDesconto());
            est.setCupomReativacaoValorDesconto(dto.getValorDesconto());
        } else {
            // Desliga, mas preserva o último desconto configurado — se o
            // dono reativar depois, não precisa reconfigurar do zero.
            est.setCupomReativacaoAutomaticoAtivo(false);
        }

        estabelecimentoRepository.save(est);
        return ResponseEntity.ok(Map.of(
                "ativo", est.isCupomReativacaoAutomaticoAtivo(),
                "tipoDesconto", est.getCupomReativacaoTipoDesconto() == null ? "" : est.getCupomReativacaoTipoDesconto(),
                "valorDesconto", est.getCupomReativacaoValorDesconto() == null ? BigDecimal.ZERO : est.getCupomReativacaoValorDesconto()
        ));
    }
}