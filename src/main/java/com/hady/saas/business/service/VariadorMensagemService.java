package com.hady.saas.business.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Aplica variação de texto POR MENSAGEM (não por lote) nas campanhas de
 * disparo em massa, com o objetivo único de reduzir o padrão de "mensagem
 * idêntica em massa" que os sistemas anti-spam do WhatsApp detectam —
 * sem alterar o sentido/oferta que o dono do estabelecimento escreveu.
 *
 * Duas camadas de variação, ambas independentes entre si e sorteadas
 * separadamente pra cada destinatário:
 *
 *   1) Saudação de abertura (já existia em CampanhaEnvioService, migrado
 *      pra cá com uma lista bem maior).
 *   2) Substituição de frases de fechamento/CTA conhecidas por variações
 *      com o mesmo sentido — só troca frases que a gente reconhece
 *      explicitamente (ver CTA_VARIACOES), nunca mexe no texto livre da
 *      oferta em si ({{oferta}}, {{condicoes}}, etc. já resolvidos).
 *
 * Importante: a ORDEM de aplicação é sempre a mesma (saudação, depois CTA),
 * e cada chamada usa ThreadLocalRandom, então duas mensagens do mesmo
 * cliente em campanhas diferentes não saem idênticas, mas a mesma campanha
 * rodada de novo (se isso um dia existir) também não repetiria o padrão.
 */
@Service
public class VariadorMensagemService {

    // ── Saudações ────────────────────────────────────────────────────────────
    // Lista grande de propósito: sorteada por MENSAGEM, não por lote como
    // antes — com poucas opções e sorteio por lote, 30 clientes seguidos
    // recebiam a saudação idêntica, o que ainda é um padrão bem repetitivo.
    private static final List<String> SAUDACOES = List.of(
            "Olá", "Oi", "E aí", "Olá, tudo bem?", "Oi, tudo bem?",
            "Oi, tudo certo?", "Olá, tudo certo?", "Fala", "Bom te falar",
            "Oi, como vai?", "Olá, como vai?", "E aí, tudo bem?",
            "Oi, passando aqui", "Olá, passando aqui", "Tudo bem?"
    );

    // ── Variações de CTA/fechamento ─────────────────────────────────────────
    // Chave: um regex (case-insensitive) que casa com a frase EXATA usada
    // nos templates do catálogo (ver CampanhaTemplateCatalogo). Valor: lista
    // de alternativas com o mesmo sentido, uma delas sorteada na hora de
    // montar a mensagem. Cobre as frases mais repetidas entre os 22
    // templates predefinidos — mensagens totalmente customizadas que não
    // batem com nenhum padrão aqui simplesmente não são alteradas nessa
    // camada (só recebem a variação de saudação).
    private static final Map<Pattern, List<String>> CTA_VARIACOES = Map.ofEntries(
            entry("Responda essa mensagem pra garantir (o )?seu hor[aá]rio!?", List.of(
                    "Responda essa mensagem pra garantir seu horário!",
                    "Me chama por aqui pra garantir seu horário!",
                    "É só responder aqui que eu já separo seu horário!",
                    "Manda um oi aqui que eu garanto seu horário!",
                    "Responde essa mensagem que eu já reservo seu horário!"
            )),
            entry("Responda essa mensagem!?", List.of(
                    "Responda essa mensagem!",
                    "Me chama por aqui!",
                    "É só responder aqui!",
                    "Manda um oi que eu te ajudo!",
                    "Toca aqui pra saber mais!"
            )),
            entry("Não perca!? Responda essa mensagem pra garantir (o )?seu hor[aá]rio\\.?", List.of(
                    "Não perca! Responda essa mensagem pra garantir seu horário.",
                    "Corre que é por tempo limitado — me chama aqui!",
                    "Última chance, hein? Responde aqui pra garantir!",
                    "Não deixa pra depois, me chama por aqui!"
            )),
            entry("Garanta seu hor[aá]rio!? Responda essa mensagem\\.?", List.of(
                    "Garanta seu horário! Responda essa mensagem.",
                    "Já pode garantir seu horário, é só chamar aqui!",
                    "Corre e garanta seu horário — responde aqui!",
                    "Separa seu horário respondendo essa mensagem!"
            )),
            entry("Quer saber mais\\? Responda essa mensagem!?", List.of(
                    "Quer saber mais? Responda essa mensagem!",
                    "Quer saber mais? Me chama aqui!",
                    "Curioso(a)? Responde aqui que eu te conto tudo!"
            )),
            entry("Topa aproveitar\\? Responde aqui rapidinho!?", List.of(
                    "Topa aproveitar? Responde aqui rapidinho!",
                    "Bora aproveitar? Me chama rapidinho!",
                    "Vai querer? É só responder aqui!"
            )),
            entry("Que tal marcar um novo hor[aá]rio\\? [ÉE] s[oó] responder essa mensagem", List.of(
                    "Que tal marcar um novo horário? É só responder essa mensagem",
                    "Bora marcar um novo horário? Me chama aqui",
                    "Vamos marcar seu retorno? É só me chamar por aqui"
            ))
    );

    private static Map.Entry<Pattern, List<String>> entry(String regex, List<String> variacoes) {
        return Map.entry(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), variacoes);
    }

    /**
     * Sorteia uma saudação. Chamado por mensagem (por destinatário), não por
     * lote — cada cliente recebe uma abertura potencialmente diferente,
     * mesmo dentro do mesmo lote de disparo.
     */
    public String sortearSaudacao() {
        return SAUDACOES.get(ThreadLocalRandom.current().nextInt(SAUDACOES.size()));
    }

    /**
     * Aplica as variações de CTA/fechamento conhecidas sobre o corpo já
     * montado (saudação + texto livre do dono, com placeholders já
     * resolvidos). Frases que não batem com nenhum padrão conhecido saem
     * inalteradas — a função é aditiva, nunca reduz a mensagem a algo
     * genérico.
     */
    public String variarFechamento(String mensagem) {
        String resultado = mensagem;
        for (Map.Entry<Pattern, List<String>> par : CTA_VARIACOES.entrySet()) {
            Matcher m = par.getKey().matcher(resultado);
            if (m.find()) {
                List<String> alternativas = par.getValue();
                String escolhida = alternativas.get(ThreadLocalRandom.current().nextInt(alternativas.size()));
                resultado = m.replaceFirst(Matcher.quoteReplacement(escolhida));
                // Só aplica UMA substituição de CTA por mensagem — evitar
                // que duas regras diferentes casem no mesmo texto e deixem
                // a mensagem estranha por causa de dois replaces seguidos.
                break;
            }
        }
        return resultado;
    }
}