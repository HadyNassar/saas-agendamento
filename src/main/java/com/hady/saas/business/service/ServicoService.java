package com.hady.saas.business.service;

import com.hady.saas.domain.servico.Servico;
import com.hady.saas.infrastructure.repository.ServicoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;

    public ServicoService(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }

    public Servico criar(Servico servico) {
        return servicoRepository.save(servico);
    }

    // Usado pelo controller quando o estabelecimento é resolvido via AuthUtil (sem carregar a entidade)
    public Servico criarComEstabelecimentoId(Servico servico, com.hady.saas.application.dto.ServicoDTO dto, Long estabelecimentoId) {
        com.hady.saas.domain.estabelecimento.Estabelecimento estabRef = new com.hady.saas.domain.estabelecimento.Estabelecimento();
        estabRef.setId(estabelecimentoId);
        servico.setEstabelecimento(estabRef);
        return servicoRepository.save(servico);
    }

    public List<Servico> listarAtivos(Long estabelecimentoId) {
        return servicoRepository.findByEstabelecimentoIdAndAtivo(estabelecimentoId, true);
    }

    public Servico buscarPorId(Long id, Long estabelecimentoId) {
        return servicoRepository.findByIdAndEstabelecimentoId(id, estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado"));
    }

    public Servico atualizar(Long id, Servico dados, Long estabelecimentoId) {
        Servico servico = buscarPorId(id, estabelecimentoId);
        servico.setNome(dados.getNome());
        servico.setDescricao(dados.getDescricao());
        servico.setDuracaoMinutos(dados.getDuracaoMinutos());
        servico.setPreco(dados.getPreco());
        servico.setCategoria(dados.getCategoria());
        servico.setCustoMaterialFixo(
            dados.getCustoMaterialFixo() != null ? dados.getCustoMaterialFixo() : BigDecimal.ZERO
        );
        servico.setCustoMaterialPercent(
            dados.getCustoMaterialPercent() != null ? dados.getCustoMaterialPercent() : BigDecimal.ZERO
        );
        return servicoRepository.save(servico);
    }

    public void desativar(Long id, Long estabelecimentoId) {
        Servico servico = buscarPorId(id, estabelecimentoId);
        servico.setAtivo(false);
        servicoRepository.save(servico);
    }
}
