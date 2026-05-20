package com.hady.saas.business.service;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.profissional.Especialidade;
import com.hady.saas.domain.profissional.Profissional;
import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.repository.EspecialidadeRepository;
import com.hady.saas.infrastructure.repository.ProfissionalRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;
    private final EspecialidadeRepository especialidadeRepository;
    private final UsuarioService usuarioService;

    public ProfissionalService(ProfissionalRepository profissionalRepository,
                               EspecialidadeRepository especialidadeRepository,
                               UsuarioService usuarioService) {
        this.profissionalRepository = profissionalRepository;
        this.especialidadeRepository = especialidadeRepository;
        this.usuarioService = usuarioService;
    }

    // Cria profissional + usuário de login de uma vez
    public Profissional cadastrar(Profissional profissional, Usuario usuarioLogin) {

        // Salva o usuário de login (valida limite do plano)
        Usuario usuarioSalvo = usuarioService.salvarFuncionario(usuarioLogin);

        // Vincula o usuário ao profissional
        profissional.setUsuario(usuarioSalvo);

        return profissionalRepository.save(profissional);
    }

    // Lista profissionais ativos do estabelecimento
    public List<Profissional> listarAtivos(Long estabelecimentoId) {
        return profissionalRepository.findByEstabelecimentoIdAndAtivo(estabelecimentoId, true);
    }

    // Busca profissional por ID validando que pertence ao estabelecimento
    public Profissional buscarPorId(Long id, Long estabelecimentoId) {
        Profissional profissional = profissionalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        if (!profissional.getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Profissional não pertence a este estabelecimento");
        }

        return profissional;
    }

    // Busca o profissional vinculado ao usuário logado
    public Profissional buscarPorUsuario(Long usuarioId) {
        return profissionalRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado para este usuário"));
    }

    // Ativa ou desativa profissional
    public Profissional alterarStatus(Long id, Long estabelecimentoId, boolean ativo) {
        Profissional profissional = buscarPorId(id, estabelecimentoId);
        profissional.setAtivo(ativo);
        return profissionalRepository.save(profissional);
    }

    // Atualiza especialidades do profissional
    public Profissional atualizarEspecialidades(Long profissionalId, List<Long> especialidadeIds, Long estabelecimentoId) {
        Profissional profissional = buscarPorId(profissionalId, estabelecimentoId);

        List<Especialidade> especialidades = especialidadeIds.stream()
                .map(espId -> especialidadeRepository.findById(espId)
                        .orElseThrow(() -> new RuntimeException("Especialidade não encontrada: " + espId)))
                .toList();

        profissional.setEspecialidades(especialidades);
        return profissionalRepository.save(profissional);
    }

    // Lista especialidades do estabelecimento
    public List<Especialidade> listarEspecialidades(Long estabelecimentoId) {
        return especialidadeRepository.findByEstabelecimentoId(estabelecimentoId);
    }

    // Cria uma especialidade nova
    public Especialidade criarEspecialidade(String nome, Estabelecimento estabelecimento) {
        Especialidade especialidade = new Especialidade();
        especialidade.setNome(nome);
        especialidade.setEstabelecimento(estabelecimento);
        return especialidadeRepository.save(especialidade);
    }
}
