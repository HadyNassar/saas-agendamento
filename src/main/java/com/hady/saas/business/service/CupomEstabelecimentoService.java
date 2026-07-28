package com.hady.saas.business.service;

import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.cupom.CupomEstabelecimento;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.AgendamentoRepository;
import com.hady.saas.infrastructure.repository.CupomEstabelecimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cupom de desconto do dono do estabelecimento para os próprios clientes
 * (seção 5-B). CRUD de gestão pelo painel + validação/aplicação no fluxo de
 * agendamento público (o link já vem com o cupom aplicado — decisão
 * confirmada com o dono do produto).
 */
@Service
public class CupomEstabelecimentoService {

    private static final List<String> TIPOS_VALIDOS = List.of("PERCENTUAL", "VALOR_FIXO");

    private final CupomEstabelecimentoRepository cupomRepository;
    private final AgendamentoRepository agendamentoRepository;

    public CupomEstabelecimentoService(CupomEstabelecimentoRepository cupomRepository,
                                       AgendamentoRepository agendamentoRepository) {
        this.cupomRepository = cupomRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    public static class CupomEstabelecimentoException extends RuntimeException {
        public CupomEstabelecimentoException(String mensagem) { super(mensagem); }
    }

    // ── Gestão (painel do dono) ─────────────────────────────────────────────

    @Transactional
    public CupomEstabelecimento criar(Estabelecimento estabelecimento, String codigo, String tipoDesconto,
                                      BigDecimal valorDesconto, LocalDate validadeData, Integer limiteUsos) {
        if (codigo == null || codigo.isBlank()) {
            throw new CupomEstabelecimentoException("Campo 'codigo' é obrigatório.");
        }
        if (tipoDesconto == null || !TIPOS_VALIDOS.contains(tipoDesconto)) {
            throw new CupomEstabelecimentoException("Campo 'tipoDesconto' deve ser PERCENTUAL ou VALOR_FIXO.");
        }
        if (valorDesconto == null || valorDesconto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CupomEstabelecimentoException("Campo 'valorDesconto' deve ser maior que zero.");
        }
        if ("PERCENTUAL".equals(tipoDesconto) && valorDesconto.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new CupomEstabelecimentoException("Desconto percentual não pode ser maior que 100.");
        }
        if (limiteUsos != null && limiteUsos <= 0) {
            throw new CupomEstabelecimentoException("Campo 'limiteUsos', quando informado, deve ser maior que zero.");
        }

        String codigoNormalizado = codigo.trim().toUpperCase();
        cupomRepository.findByEstabelecimentoIdAndCodigoIgnoreCase(estabelecimento.getId(), codigoNormalizado)
                .ifPresent(c -> {
                    throw new CupomEstabelecimentoException("Já existe um cupom com o código '" + codigoNormalizado + "'.");
                });

        CupomEstabelecimento cupom = new CupomEstabelecimento();
        cupom.setEstabelecimento(estabelecimento);
        cupom.setCodigo(codigoNormalizado);
        cupom.setTipoDesconto(tipoDesconto);
        cupom.setValorDesconto(valorDesconto);
        cupom.setValidadeData(validadeData);
        cupom.setLimiteUsos(limiteUsos);
        cupom.setAtivo(true);

        return cupomRepository.save(cupom);
    }

    public List<CupomEstabelecimento> listar(Estabelecimento estabelecimento) {
        return cupomRepository.findByEstabelecimentoIdOrderByCriadoEmDesc(estabelecimento.getId());
    }

    @Transactional
    public CupomEstabelecimento desativar(Estabelecimento estabelecimento, Long cupomId) {
        CupomEstabelecimento cupom = buscarDoEstabelecimento(estabelecimento, cupomId);
        cupom.setAtivo(false);
        return cupomRepository.save(cupom);
    }

    @Transactional
    public CupomEstabelecimento reativar(Estabelecimento estabelecimento, Long cupomId) {
        CupomEstabelecimento cupom = buscarDoEstabelecimento(estabelecimento, cupomId);
        cupom.setAtivo(true);
        return cupomRepository.save(cupom);
    }

    public CupomEstabelecimento buscarDoEstabelecimento(Estabelecimento estabelecimento, Long cupomId) {
        return cupomRepository.findByIdAndEstabelecimentoId(cupomId, estabelecimento.getId())
                .orElseThrow(() -> new CupomEstabelecimentoException("Cupom não encontrado."));
    }

    // ── Validação (link público, antes de confirmar o agendamento) ─────────
    //
    // Não consome uso ainda — só diz se o código é válido e qual seria o
    // valor com desconto, pra exibir na tela antes do cliente confirmar.
    public CupomEstabelecimento validar(Estabelecimento estabelecimento, String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new CupomEstabelecimentoException("Código do cupom não informado.");
        }
        CupomEstabelecimento cupom = cupomRepository
                .findByEstabelecimentoIdAndCodigoIgnoreCase(estabelecimento.getId(), codigo.trim())
                .orElseThrow(() -> new CupomEstabelecimentoException("Cupom inválido."));

        if (!cupom.estaValido()) {
            throw new CupomEstabelecimentoException("Este cupom não está mais válido (expirado, esgotado ou desativado).");
        }
        return cupom;
    }

    // ── Aplicação (no momento em que o agendamento é criado) ───────────────
    //
    // Chamado só DEPOIS que o Agendamento já foi criado com sucesso pelo
    // fluxo normal (AgendamentoService.agendarPeloCliente) — decisão de
    // design: não alterei a assinatura de agendarPeloCliente (usada também
    // pelo bot conversacional) só pra encaixar cupom; a aplicação acontece
    // como um passo separado, no mesmo controller público, sobre o
    // agendamento já persistido. Revalida o cupom aqui (não confia só na
    // validação prévia) porque pode ter passado tempo/uso entre as duas
    // chamadas — evita corrida em que dois clientes usam o último uso do
    // mesmo cupom quase ao mesmo tempo.
    @Transactional
    public void aplicarNoAgendamento(Estabelecimento estabelecimento, Agendamento agendamento, String codigo) {
        CupomEstabelecimento cupom = validar(estabelecimento, codigo);

        BigDecimal precoServico = agendamento.getServico().getPreco();
        BigDecimal valorDesconto = cupom.calcularValorDesconto(precoServico);

        cupom.setUsosAtuais(cupom.getUsosAtuais() + 1);
        cupomRepository.save(cupom);

        agendamento.setCupomEstabelecimento(cupom);
        agendamento.setValorDescontoAplicado(valorDesconto);
        agendamentoRepository.save(agendamento);
    }
}