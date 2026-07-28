package com.hady.saas.application.controller;

import com.hady.saas.application.dto.CriarCupomEstabelecimentoDTO;
import com.hady.saas.application.dto.CupomEstabelecimentoDTO;
import com.hady.saas.business.service.CupomEstabelecimentoService;
import com.hady.saas.business.service.CupomEstabelecimentoService.CupomEstabelecimentoException;
import com.hady.saas.domain.cupom.CupomEstabelecimento;
import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.security.AuthUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Gestão de cupom de desconto do estabelecimento (seção 5-B) — ferramenta do
 * dono do salão pra criar cupons pros PRÓPRIOS clientes. Não confundir com
 * CupomAdminController (cupom de assinatura, admin-master da plataforma).
 *
 * Aplicação do cupom acontece no fluxo público de agendamento (link já vem
 * com o cupom aplicado), ver AgendamentoPublicoController.
 *
 * Gating: mesmo padrão do resto do robô/marketing, `!= Plano.STANDARD`.
 */
@RestController
@RequestMapping("/painel/cupons-estabelecimento")
public class CupomEstabelecimentoController {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final CupomEstabelecimentoService cupomService;
    private final AuthUtil authUtil;

    public CupomEstabelecimentoController(EstabelecimentoRepository estabelecimentoRepository,
                                          CupomEstabelecimentoService cupomService,
                                          AuthUtil authUtil) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.cupomService = cupomService;
        this.authUtil = authUtil;
    }

    private Estabelecimento getEstab(Authentication auth) {
        return estabelecimentoRepository.findById(authUtil.getEstabelecimentoId(auth))
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
    }

    private ResponseEntity<Map<String, Object>> bloqueioDePlanoOuNull(Estabelecimento est) {
        if (est.getPlanoEfetivo() == Plano.STANDARD) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(Map.of("erro", "Cupom de desconto para clientes está disponível a partir do plano Elite."));
        }
        return null;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> criar(Authentication auth, @RequestBody CriarCupomEstabelecimentoDTO dto) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        if (dto == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Corpo da requisição é obrigatório."));
        }

        try {
            CupomEstabelecimento cupom = cupomService.criar(est, dto.getCodigo(), dto.getTipoDesconto(),
                    dto.getValorDesconto(), dto.getValidadeData(), dto.getLimiteUsos());
            return ResponseEntity.status(HttpStatus.CREATED).body(new CupomEstabelecimentoDTO(cupom));
        } catch (CupomEstabelecimentoException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> listar(Authentication auth) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        List<CupomEstabelecimentoDTO> resposta = cupomService.listar(est).stream()
                .map(CupomEstabelecimentoDTO::new)
                .toList();
        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/{id}/desativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> desativar(Authentication auth, @PathVariable Long id) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        try {
            return ResponseEntity.ok(new CupomEstabelecimentoDTO(cupomService.desativar(est, id)));
        } catch (CupomEstabelecimentoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> reativar(Authentication auth, @PathVariable Long id) {
        Estabelecimento est = getEstab(auth);
        var bloqueio = bloqueioDePlanoOuNull(est);
        if (bloqueio != null) return bloqueio;

        try {
            return ResponseEntity.ok(new CupomEstabelecimentoDTO(cupomService.reativar(est, id)));
        } catch (CupomEstabelecimentoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }
}