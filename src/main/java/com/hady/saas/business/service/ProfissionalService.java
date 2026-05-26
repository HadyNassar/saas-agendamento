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

    public Profissional cadastrar(Profissional profissional, Usuario usuarioLogin) {
        Usuario usuarioSalvo = usuarioService.salvarFuncionario(usuarioLogin);
        profissional.setUsuario(usuarioSalvo);
        return profissionalRepository.save(profissional);
    }

    public List<Profissional> listarAtivos(Long estabelecimentoId) {
        return profissionalRepository.findByEstabelecimentoIdAndAtivo(estabelecimentoId, true);
    }

    public Profissional buscarPorId(Long id, Long estabelecimentoId) {
        // ✅ filtra diretamente na query — evita LazyInitializationException
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
}
