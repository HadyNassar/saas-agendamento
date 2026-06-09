package com.hady.saas.business.service;

import com.hady.saas.domain.anamnese.PerguntaAnamnese;
import com.hady.saas.domain.anamnese.RespostaAnamnese;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.PerguntaAnamneseRepository;
import com.hady.saas.infrastructure.repository.RespostaAnamneseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AnamneseService {

    private final PerguntaAnamneseRepository perguntaRepository;
    private final RespostaAnamneseRepository respostaRepository;

    public AnamneseService(PerguntaAnamneseRepository perguntaRepository,
                           RespostaAnamneseRepository respostaRepository) {
        this.perguntaRepository = perguntaRepository;
        this.respostaRepository = respostaRepository;
    }

    // ==================== PERGUNTAS ====================

    public List<PerguntaAnamnese> listarPerguntasAtivas(Long estabelecimentoId) {
        return perguntaRepository.findByEstabelecimentoIdAndAtivaOrderByOrdem(estabelecimentoId, true);
    }

    public List<PerguntaAnamnese> listarTodasPerguntas(Long estabelecimentoId) {
        return perguntaRepository.findByEstabelecimentoIdOrderByOrdem(estabelecimentoId);
    }

    public PerguntaAnamnese criarPergunta(String texto, String tipo, int ordem, Estabelecimento estabelecimento) {
        PerguntaAnamnese pergunta = new PerguntaAnamnese();
        pergunta.setTexto(texto);
        pergunta.setTipo(tipo);
        pergunta.setOrdem(ordem);
        pergunta.setEstabelecimento(estabelecimento);
        return perguntaRepository.save(pergunta);
    }

    public PerguntaAnamnese editarPergunta(Long perguntaId, String texto, String tipo,
                                           int ordem, Long estabelecimentoId) {
        PerguntaAnamnese pergunta = perguntaRepository.findById(perguntaId)
                .orElseThrow(() -> new RuntimeException("Pergunta não encontrada"));

        if (!pergunta.getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Pergunta não pertence a este estabelecimento");
        }

        pergunta.setTexto(texto);
        pergunta.setTipo(tipo);
        pergunta.setOrdem(ordem);
        return perguntaRepository.save(pergunta);
    }

    public void desativarPergunta(Long perguntaId, Long estabelecimentoId) {
        PerguntaAnamnese pergunta = perguntaRepository.findById(perguntaId)
                .orElseThrow(() -> new RuntimeException("Pergunta não encontrada"));

        if (!pergunta.getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Pergunta não pertence a este estabelecimento");
        }

        pergunta.setAtiva(false);
        perguntaRepository.save(pergunta);
    }

    public void ativarPergunta(Long perguntaId, Long estabelecimentoId) {
        PerguntaAnamnese pergunta = perguntaRepository.findById(perguntaId)
                .orElseThrow(() -> new RuntimeException("Pergunta não encontrada"));

        if (!pergunta.getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Pergunta não pertence a este estabelecimento");
        }

        pergunta.setAtiva(true);
        perguntaRepository.save(pergunta);
    }

    @Transactional
    public void excluirPergunta(Long perguntaId, Long estabelecimentoId) {
        PerguntaAnamnese pergunta = perguntaRepository.findById(perguntaId)
                .orElseThrow(() -> new RuntimeException("Pergunta não encontrada"));

        if (!pergunta.getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Pergunta não pertence a este estabelecimento");
        }

        // Remove as respostas vinculadas antes de excluir a pergunta
        respostaRepository.deleteByPerguntaId(perguntaId);
        perguntaRepository.delete(pergunta);
    }

    // ==================== RESPOSTAS ====================

    @Transactional
    public void preencherAnamnese(Cliente cliente, Map<Long, RespostaDTO> respostas) {
        if (respostaRepository.existsByClienteId(cliente.getId())) {
            throw new RuntimeException("Anamnese já preenchida. Apenas o estabelecimento pode editar.");
        }

        respostas.forEach((perguntaId, dto) -> {
            PerguntaAnamnese pergunta = perguntaRepository.findById(perguntaId)
                    .orElseThrow(() -> new RuntimeException("Pergunta não encontrada: " + perguntaId));

            RespostaAnamnese resposta = new RespostaAnamnese();
            resposta.setCliente(cliente);
            resposta.setPergunta(pergunta);
            resposta.setResposta(dto.getResposta());
            resposta.setDetalhe(dto.getDetalhe());
            resposta.setPreenchidoPeloCliente(true);
            respostaRepository.save(resposta);
        });
    }

    @Transactional
    public void editarRespostas(Long clienteId, Map<Long, RespostaDTO> respostas, Long estabelecimentoId) {
        respostas.forEach((perguntaId, dto) -> {
            PerguntaAnamnese pergunta = perguntaRepository.findById(perguntaId)
                    .orElseThrow(() -> new RuntimeException("Pergunta não encontrada: " + perguntaId));

            if (!pergunta.getEstabelecimento().getId().equals(estabelecimentoId)) {
                throw new RuntimeException("Pergunta não pertence a este estabelecimento");
            }

            RespostaAnamnese resposta = respostaRepository
                    .findByClienteIdAndPerguntaId(clienteId, perguntaId)
                    .orElse(new RespostaAnamnese());

            if (resposta.getCliente() == null) {
                Cliente cliente = new Cliente();
                cliente.setId(clienteId);
                resposta.setCliente(cliente);
                resposta.setPergunta(pergunta);
                resposta.setPreenchidoPeloCliente(false);
            }

            resposta.setResposta(dto.getResposta());
            resposta.setDetalhe(dto.getDetalhe());
            resposta.setEditadoEm(LocalDateTime.now());
            respostaRepository.save(resposta);
        });
    }

    public List<RespostaAnamnese> buscarRespostasCliente(Long clienteId) {
        return respostaRepository.findByClienteId(clienteId);
    }

    public boolean clienteJaPreencheu(Long clienteId) {
        return respostaRepository.existsByClienteId(clienteId);
    }

    public static class RespostaDTO {
        private String resposta;
        private String detalhe;

        public String getResposta() { return resposta; }
        public String getDetalhe() { return detalhe; }
        public void setResposta(String resposta) { this.resposta = resposta; }
        public void setDetalhe(String detalhe) { this.detalhe = detalhe; }
    }
}