package com.hady.saas.business.service;

import com.hady.saas.application.dto.AssinaturaDTO;
import com.hady.saas.application.dto.TrocarPlanoDTO;
import com.hady.saas.domain.cobranca.CobrancaPlano;
import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.config.MercadoPagoConfiguration;
import com.hady.saas.infrastructure.repository.CobrancaPlanoRepository;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssinaturaServiceTest {

    @Mock EstabelecimentoRepository estabelecimentoRepo;
    @Mock CobrancaPlanoRepository   cobrancaRepo;
    @Mock MercadoPagoConfiguration  mpConfig;
    @Mock RestTemplate              restTemplate;

    @InjectMocks AssinaturaService service;

    // ─── Fixtures ─────────────────────────────────────────────────────────────

    private Estabelecimento estabSemAssinatura() {
        Estabelecimento e = new Estabelecimento();
        e.setPlano(null);
        e.setPlanoAtivo(false);
        e.setMpSubscriptionId(null);
        return e;
    }

    private Estabelecimento estabComAssinatura(Plano plano) {
        Estabelecimento e = new Estabelecimento();
        e.setPlano(plano);
        e.setPlanoAtivo(true);
        e.setMpSubscriptionId("sub_existente");
        e.setPlanoValidoAte(LocalDate.now().plusMonths(1));
        return e;
    }

    private Map<String, Object> respostaMpOk(String id) {
        return Map.of("id", id, "status", "authorized");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // assinarRecorrente
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("assinarRecorrente")
    class AssinarRecorrente {

        @BeforeEach
        void setup() {
            when(mpConfig.getAccessToken()).thenReturn("token-teste");
            when(mpConfig.getPlanoIdPorTipo("STANDARD")).thenReturn("plan_standard_id");
        }

        @Test
        @DisplayName("cria assinatura e persiste cobrança quando não há assinatura anterior")
        void deveCriarAssinaturaSemAssinaturaAnterior() {
            Estabelecimento estab = estabSemAssinatura();
            when(estabelecimentoRepo.findById(1L)).thenReturn(Optional.of(estab));
            when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(respostaMpOk("sub_nova")));

            AssinaturaDTO dto = new AssinaturaDTO();
            dto.setPlano("STANDARD");
            dto.setCardToken("card_token_123");
            dto.setEmail("dono@email.com");

            Map<String, Object> result = service.assinarRecorrente(1L, dto);

            assertThat(result.get("plano")).isEqualTo("STANDARD");
            assertThat(result.get("subscriptionId")).isEqualTo("sub_nova");

            assertThat(estab.getPlano()).isEqualTo(Plano.STANDARD);
            assertThat(estab.isPlanoAtivo()).isTrue();
            assertThat(estab.getPlanoValidoAte()).isEqualTo(LocalDate.now().plusMonths(1));

            ArgumentCaptor<CobrancaPlano> captor = ArgumentCaptor.forClass(CobrancaPlano.class);
            verify(cobrancaRepo).save(captor.capture());
            assertThat(captor.getValue().getTipoCobranca()).isEqualTo("RECORRENTE");
            assertThat(captor.getValue().getStatus()).isEqualTo("AUTORIZADO");
        }

        @Test
        @DisplayName("cancela assinatura anterior no MP antes de criar a nova")
        void deveCancelarAssinaturaAnteriorSeExistir() {
            Estabelecimento estab = estabComAssinatura(Plano.STANDARD);
            when(estabelecimentoRepo.findById(1L)).thenReturn(Optional.of(estab));
            when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(respostaMpOk("sub_nova")));
            when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of()));

            AssinaturaDTO dto = new AssinaturaDTO();
            dto.setPlano("STANDARD");
            dto.setCardToken("card_token_abc");
            dto.setEmail("dono@email.com");

            service.assinarRecorrente(1L, dto);

            // Deve ter chamado PUT /preapproval/{id} para cancelar a anterior
            verify(restTemplate).exchange(
                    contains("sub_existente"), eq(HttpMethod.PUT), any(), eq(Map.class));
        }

        @Test
        @DisplayName("lança RuntimeException quando estabelecimento não existe")
        void deveLancarExcecaoEstabelecimentoNaoEncontrado() {
            when(estabelecimentoRepo.findById(99L)).thenReturn(Optional.empty());

            AssinaturaDTO dto = new AssinaturaDTO();
            dto.setPlano("STANDARD");
            dto.setCardToken("token");
            dto.setEmail("x@x.com");

            assertThatThrownBy(() -> service.assinarRecorrente(99L, dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não encontrado");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // trocarPlano
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("trocarPlano")
    class TrocarPlano {

        @BeforeEach
        void setup() {
            when(mpConfig.getAccessToken()).thenReturn("token-teste");
        }

        @Test
        @DisplayName("realiza upgrade de STANDARD para ELITE com sucesso")
        void deveRealizarUpgrade() {
            Estabelecimento estab = estabComAssinatura(Plano.STANDARD);
            when(estabelecimentoRepo.findById(1L)).thenReturn(Optional.of(estab));
            when(mpConfig.getPlanoIdPorTipo("ELITE")).thenReturn("plan_elite_id");
            when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of()));
            when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(respostaMpOk("sub_elite")));

            TrocarPlanoDTO dto = new TrocarPlanoDTO();
            dto.setPlano("ELITE");
            dto.setCardToken("card_token_elite");
            dto.setEmail("dono@email.com");

            Map<String, Object> result = service.trocarPlano(1L, dto);

            assertThat(result.get("planoAnterior")).isEqualTo("STANDARD");
            assertThat(result.get("planoNovo")).isEqualTo("ELITE");
            assertThat(result.get("subscriptionId")).isEqualTo("sub_elite");

            assertThat(estab.getPlano()).isEqualTo(Plano.ELITE);
            assertThat(estab.isPlanoAtivo()).isTrue();
            assertThat(estab.getMpSubscriptionId()).isEqualTo("sub_elite");

            ArgumentCaptor<CobrancaPlano> captor = ArgumentCaptor.forClass(CobrancaPlano.class);
            verify(cobrancaRepo).save(captor.capture());
            assertThat(captor.getValue().getTipoCobranca()).isEqualTo("TROCA");
            assertThat(captor.getValue().getPlano()).isEqualTo("ELITE");
        }

        @Test
        @DisplayName("realiza downgrade de PLATINUM para STANDARD com sucesso")
        void deveRealizarDowngrade() {
            Estabelecimento estab = estabComAssinatura(Plano.PLATINUM);
            when(estabelecimentoRepo.findById(1L)).thenReturn(Optional.of(estab));
            when(mpConfig.getPlanoIdPorTipo("STANDARD")).thenReturn("plan_standard_id");
            when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of()));
            when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(respostaMpOk("sub_std")));

            TrocarPlanoDTO dto = new TrocarPlanoDTO();
            dto.setPlano("STANDARD");
            dto.setCardToken("card_downgrade");
            dto.setEmail("dono@email.com");

            Map<String, Object> result = service.trocarPlano(1L, dto);

            assertThat(result.get("planoAnterior")).isEqualTo("PLATINUM");
            assertThat(result.get("planoNovo")).isEqualTo("STANDARD");
            assertThat(estab.getPlano()).isEqualTo(Plano.STANDARD);
        }

        @Test
        @DisplayName("lança exceção ao tentar trocar para o mesmo plano")
        void deveLancarExcecaoMesmoPlano() {
            Estabelecimento estab = estabComAssinatura(Plano.ELITE);
            when(estabelecimentoRepo.findById(1L)).thenReturn(Optional.of(estab));

            TrocarPlanoDTO dto = new TrocarPlanoDTO();
            dto.setPlano("ELITE");
            dto.setCardToken("token");
            dto.setEmail("dono@email.com");

            assertThatThrownBy(() -> service.trocarPlano(1L, dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("já está no plano ELITE");

            // Não deve ter chamado o MP nem salvo nada
            verifyNoInteractions(restTemplate);
            verifyNoInteractions(cobrancaRepo);
        }

        @Test
        @DisplayName("lança exceção quando não há assinatura ativa")
        void deveLancarExcecaoSemAssinaturaAtiva() {
            Estabelecimento estab = estabSemAssinatura();
            when(estabelecimentoRepo.findById(1L)).thenReturn(Optional.of(estab));

            TrocarPlanoDTO dto = new TrocarPlanoDTO();
            dto.setPlano("ELITE");
            dto.setCardToken("token");
            dto.setEmail("dono@email.com");

            assertThatThrownBy(() -> service.trocarPlano(1L, dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Nenhuma assinatura recorrente ativa");

            verifyNoInteractions(restTemplate);
        }

        @Test
        @DisplayName("lança exceção quando estabelecimento não existe")
        void deveLancarExcecaoEstabelecimentoNaoEncontrado() {
            when(estabelecimentoRepo.findById(99L)).thenReturn(Optional.empty());

            TrocarPlanoDTO dto = new TrocarPlanoDTO();
            dto.setPlano("ELITE");
            dto.setCardToken("token");
            dto.setEmail("x@x.com");

            assertThatThrownBy(() -> service.trocarPlano(99L, dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não encontrado");
        }

        @Test
        @DisplayName("cancela assinatura anterior no MP antes de criar a nova")
        void deveCancelarSubscriptionAnteriorNoMp() {
            Estabelecimento estab = estabComAssinatura(Plano.STANDARD);
            when(estabelecimentoRepo.findById(1L)).thenReturn(Optional.of(estab));
            when(mpConfig.getPlanoIdPorTipo("PLATINUM")).thenReturn("plan_plat_id");
            when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of()));
            when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(respostaMpOk("sub_plat")));

            TrocarPlanoDTO dto = new TrocarPlanoDTO();
            dto.setPlano("PLATINUM");
            dto.setCardToken("card_plat");
            dto.setEmail("dono@email.com");

            service.trocarPlano(1L, dto);

            verify(restTemplate).exchange(
                    contains("sub_existente"), eq(HttpMethod.PUT), any(), eq(Map.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // cancelarAssinatura
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("cancelarAssinatura")
    class CancelarAssinatura {

        @Test
        @DisplayName("cancela no MP e limpa o subscriptionId e planoAtivo")
        void deveCancelarAssinatura() {
            Estabelecimento estab = estabComAssinatura(Plano.ELITE);
            when(estabelecimentoRepo.findById(1L)).thenReturn(Optional.of(estab));
            when(mpConfig.getAccessToken()).thenReturn("token-teste");
            when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of()));

            service.cancelarAssinatura(1L);

            assertThat(estab.isPlanoAtivo()).isFalse();
            assertThat(estab.getMpSubscriptionId()).isNull();
            verify(estabelecimentoRepo).save(estab);
        }

        @Test
        @DisplayName("não faz nada quando não há subscriptionId")
        void deveIgnorarSeSemSubscription() {
            Estabelecimento estab = estabSemAssinatura();
            when(estabelecimentoRepo.findById(1L)).thenReturn(Optional.of(estab));

            service.cancelarAssinatura(1L);

            verifyNoInteractions(restTemplate);
            verify(estabelecimentoRepo, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // statusPlano
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("statusPlano")
    class StatusPlano {

        @Test
        @DisplayName("retorna campos corretos para assinatura ativa")
        void deveRetornarStatusAtivo() {
            Estabelecimento estab = estabComAssinatura(Plano.PLATINUM);
            when(estabelecimentoRepo.findById(1L)).thenReturn(Optional.of(estab));

            Map<String, Object> status = service.statusPlano(1L);

            assertThat(status.get("plano")).isEqualTo("PLATINUM");
            assertThat(status.get("ativo")).isEqualTo(true);
            assertThat(status.get("subscriptionId")).isEqualTo("sub_existente");
        }

        @Test
        @DisplayName("retorna plano null quando estabelecimento não tem plano")
        void deveRetornarPlanoNullSemAssinatura() {
            Estabelecimento estab = estabSemAssinatura();
            when(estabelecimentoRepo.findById(1L)).thenReturn(Optional.of(estab));

            Map<String, Object> status = service.statusPlano(1L);

            assertThat(status.get("plano")).isNull();
            assertThat(status.get("ativo")).isEqualTo(false);
        }
    }
}
