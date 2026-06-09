package com.hady.saas.business.service;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.profissional.Especialidade;
import com.hady.saas.domain.profissional.Profissional;
import com.hady.saas.domain.servico.Servico;
import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.repository.EspecialidadeRepository;
import com.hady.saas.infrastructure.repository.ProfissionalRepository;
import com.hady.saas.infrastructure.repository.ServicoRepository;
import com.hady.saas.infrastructure.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;
    private final EspecialidadeRepository especialidadeRepository;
    private final ServicoRepository servicoRepository;
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    public ProfissionalService(ProfissionalRepository profissionalRepository,
                               EspecialidadeRepository especialidadeRepository,
                               ServicoRepository servicoRepository,
                               UsuarioService usuarioService,
                               UsuarioRepository usuarioRepository) {
        this.profissionalRepository = profissionalRepository;
        this.especialidadeRepository = especialidadeRepository;
        this.servicoRepository = servicoRepository;
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Profissional cadastrar(Profissional profissional, Usuario usuarioLogin) {
        Usuario usuarioSalvo = usuarioService.salvarFuncionario(usuarioLogin);
        profissional.setUsuario(usuarioSalvo);
        return profissionalRepository.save(profissional);
    }

    public List<Profissional> listarAtivos(Long estabelecimentoId) {
        return profissionalRepository.findByEstabelecimentoIdAndAtivo(estabelecimentoId, true);
    }

    public Profissional buscarPorId(Long id, Long estabelecimentoId) {
        return profissionalRepository.findByIdAndEstabelecimentoId(id, estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));
    }

    public Profissional buscarPorUsuario(Long usuarioId) {
        return profissionalRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado para este usuário"));
    }

    public Profissional alterarStatus(Long id, Long estabelecimentoId, boolean ativo) {
        Profissional profissional = buscarPorId(id, estabelecimentoId);
        profissional.setAtivo(ativo);
        return profissionalRepository.save(profissional);
    }

    public Profissional atualizarComissao(Long id, Long estabelecimentoId, BigDecimal comissaoPercent) {
        Profissional profissional = buscarPorId(id, estabelecimentoId);
        profissional.setComissaoPercent(comissaoPercent != null ? comissaoPercent : BigDecimal.ZERO);
        return profissionalRepository.save(profissional);
    }

    public Profissional atualizarEspecialidades(Long profissionalId, List<Long> especialidadeIds, Long estabelecimentoId) {
        Profissional profissional = buscarPorId(profissionalId, estabelecimentoId);

        List<Especialidade> especialidades = especialidadeIds.stream()
                .map(espId -> especialidadeRepository.findById(espId)
                        .orElseThrow(() -> new RuntimeException("Especialidade não encontrada: " + espId)))
                .toList();

        profissional.setEspecialidades(especialidades);
        return profissionalRepository.save(profissional);
    }

    public List<Especialidade> listarEspecialidades(Long estabelecimentoId) {
        return especialidadeRepository.findByEstabelecimentoId(estabelecimentoId);
    }

    public Especialidade criarEspecialidade(String nome, Estabelecimento estabelecimento) {
        Especialidade especialidade = new Especialidade();
        especialidade.setNome(nome);
        especialidade.setEstabelecimento(estabelecimento);
        return especialidadeRepository.save(especialidade);
    }

    /**
     * Vincula uma lista de serviços a um profissional.
     *
     * Design N×N — o mesmo serviço pode estar vinculado a vários profissionais
     * ao mesmo tempo (ex: "Corte" vinculado a Ana, João e Maria).
     * A operação é idempotente: chama novamente com a mesma lista = mesmo resultado.
     *
     * Segurança: só aceita servicoIds que pertencem ao mesmo estabelecimento,
     * impedindo que um ADMIN vincule serviços de outro salão.
     */
    @Transactional
    public Profissional atualizarServicos(Long profissionalId, List<Long> servicoIds, Long estabelecimentoId) {
        Profissional profissional = buscarPorId(profissionalId, estabelecimentoId);

        // Filtra apenas os ids que realmente pertencem a este estabelecimento
        List<Servico> servicos = servicoIds == null || servicoIds.isEmpty()
                ? new java.util.ArrayList<>()
                : servicoRepository.findByIdInAndEstabelecimentoId(servicoIds, estabelecimentoId);

        profissional.setServicos(servicos);
        return profissionalRepository.save(profissional);
    }

    /**
     * Retorna os serviços vinculados a um profissional específico.
     * Usado pelo agendamento público para mostrar só o que aquele profissional faz.
     * Como a relação é N×N, o mesmo serviço pode aparecer em outros profissionais também.
     */
    @Transactional(readOnly = true)
    public List<Servico> listarServicosDoProfissional(Long profissionalId, Long estabelecimentoId) {
        Profissional profissional = buscarPorId(profissionalId, estabelecimentoId);
        // Força inicialização da coleção LAZY dentro da transação
        profissional.getServicos().size();
        return profissional.getServicos();
    }

    /**
     * Exclui um profissional e o usuário de login vinculado.
     * Remove primeiro o profissional (que tem FK para usuario),
     * depois exclui o usuário — ordem importa para evitar constraint violation.
     */
    @Transactional
    public void excluir(Long profissionalId, Long estabelecimentoId) {
        Profissional profissional = buscarPorId(profissionalId, estabelecimentoId);
        Long usuarioId = profissional.getUsuario() != null ? profissional.getUsuario().getId() : null;
        profissionalRepository.delete(profissional);
        if (usuarioId != null) {
            usuarioRepository.deleteById(usuarioId);
        }
    }
}