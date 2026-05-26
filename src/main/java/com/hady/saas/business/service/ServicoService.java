package com.hady.saas.business.service;

import com.hady.saas.domain.servico.Servico;
import com.hady.saas.infrastructure.repository.ServicoRepository;
import org.springframework.stereotype.Service;

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

    public List<Servico> listarAtivos(Long estabelecimentoId) {
        return servicoRepository.findByEstabelecimentoIdAndAtivo(estabelecimentoId, true);
    }

    public Servico buscarPorId(Long id, Long estabelecimentoId) {
        // ✅ busca já filtrando por estabelecimentoId diretamente na query
        // evita LazyInitializationException ao acessar servico.getEstabelecimento().getId()
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
        return servicoRepository.save(servico);
    }

    public void desativar(Long id, Long estabelecimentoId) {
        Servico servico = buscarPorId(id, estabelecimentoId);
        servico.setAtivo(false);
        servicoRepository.save(servico);
    }
}
