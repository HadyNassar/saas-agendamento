package com.hady.saas.business.service;

import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    // Busca cliente pelo telefone — se não existir cria automaticamente
    // Usado no agendamento pelo link público
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

    public Cliente buscarPorId(Long id, Long estabelecimentoId) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        if (!cliente.getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Cliente não pertence a este estabelecimento");
        }

        return cliente;
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

    // Bloqueia cliente — apenas ADMIN pode fazer isso
    public void bloquear(Long id, String motivo, Long estabelecimentoId) {
        Cliente cliente = buscarPorId(id, estabelecimentoId);
        cliente.setBloqueado(true);
        cliente.setMotivoBloqueio(motivo);
        clienteRepository.save(cliente);
    }

    // Desbloqueia cliente
    public void desbloquear(Long id, Long estabelecimentoId) {
        Cliente cliente = buscarPorId(id, estabelecimentoId);
        cliente.setBloqueado(false);
        cliente.setMotivoBloqueio(null);
        clienteRepository.save(cliente);
    }

    // Registra falta do cliente
    public void registrarFalta(Long id, Long estabelecimentoId) {
        Cliente cliente = buscarPorId(id, estabelecimentoId);
        cliente.setTotalFaltas(cliente.getTotalFaltas() + 1);
        clienteRepository.save(cliente);
    }
}
