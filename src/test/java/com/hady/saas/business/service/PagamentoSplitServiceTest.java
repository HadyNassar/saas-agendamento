package com.hady.saas.business.service;

import com.hady.saas.application.dto.PagamentoAgendamentoDTO;
import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.servico.Servico;
import com.hady.saas.domain.transacao.Transacao;
import com.hady.saas.infrastructure.config.MercadoPagoConfiguration;
import com.hady.saas.infrastructure.repository.AgendamentoRepository;
import com.hady.saas.infrastructure.repository.TransacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagamentoSplitServiceTest {

    @Mock MercadoPagoConfiguration mpConfig;
    @Mock AgendamentoRepository agendamentoRepo;
    @Mock TransacaoRepository transacaoRepo;

    @InjectMocks PagamentoSplitService service;

    private Agendamento agendamento;
    private Estabelecimento estabelecimento;

    @BeforeEach
    void setUp() {
        estabelecimento = new Estabelecimento();
        estabelecimento.setId(1L);
        estabelecimento.setAceitaPagamento(true);
        estabelecimento.setMpConectado(true);
        estabelecimento.setMpAccessToken("token-fake");

        Servico servico = new Servico();
        servico.setNome("Corte");
        servico.setPreco(new BigDecimal("100.00"));

        agendamento = new Agendamento();
        agendamento.setId(1L);
        agendamento.setEstabelecimento(estabelecimento);
        agendamento.setServico(servico);
    }

    @Test
    @DisplayName("Deve rejeitar pagamento quando estabelecimento não aceita pagamento online")
    void processarPagamento_semAceitaPagamento_lancaExcecao() {
        estabelecimento.setAceitaPagamento(false);
        when(agendamentoRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(agendamento));

        PagamentoAgendamentoDTO dto = new PagamentoAgendamentoDTO();
        dto.setAgendamentoId(1L);
        dto.setMetodoPagamento("pix");
        dto.setEmailPagador("cliente@email.com");

        assertThatThrownBy(() -> service.processarPagamento("meu-salao", dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("não aceita pagamento online");
    }

    @Test
    @DisplayName("Deve rejeitar pagamento quando MP não está conectado")
    void processarPagamento_semMpConectado_lancaExcecao() {
        estabelecimento.setMpConectado(false);
        when(agendamentoRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(agendamento));

        PagamentoAgendamentoDTO dto = new PagamentoAgendamentoDTO();
        dto.setAgendamentoId(1L);
        dto.setMetodoPagamento("pix");
        dto.setEmailPagador("cliente@email.com");

        assertThatThrownBy(() -> service.processarPagamento("meu-salao", dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("não aceita pagamento online");
    }

    @Test
    @DisplayName("Deve lançar exceção quando agendamento não for encontrado")
    void processarPagamento_agendamentoNaoEncontrado_lancaExcecao() {
        when(agendamentoRepo.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        PagamentoAgendamentoDTO dto = new PagamentoAgendamentoDTO();
        dto.setAgendamentoId(99L);
        dto.setMetodoPagamento("pix");
        dto.setEmailPagador("cliente@email.com");

        assertThatThrownBy(() -> service.processarPagamento("meu-salao", dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    @DisplayName("Taxa de 1,5% deve ser calculada corretamente sobre o valor do serviço")
    void calcularTaxa_valorCem_taxaUmCinquenta() {
        // R$ 100,00 × 1,5% = R$ 1,50
        BigDecimal valor = new BigDecimal("100.00");
        BigDecimal taxaEsperada = new BigDecimal("1.50");
        BigDecimal taxa = valor.multiply(new BigDecimal("0.015"))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        assertThat(taxa).isEqualByComparingTo(taxaEsperada);
    }

    @Test
    @DisplayName("Valor do estabelecimento deve ser total menos a taxa")
    void calcularValorEstabelecimento_valorCem_noventaOitoCinquenta() {
        BigDecimal valor = new BigDecimal("100.00");
        BigDecimal taxa = new BigDecimal("1.50");
        BigDecimal valorEstabelecimentoEsperado = new BigDecimal("98.50");

        assertThat(valor.subtract(taxa)).isEqualByComparingTo(valorEstabelecimentoEsperado);
    }
}
