package com.hady.saas.business.service;

import com.hady.saas.domain.anamnese.PerguntaAnamnese;
import com.hady.saas.domain.anamnese.RespostaAnamnese;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.PerguntaAnamneseRepository;
import com.hady.saas.infrastructure.repository.RespostaAnamneseRepository;
import org.springframework.stereotype.Service;

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

    // Lista perguntas ativas para exibir ao cliente
    public List<PerguntaAnamnese> listarPerguntasAtivas(Long estabelecimentoId) {
        return perguntaRepository.findByEstabelecimentoIdAndAtivaOrderByOrdem(estabelecimentoId, true);
    }

    // Lista todas as perguntas para o admin gerenciar
    public List<PerguntaAnamnese> listarTodasPerguntas(Long estabelecimentoId) {
        return perguntaRepository.findByEstabelecimentoIdOrderByOrdem(estabelecimentoId);
    }

    // Cria pergunta nova
    public PerguntaAnamnese criarPergunta(String texto, String tipo, int ordem, Estabelecimento estabelecimento) {
        PerguntaAnamnese pergunta = new PerguntaAnamnese();
        pergunta.setTexto(texto);
        pergunta.setTipo(tipo);
        pergunta.setOrdem(ordem);
        pergunta.setEstabelecimento(estabelecimento);
        return perguntaRepository.save(pergunta);
    }

    // Edita pergunta existente
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

    // Desativa pergunta (não deleta para manter histórico)
    public void desativarPergunta(Long perguntaId, Long estabelecimentoId) {
        PerguntaAnamnese pergunta = perguntaRepository.findById(perguntaId)
                .orElseThrow(() -> new RuntimeException("Pergunta não encontrada"));

        if (!pergunta.getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Pergunta não pertence a este estabelecimento");
        }

        pergunta.setAtiva(false);
        perguntaRepository.save(pergunta);
    }

    // ==================== RESPOSTAS ====================

    // Cliente preenche anamnese — só pode preencher uma vez
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

    // Usuário interno edita respostas do cliente
    public void editarRespostas(Long clienteId, Map<Long, RespostaDTO> respostas, Long estabelecimentoId) {

        respostas.forEach((perguntaId, dto) -> {
            PerguntaAnamnese pergunta = perguntaRepository.findById(perguntaId)
                    .orElseThrow(() -> new RuntimeException("Pergunta não encontrada: " + perguntaId));

            if (!pergunta.getEstabelecimento().getId().equals(estabelecimentoId)) {
                throw new RuntimeException("Pergunta não pertence a este estabelecimento");
            }

            // Busca resposta existente ou cria nova
            RespostaAnamnese resposta = respostaRepository
                    .findByClienteIdAndPerguntaId(clienteId, perguntaId)
                    .orElse(new RespostaAnamnese());

            if (resposta.getCliente() == null) {
                // Nova resposta criada pelo usuário
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

    // Busca respostas de um cliente para exibir no agendamento
    public List<RespostaAnamnese> buscarRespostasCliente(Long clienteId) {
        return respostaRepository.findByClienteId(clienteId);
    }

    // Verifica se cliente já preencheu anamnese
    public boolean clienteJaPreencheu(Long clienteId) {
        return respostaRepository.existsByClienteId(clienteId);
    }

    // DTO interno para receber respostas
    public static class RespostaDTO {
        private String resposta;
        private String detalhe;

        public String getResposta() { return resposta; }
        public String getDetalhe() { return detalhe; }
        public void setResposta(String resposta) { this.resposta = resposta; }
        public void setDetalhe(String detalhe) { this.detalhe = detalhe; }
    }
}
