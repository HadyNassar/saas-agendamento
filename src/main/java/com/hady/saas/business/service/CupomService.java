package com.hady.saas.business.service;

import com.hady.saas.application.dto.CriarCupomDTO;
import com.hady.saas.application.dto.CupomDTO;
import com.hady.saas.application.dto.ValidarCupomResponseDTO;
import com.hady.saas.domain.cupom.Cupom;
import com.hady.saas.infrastructure.repository.CupomRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CupomService {

    private static final String CARACTERES_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sem O/0/I/1 (confusão visual)
    private final CupomRepository cupomRepository;
    private final JdbcTemplate jdbcTemplate;
    private final SecureRandom random = new SecureRandom();

    public CupomService(CupomRepository cupomRepository, JdbcTemplate jdbcTemplate) {
        this.cupomRepository = cupomRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── Criação ───────────────────────────────────────────────────────
    @Transactional
    public CupomDTO criar(CriarCupomDTO dto) {
        if (!"PERCENTUAL".equals(dto.getTipoDesconto()) && !"VALOR_FIXO".equals(dto.getTipoDesconto())) {
            throw new RuntimeException("Tipo de desconto deve ser PERCENTUAL ou VALOR_FIXO");
        }
        if ("PERCENTUAL".equals(dto.getTipoDesconto()) && dto.getValorDesconto().doubleValue() > 100) {
            throw new RuntimeException("Desconto percentual não pode ser maior que 100%");
        }

        String codigo = (dto.getCodigo() != null && !dto.getCodigo().isBlank())
                ? dto.getCodigo().trim().toUpperCase()
                : gerarCodigoUnico();

        if (cupomRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new RuntimeException("Já existe um cupom com o código: " + codigo);
        }

        Cupom cupom = new Cupom();
        cupom.setCodigo(codigo);
        cupom.setTipoDesconto(dto.getTipoDesconto());
        cupom.setValorDesconto(dto.getValorDesconto());
        cupom.setPlanoRestrito(dto.getPlanoRestrito());
        cupom.setValidadeData(dto.getValidadeData());
        cupom.setLimiteUsos(dto.getLimiteUsos());
        cupom.setAtivo(true);
        cupom.setCriadoEm(LocalDateTime.now());

        Cupom salvo = cupomRepository.save(cupom);
        return paraDTO(salvo);
    }

    // ── Listagem ──────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<CupomDTO> listar() {
        return cupomRepository.findAll().stream()
                .sorted((a, b) -> b.getCriadoEm().compareTo(a.getCriadoEm()))
                .map(this::paraDTO)
                .toList();
    }

    // ── Ativar/desativar ──────────────────────────────────────────────
    @Transactional
    public void alterarStatus(Long id, boolean ativo) {
        Cupom cupom = cupomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cupom não encontrado"));
        cupom.setAtivo(ativo);
        cupomRepository.save(cupom);
    }

    // ── Validação pública — usada no fluxo de assinatura antes de pagar ──
    @Transactional(readOnly = true)
    public ValidarCupomResponseDTO validar(String codigo, String plano) {
        Cupom cupom = cupomRepository.findByCodigoIgnoreCase(codigo).orElse(null);

        if (cupom == null) {
            return ValidarCupomResponseDTO.invalido("Cupom não encontrado.");
        }
        if (!cupom.estaValido()) {
            return ValidarCupomResponseDTO.invalido("Este cupom expirou ou atingiu o limite de usos.");
        }
        if (!cupom.valeParaPlano(plano)) {
            return ValidarCupomResponseDTO.invalido("Este cupom não é válido para o plano selecionado.");
        }

        return ValidarCupomResponseDTO.valido(cupom.getTipoDesconto(), cupom.getValorDesconto());
    }

    // ── Aplicar cupom — registra o uso e incrementa o contador ───────────
    // Chamado no momento da confirmação do pagamento (não na validação prévia,
    // que só checa se pode ser usado sem "gastar" o cupom ainda).
    @Transactional
    public void aplicar(String codigo, Long estabelecimentoId) {
        Cupom cupom = cupomRepository.findByCodigoIgnoreCase(codigo)
                .orElseThrow(() -> new RuntimeException("Cupom não encontrado"));

        if (!cupom.estaValido()) {
            throw new RuntimeException("Este cupom não é mais válido.");
        }

        boolean jaUsado = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cupom_usos WHERE cupom_id = ? AND estabelecimento_id = ?",
                Integer.class, cupom.getId(), estabelecimentoId
        ) > 0;

        if (jaUsado) {
            throw new RuntimeException("Este estabelecimento já utilizou este cupom.");
        }

        jdbcTemplate.update(
                "INSERT INTO cupom_usos (cupom_id, estabelecimento_id) VALUES (?, ?)",
                cupom.getId(), estabelecimentoId
        );

        cupom.setUsosAtuais(cupom.getUsosAtuais() + 1);
        cupomRepository.save(cupom);
    }

    // ── Privados ──────────────────────────────────────────────────────

    private String gerarCodigoUnico() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(CARACTERES_CODIGO.charAt(random.nextInt(CARACTERES_CODIGO.length())));
            }
            codigo = sb.toString();
        } while (cupomRepository.existsByCodigoIgnoreCase(codigo));
        return codigo;
    }

    private CupomDTO paraDTO(Cupom c) {
        return new CupomDTO(
                c.getId(), c.getCodigo(), c.getTipoDesconto(), c.getValorDesconto(),
                c.getPlanoRestrito(), c.getValidadeData(), c.getLimiteUsos(),
                c.getUsosAtuais(), c.isAtivo(), c.estaValido(), c.getCriadoEm()
        );
    }
}