package com.hady.saas.business.service;

import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.ClienteRepository;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;

    public ClienteService(ClienteRepository clienteRepository,
                          EstabelecimentoRepository estabelecimentoRepository) {
        this.clienteRepository = clienteRepository;
        this.estabelecimentoRepository = estabelecimentoRepository;
    }

    // Busca cliente pelo telefone — se não existir cria automaticamente
    public Cliente buscarOuCriar(String telefone, String nome, String email, Estabelecimento estabelecimento) {
        return clienteRepository
                .findByTelefoneAndEstabelecimentoId(telefone, estabelecimento.getId())
                .orElseGet(() -> {
                    Cliente novo = new Cliente();
                    novo.setNome(nome);
                    novo.setTelefone(telefone);
                    novo.setEmail(email);
                    novo.setEstabelecimento(estabelecimento);
                    return clienteRepository.save(novo);
                });
    }

    // Cria novo cliente pelo painel — se telefone já existir, retorna o existente
    public Cliente criar(Cliente dados, Long estabelecimentoId) {
        // Se já existe cliente com esse telefone neste estabelecimento, retorna ele
        if (dados.getTelefone() != null && !dados.getTelefone().isBlank()) {
            var existente = clienteRepository
                    .findByTelefoneAndEstabelecimentoId(dados.getTelefone(), estabelecimentoId);
            if (existente.isPresent()) {
                return existente.get();
            }
        }

        Estabelecimento estabelecimento = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
        dados.setEstabelecimento(estabelecimento);
        dados.setBloqueado(false);
        dados.setTotalFaltas(0);
        return clienteRepository.save(dados);
    }

    public Cliente buscarPorId(Long id, Long estabelecimentoId) {
        return clienteRepository.findByIdAndEstabelecimentoId(id, estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
    }

    public List<Cliente> listar(Long estabelecimentoId) {
        return clienteRepository.findByEstabelecimentoId(estabelecimentoId);
    }

    public Cliente atualizar(Long id, Cliente dados, Long estabelecimentoId) {
        Cliente cliente = buscarPorId(id, estabelecimentoId);
        cliente.setNome(dados.getNome());
        cliente.setTelefone(dados.getTelefone());
        cliente.setEmail(dados.getEmail());
        cliente.setAlergias(dados.getAlergias());
        cliente.setPreferencias(dados.getPreferencias());
        cliente.setRestricoes(dados.getRestricoes());
        cliente.setObservacoes(dados.getObservacoes());
        return clienteRepository.save(cliente);
    }

    public void bloquear(Long id, String motivo, Long estabelecimentoId) {
        Cliente cliente = buscarPorId(id, estabelecimentoId);
        cliente.setBloqueado(true);
        cliente.setMotivoBloqueio(motivo);
        clienteRepository.save(cliente);
    }

    public void desbloquear(Long id, Long estabelecimentoId) {
        Cliente cliente = buscarPorId(id, estabelecimentoId);
        cliente.setBloqueado(false);
        cliente.setMotivoBloqueio(null);
        clienteRepository.save(cliente);
    }

    public void registrarFalta(Long id, Long estabelecimentoId) {
        Cliente cliente = buscarPorId(id, estabelecimentoId);
        cliente.setTotalFaltas(cliente.getTotalFaltas() + 1);
        clienteRepository.save(cliente);
    }
}