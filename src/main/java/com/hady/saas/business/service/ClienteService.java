package com.hady.saas.business.service;

import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.ClienteRepository;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    public Cliente criar(Cliente dados, Long estabelecimentoId) {
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

    // ─── Listagem paginada com busca unificada e filtro de bloqueados ─────────
    public Page<Cliente> listarPaginado(Long estabelecimentoId, int page, int size,
                                        String busca, Boolean bloqueado) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());

        if (busca != null && !busca.isBlank()) {
            return clienteRepository.buscarPorNomeTelefoneOuEmail(
                    estabelecimentoId, busca.trim(), pageable);
        }
        if (bloqueado != null) {
            return clienteRepository.findByEstabelecimentoIdAndBloqueado(
                    estabelecimentoId, bloqueado, pageable);
        }
        return clienteRepository.findByEstabelecimentoId(estabelecimentoId, pageable);
    }

    // Sobrecarga para compatibilidade com chamadas sem filtro de bloqueado
    public Page<Cliente> listarPaginado(Long estabelecimentoId, int page, int size, String busca) {
        return listarPaginado(estabelecimentoId, page, size, busca, null);
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