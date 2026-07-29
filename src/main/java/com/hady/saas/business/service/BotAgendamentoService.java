package com.hady.saas.business.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.profissional.Profissional;
import com.hady.saas.domain.servico.Servico;
import com.hady.saas.domain.whatsapp.BotContexto;
import com.hady.saas.domain.whatsapp.ConversaWhatsApp;
import com.hady.saas.domain.horario.HorarioFuncionamento;
import com.hady.saas.infrastructure.repository.HorarioFuncionamentoRepository;
import com.hady.saas.infrastructure.repository.ProfissionalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Item 1 da camada ELITE (seção 5 do prompt mestre): bot de agendamento conversacional.
 *
 * Máquina de estados usando ConversaWhatsApp.estado + contexto (JSON simples via
 * ObjectMapper, guardado na coluna TEXT que já existia). Estados:
 *
 *   LIVRE -> AGUARDANDO_SERVICO -> AGUARDANDO_PROFISSIONAL -> AGUARDANDO_DATA
 *         -> AGUARDANDO_HORARIO -> CONFIRMANDO -> (agenda e volta pra LIVRE)
 *
 * Este serviço só decide o que responder por texto. Quem manda a mensagem de
 * volta pro cliente e grava o log de saída é o EvolutionWebhook (separação de
 * responsabilidade: aqui é regra de conversa, lá é I/O com a Evolution API).
 *
 * Item 7 (camada ELITE): fora do horário comercial o bot NÃO é bloqueado —
 * continua funcionando 24/7 normalmente (agenda, remarca etc., respeitando só
 * os horários vagos de sempre). A única diferença é que a mensagem de
 * fallback (quando nada bate com "agendar"/"remarcar") passa a informar o
 * horário de funcionamento quando é chamada fora do expediente. Ver
 * mensagemForaDoHorarioSeAplicavel().
 *
 * IMPORTANTE (regra de negócio já confirmada — ver seção 4 do prompt mestre):
 * isto NÃO tem nenhum acoplamento com WhatsAppService (mensagens automáticas
 * transacionais). Quando agendarPeloCliente() é chamado no fim do fluxo, ele já
 * dispara a notificação de confirmação normal por conta própria — por isso a
 * resposta do bot após confirmar é propositalmente curta, para não duplicar
 * informação que o cliente já vai receber em seguida pela via de sempre.
 */
@Service
public class BotAgendamentoService {

    private static final Logger log = LoggerFactory.getLogger(BotAgendamentoService.class);
    private static final ZoneId ZONA_BRASIL = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FMT_DATA_CURTA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FMT_DATA_LONGA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Limite de horários mostrados por mensagem, pra não estourar um WhatsApp
    // com uma lista gigante quando o dia está bem vazio. Escolha padrão minha,
    // não é regra de negócio validada — ajustar se o dono do produto preferir outro número.
    private static final int MAX_HORARIOS_EXIBIDOS = 12;

    // Se o cliente parar no meio de um fluxo (ex: travou em "AGUARDANDO_SERVICO")
    // e só voltar a escrever depois de muito tempo, não faz sentido continuar de
    // onde parou — melhor resetar pra LIVRE e deixar o menu principal decidir de
    // novo. 12h escolhido para cobrir "sumiu de um dia pro outro" sem incomodar
    // quem só demorou algumas horas pra responder no mesmo dia. Não é regra de
    // negócio validada — ajustar se o dono do produto preferir outro valor.
    private static final long HORAS_INATIVIDADE_PARA_RESETAR_FLUXO = 12;

    private final ServicoService servicoService;
    private final ProfissionalService profissionalService;
    private final ProfissionalRepository profissionalRepository;
    private final AgendamentoService agendamentoService;
    private final HorarioFuncionamentoRepository horarioFuncionamentoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BotAgendamentoService(ServicoService servicoService,
                                 ProfissionalService profissionalService,
                                 ProfissionalRepository profissionalRepository,
                                 AgendamentoService agendamentoService,
                                 HorarioFuncionamentoRepository horarioFuncionamentoRepository) {
        this.servicoService = servicoService;
        this.profissionalService = profissionalService;
        this.profissionalRepository = profissionalRepository;
        this.agendamentoService = agendamentoService;
        this.horarioFuncionamentoRepository = horarioFuncionamentoRepository;
    }

    /**
     * Processa a mensagem recebida e devolve o texto de resposta do bot.
     * Nunca retorna null — sempre há algo a dizer (mesmo que seja um pedido de
     * esclarecimento). Pode alterar conversa.estado e conversa.contexto, mas
     * quem persiste (conversaRepository.save) é o chamador (EvolutionWebhook).
     */
    @Transactional
    public String processarMensagem(Estabelecimento estabelecimento, Cliente cliente,
                                    ConversaWhatsApp conversa, String textoRecebido,
                                    LocalDateTime ultimaMensagemAnterior) {

        String texto = textoRecebido == null ? "" : textoRecebido.trim();
        String textoNormalizado = normalizar(texto);

        // Reseta o fluxo se o cliente sumiu por mais de
        // HORAS_INATIVIDADE_PARA_RESETAR_FLUXO no meio de uma conversa em
        // andamento (qualquer estado diferente de LIVRE). Não reseta se já está
        // em LIVRE (não há nada a resetar) nem se é a primeira mensagem dessa
        // conversa (ultimaMensagemAnterior == null). Feito ANTES de qualquer outra
        // regra, inclusive do estado de lembrete (item 2) — se o cliente demorou
        // 12h+ pra responder um lembrete, também não faz sentido tratar a resposta
        // atual como confirmação daquele lembrete específico.
        if (ultimaMensagemAnterior != null
                && !ConversaWhatsApp.ESTADO_LIVRE.equals(conversa.getEstado())
                && ultimaMensagemAnterior.plusHours(HORAS_INATIVIDADE_PARA_RESETAR_FLUXO).isBefore(LocalDateTime.now())) {
            log.info("[Bot Agendamento] Conversa id={} inativa há mais de {}h — resetando fluxo (estado anterior: {}).",
                    conversa.getId(), HORAS_INATIVIDADE_PARA_RESETAR_FLUXO, conversa.getEstado());
            resetar(conversa);
        }

        // Item 2 — PRIORIDADE MÁXIMA: se o cliente está respondendo a um lembrete
        // de 24h/2h, isso não pode ser interpretado como o atalho global de
        // "cancelar" (que aborta o fluxo de agendamento) nem cair no switch de
        // estados do bot de agendamento — "cancelar" aqui significa "cancelar
        // o agendamento", não "sair do fluxo de marcação".
        if (ConversaWhatsApp.ESTADO_AGUARDANDO_CONFIRMACAO_LEMBRETE.equals(conversa.getEstado())) {
            try {
                return tratarConfirmacaoLembrete(textoNormalizado, estabelecimento, conversa);
            } catch (Exception e) {
                log.error("[Bot Agendamento] Erro processando confirmação de lembrete da conversa id={}: {}",
                        conversa.getId(), e.getMessage(), e);
                resetar(conversa);
                return null;
            }
        }

        // Palavra de escape: funciona em qualquer estado, inclusive LIVRE (onde é um no-op silencioso).
        if (isCancelar(textoNormalizado)) {
            boolean tinhaFluxoEmAndamento = !ConversaWhatsApp.ESTADO_LIVRE.equals(conversa.getEstado());
            resetar(conversa);
            if (tinhaFluxoEmAndamento) {
                return "Ok, cancelei o agendamento em andamento. Quando quiser recomeçar, é só digitar *agendar* 😊";
            }
            return null; // "cancelar" fora de qualquer fluxo não precisa de resposta
        }

        String estado = conversa.getEstado() == null ? ConversaWhatsApp.ESTADO_LIVRE : conversa.getEstado();

        try {
            return switch (estado) {
                case ConversaWhatsApp.ESTADO_LIVRE -> tratarLivre(textoNormalizado, cliente, estabelecimento, conversa);
                case "AGUARDANDO_SERVICO" -> tratarAguardandoServico(texto, estabelecimento, conversa);
                case "AGUARDANDO_PROFISSIONAL" -> tratarAguardandoProfissional(texto, estabelecimento, conversa);
                case "AGUARDANDO_DATA" -> tratarAguardandoData(texto, estabelecimento, conversa);
                case "AGUARDANDO_HORARIO" -> tratarAguardandoHorario(texto, conversa);
                case "CONFIRMANDO" -> tratarConfirmando(texto, cliente, estabelecimento, conversa);
                case ConversaWhatsApp.ESTADO_AGUARDANDO_ESCOLHA_AGENDAMENTO ->
                        tratarAguardandoEscolhaAgendamento(texto, estabelecimento, conversa);
                case ConversaWhatsApp.ESTADO_AGUARDANDO_NOVO_HORARIO ->
                        tratarAguardandoNovoHorario(texto, cliente, estabelecimento, conversa);
                case ConversaWhatsApp.ESTADO_AGUARDANDO_MENU_PRINCIPAL ->
                        tratarAguardandoMenuPrincipal(textoNormalizado, cliente, estabelecimento, conversa);
                case ConversaWhatsApp.ESTADO_AGUARDANDO_ESCOLHA_CANCELAMENTO ->
                        tratarAguardandoEscolhaCancelamento(texto, estabelecimento, conversa);
                default -> {
                    resetar(conversa);
                    yield null;
                }
            };
        } catch (Exception e) {
            // Qualquer erro inesperado no meio do fluxo: não deixa o cliente travado
            // numa conversa quebrada, volta pro início.
            log.error("[Bot Agendamento] Erro inesperado processando estado '{}' da conversa id={}: {}",
                    estado, conversa.getId(), e.getMessage(), e);
            resetar(conversa);
            return "Ops, tive um probleminha aqui e perdi o fio da meada 😅 Pode digitar *agendar* que a gente recomeça?";
        }
    }

    // ── LIVRE ──────────────────────────────────────────────────────────────

    private String tratarLivre(String textoNormalizado, Cliente cliente, Estabelecimento estabelecimento, ConversaWhatsApp conversa) {
        // Item 3 — gatilho de reagendamento checado ANTES do gatilho de novo
        // agendamento: "remarcar"/"reagendar" também casaria com "agend" (ex:
        // "preciso reagendar"), então a intenção mais específica vence aqui.
        boolean quisReagendar = textoNormalizado.contains("remarcar")
                || textoNormalizado.contains("reagendar")
                || textoNormalizado.contains("mudar o horario")
                || textoNormalizado.contains("trocar o horario")
                || textoNormalizado.contains("mudar a data")
                || textoNormalizado.contains("adiar");

        if (quisReagendar) {
            return iniciarReagendamento(cliente, estabelecimento, conversa);
        }

        boolean quiseAgendar = textoNormalizado.contains("agend")
                || textoNormalizado.contains("marcar")
                || textoNormalizado.contains("horario")
                || textoNormalizado.contains("hora vaga");

        if (!quiseAgendar) {
            // Item 4 (menu principal) — ATUALIZADO: antes, só uma saudação pura
            // ("oi", "ola" etc., function isSaudacaoPura) abria o menu de opções;
            // qualquer outro texto não reconhecido caía numa mensagem fixa de
            // fallback. Isso fazia o bot "não responder direito" pra clientes que
            // digitavam algo diferente do esperado (erro de digitação, mensagem
            // fora do padrão, emoji sozinho etc.).
            //
            // Agora: QUALQUER mensagem que chegue com a conversa em estado LIVRE e
            // que não bata com um atalho direto reconhecido (reagendar/agendar já
            // tratados acima) abre o mesmo menu principal (Agendar/Reagendar/
            // Cancelar/Falar com atendente), garantindo que o cliente sempre recebe
            // o painel e consegue seguir o funil normalmente, não importa o que
            // tenha escrito. isSaudacaoPura() não é mais usada aqui (mantida só
            // para não quebrar quem ainda referencia o método), pois virou
            // redundante: toda mensagem não reconhecida já cai neste caminho.
            //
            // Item 7 (camada ELITE) continua valendo: fora do horário de
            // expediente do salão, a mensagem também avisa o horário de
            // funcionamento antes de cair no menu — dando prioridade a essa
            // informação por ser mais específica pro momento.
            String avisoForaDoHorario = mensagemForaDoHorarioSeAplicavel(estabelecimento);
            if (avisoForaDoHorario != null) {
                return avisoForaDoHorario;
            }
            return mostrarMenuPrincipal(conversa);
        }

        return iniciarFluxoDeAgendamento(estabelecimento, conversa);
    }

    // Extraído de tratarLivre para ser reaproveitado também pela opção
    // "1 - Agendar" do menu principal (item 4).
    private String iniciarFluxoDeAgendamento(Estabelecimento estabelecimento, ConversaWhatsApp conversa) {
        List<Servico> servicos = servicoService.listarAtivos(estabelecimento.getId());
        if (servicos.isEmpty()) {
            return "Ainda não temos serviços cadastrados por aqui — vou avisar a equipe do salão para te ajudar diretamente!";
        }

        BotContexto contexto = new BotContexto();
        contexto.setServicoIds(servicos.stream().map(Servico::getId).toList());
        salvarContexto(conversa, contexto);
        conversa.setEstado("AGUARDANDO_SERVICO");

        StringBuilder sb = new StringBuilder("Vamos marcar seu horário! 📅\n\nEscolha o serviço:\n");
        for (int i = 0; i < servicos.size(); i++) {
            Servico s = servicos.get(i);
            sb.append(i + 1).append(" - ").append(s.getNome())
                    .append(" (").append(s.getDuracaoMinutos()).append("min)\n");
        }
        sb.append("\nResponda só com o número da opção.");
        return sb.toString();
    }

    // ── RESPOSTA FORA DO HORÁRIO COMERCIAL (item 7) ─────────────────────────
    //
    // Escolha de design (não é regra de negócio validada — combinado que o mais
    // profissional é não incomodar o cliente): isto NÃO bloqueia o bot, que
    // continua funcionando 24/7 (agenda, remarca etc. normalmente, respeitando
    // só os horários vagos de sempre via HorarioFuncionamento). A única
    // diferença é a mensagem de fallback quando a mensagem do cliente não bate
    // com nenhum gatilho reconhecido (nem "agendar" nem "remarcar"): se
    // estiver fora do expediente nesse momento, avisamos o horário de
    // funcionamento em vez da dica genérica de sempre — já que não tem
    // ninguém do salão respondendo ao vivo agora.
    //
    // Retorna null se está dentro do expediente (ou sem horário configurado
    // pro dia de hoje, tratado como "sem info, não avisa nada").

    private String mensagemForaDoHorarioSeAplicavel(Estabelecimento estabelecimento) {
        LocalDateTime agora = LocalDateTime.now(ZONA_BRASIL);
        String diaSemana = agora.getDayOfWeek().name();

        Optional<HorarioFuncionamento> horarioOpt = horarioFuncionamentoRepository
                .findByEstabelecimentoIdAndDiaSemana(estabelecimento.getId(), diaSemana);

        if (horarioOpt.isEmpty() || !horarioOpt.get().isAberto()) {
            return "No momento estamos fora do horário de atendimento, mas pode deixar sua mensagem que a equipe " +
                    "vê assim que abrir! Se quiser, também posso marcar seu horário agora mesmo — é só digitar *agendar* 😊";
        }

        HorarioFuncionamento horario = horarioOpt.get();
        LocalTime horaAtual = agora.toLocalTime();
        boolean dentroDoExpediente = !horaAtual.isBefore(horario.getHoraAbertura())
                && horaAtual.isBefore(horario.getHoraFechamento());

        if (dentroDoExpediente) {
            return null; // está no horário — mantém a dica genérica de sempre
        }

        return "No momento estamos fora do horário de atendimento (funcionamos das " +
                horario.getHoraAbertura() + " às " + horario.getHoraFechamento() +
                "), mas pode deixar sua mensagem que a equipe vê assim que abrir! " +
                "Se quiser, também posso marcar seu horário agora mesmo — é só digitar *agendar* 😊";
    }

    // ── MENU PRINCIPAL (item 4) ──────────────────────────────────────────────
    //
    // Antes, "oi"/"ola" caíam junto do gatilho de agendar e iam direto pra
    // lista de serviços. Agora uma saudação pura mostra um menu curto com as
    // 4 ações mais comuns, para orientar quem não sabe (ou esqueceu) o que o
    // bot faz. Quem já sabe o que quer continua podendo pular o menu
    // digitando o atalho direto ("agendar", "remarcar" etc.) a qualquer momento.

    private boolean isSaudacaoPura(String textoNormalizado) {
        return textoNormalizado.equals("oi") || textoNormalizado.equals("ola")
                || textoNormalizado.equals("oii") || textoNormalizado.equals("oie")
                || textoNormalizado.equals("bom dia") || textoNormalizado.equals("boa tarde")
                || textoNormalizado.equals("boa noite") || textoNormalizado.equals("hey")
                || textoNormalizado.equals("eae") || textoNormalizado.equals("e ai");
    }

    private String mostrarMenuPrincipal(ConversaWhatsApp conversa) {
        resetar(conversa);
        conversa.setEstado(ConversaWhatsApp.ESTADO_AGUARDANDO_MENU_PRINCIPAL);
        return "Oi! 👋 Como posso te ajudar?\n\n" +
                "1 - Agendar 📅\n" +
                "2 - Reagendar 🔄\n" +
                "3 - Cancelar ❌\n" +
                "4 - Falar com um atendente 🧑\n\n" +
                "Responda só com o número da opção.";
    }

    private String tratarAguardandoMenuPrincipal(String textoNormalizado, Cliente cliente,
                                                 Estabelecimento estabelecimento, ConversaWhatsApp conversa) {
        switch (textoNormalizado) {
            case "1":
                return iniciarFluxoDeAgendamento(estabelecimento, conversa);
            case "2":
                return iniciarReagendamento(cliente, estabelecimento, conversa);
            case "3":
                return iniciarCancelamento(cliente, estabelecimento, conversa);
            case "4":
                return acionarAtendimentoHumano(conversa);
            default:
                return "Não entendi 🤔 Responda com o número de uma das opções:\n\n" +
                        "1 - Agendar\n2 - Reagendar\n3 - Cancelar\n4 - Falar com um atendente";
        }
    }

    // Item 4 — opção "4 - Falar com um atendente": pausa o bot nessa conversa
    // (mesmo mecanismo de robotPausado/pausadoAte usado quando alguém do salão
    // responde manualmente pelo painel — ver WhatsAppRoboController) por
    // algumas horas, para dar tempo de um humano assumir sem o bot competir
    // com as respostas. Reaproveita a mesma janela de retorno automático (20h)
    // usada no painel, por consistência — não é uma regra de negócio nova.
    private static final int HORA_RETORNO_AUTOMATICO_ATENDIMENTO_HUMANO = 20;

    private String acionarAtendimentoHumano(ConversaWhatsApp conversa) {
        resetar(conversa);
        conversa.setRobotPausado(true);
        conversa.setPausadoAte(calcularProximoRetornoAutomatico());
        return "Combinado! Um atendente da equipe vai te responder por aqui assim que possível 🙂\n\n" +
                "Enquanto isso eu fico de lado para não atrapalhar a conversa — pode escrever à vontade que fica registrado para a equipe ver.";
    }

    private LocalDateTime calcularProximoRetornoAutomatico() {
        LocalDateTime agora = LocalDateTime.now(ZONA_BRASIL);
        LocalDateTime hoje20h = agora.toLocalDate().atTime(HORA_RETORNO_AUTOMATICO_ATENDIMENTO_HUMANO, 0);
        return agora.isBefore(hoje20h) ? hoje20h : hoje20h.plusDays(1);
    }

    // ── AGUARDANDO_SERVICO ─────────────────────────────────────────────────

    private String tratarAguardandoServico(String texto, Estabelecimento estabelecimento, ConversaWhatsApp conversa) {
        BotContexto contexto = carregarContexto(conversa);
        if (contexto == null || contexto.getServicoIds().isEmpty()) {
            resetar(conversa);
            return "Perdi o fio da conversa aqui — pode digitar *agendar* de novo?";
        }

        Integer indice = parseIndice(texto, contexto.getServicoIds().size());
        if (indice == null) {
            return "Não entendi 🤔 Responda só com o número do serviço que apareceu na lista.";
        }

        Long servicoId = contexto.getServicoIds().get(indice - 1);
        Servico servico = servicoService.buscarPorId(servicoId, estabelecimento.getId());

        List<Profissional> profissionais = profissionalRepository
                .findAtivosPorServico(estabelecimento.getId(), servicoId);

        if (profissionais.isEmpty()) {
            resetar(conversa);
            return "Esse serviço não tem nenhum profissional disponível no momento. Vou avisar a equipe pra te ajudar por aqui mesmo!";
        }

        contexto.setServicoId(servico.getId());
        contexto.setServicoNome(servico.getNome());
        contexto.setServicoDuracaoMinutos(servico.getDuracaoMinutos());
        contexto.setProfissionalIds(profissionais.stream().map(Profissional::getId).toList());
        salvarContexto(conversa, contexto);
        conversa.setEstado("AGUARDANDO_PROFISSIONAL");

        StringBuilder sb = new StringBuilder("Ótima escolha! Com quem você prefere ser atendido(a)?\n\n");
        sb.append("0 - Sem preferência\n");
        for (int i = 0; i < profissionais.size(); i++) {
            sb.append(i + 1).append(" - ").append(profissionais.get(i).getNome()).append("\n");
        }
        sb.append("\nResponda com o número.");
        return sb.toString();
    }

    // ── AGUARDANDO_PROFISSIONAL ────────────────────────────────────────────

    private String tratarAguardandoProfissional(String texto, Estabelecimento estabelecimento, ConversaWhatsApp conversa) {
        BotContexto contexto = carregarContexto(conversa);
        if (contexto == null || contexto.getProfissionalIds().isEmpty()) {
            resetar(conversa);
            return "Perdi o fio da conversa aqui — pode digitar *agendar* de novo?";
        }

        Integer indice = parseIndiceComZero(texto, contexto.getProfissionalIds().size());
        if (indice == null) {
            return "Não entendi 🤔 Responda com o número de uma das opções (0 pra sem preferência).";
        }

        Long profissionalId = indice == 0
                ? contexto.getProfissionalIds().get(0)
                : contexto.getProfissionalIds().get(indice - 1);

        Profissional profissional = profissionalService.buscarPorId(profissionalId, estabelecimento.getId());

        contexto.setProfissionalId(profissional.getId());
        contexto.setProfissionalNome(profissional.getNome());
        salvarContexto(conversa, contexto);
        conversa.setEstado("AGUARDANDO_DATA");

        return "Combinado, " + profissional.getNome() + "! Para qual dia você quer marcar?\n" +
                "(pode responder *hoje*, *amanhã*, ou uma data tipo 28/07)";
    }

    // ── AGUARDANDO_DATA ─────────────────────────────────────────────────────

    private String tratarAguardandoData(String texto, Estabelecimento estabelecimento, ConversaWhatsApp conversa) {
        BotContexto contexto = carregarContexto(conversa);
        if (contexto == null || contexto.getProfissionalId() == null || contexto.getServicoDuracaoMinutos() == null) {
            resetar(conversa);
            return "Perdi o fio da conversa aqui — pode digitar *agendar* de novo?";
        }

        LocalDate dia = parseData(texto);
        if (dia == null) {
            return "Não consegui entender essa data 🤔 Tenta assim: *hoje*, *amanhã*, ou *28/07*.";
        }

        LocalDate hoje = LocalDate.now(ZONA_BRASIL);
        if (dia.isBefore(hoje)) {
            return "Essa data já passou! Me manda uma data a partir de hoje (" + hoje.format(FMT_DATA_CURTA) + ").";
        }
        LocalDate limite = hoje.plusMonths(estabelecimento.getMesesAntecipacao());
        if (dia.isAfter(limite)) {
            return "Só conseguimos agendar com até " + estabelecimento.getMesesAntecipacao() +
                    " mês(es) de antecedência. Tenta uma data até " + limite.format(FMT_DATA_CURTA) + ".";
        }

        List<LocalTime> livres;
        try {
            livres = agendamentoService.gerarSlotsDisponiveis(
                    contexto.getProfissionalId(), estabelecimento.getId(), dia, contexto.getServicoDuracaoMinutos());
        } catch (RuntimeException e) {
            // Ex: "Horario de funcionamento nao configurado para: SUNDAY"
            return "Não temos atendimento nesse dia (" + nomeDoDia(dia) + "). Tenta outra data?";
        }

        if (livres.isEmpty()) {
            return "Não sobrou nenhum horário livre em " + dia.format(FMT_DATA_CURTA) +
                    " pra esse profissional. Quer tentar outra data?";
        }

        List<LocalTime> exibidos = livres.size() > MAX_HORARIOS_EXIBIDOS
                ? livres.subList(0, MAX_HORARIOS_EXIBIDOS)
                : livres;

        contexto.setDataEscolhida(dia.toString());
        contexto.setHorariosDisponiveis(exibidos.stream().map(LocalTime::toString).toList());
        salvarContexto(conversa, contexto);
        conversa.setEstado("AGUARDANDO_HORARIO");

        StringBuilder sb = new StringBuilder("Horários livres em " + dia.format(FMT_DATA_CURTA) + ":\n\n");
        for (int i = 0; i < exibidos.size(); i++) {
            sb.append(i + 1).append(" - ").append(exibidos.get(i)).append("\n");
        }
        if (livres.size() > MAX_HORARIOS_EXIBIDOS) {
            sb.append("\n(mostrando os ").append(MAX_HORARIOS_EXIBIDOS).append(" primeiros horários)\n");
        }
        sb.append("\nResponda com o número do horário.");
        return sb.toString();
    }

    // Aceita "hoje", "amanha"/"amanhã", ou uma data no formato dd/MM ou dd/MM/yyyy.
    // Quando o ano não é informado (dd/MM), assume o ano corrente — e se a data
    // resultante já tiver passado, assume o ano seguinte (ex: cliente digita
    // "05/01" em dezembro, quer dizer o próximo janeiro, não o passado).
    // BUG PRÉ-EXISTENTE (não é regra de negócio validada — implementado agora
    // porque o método era chamado em tratarAguardandoData mas nunca tinha sido
    // escrito): retorna null se não conseguir interpretar o texto como data.
    private LocalDate parseData(String texto) {
        String t = normalizar(texto);

        if (t.equals("hoje")) return LocalDate.now(ZONA_BRASIL);
        if (t.equals("amanha")) return LocalDate.now(ZONA_BRASIL).plusDays(1);

        try {
            String[] partes = t.split("/");
            if (partes.length == 2) {
                int diaNum = Integer.parseInt(partes[0].trim());
                int mesNum = Integer.parseInt(partes[1].trim());
                int anoAtual = LocalDate.now(ZONA_BRASIL).getYear();
                LocalDate data = LocalDate.of(anoAtual, mesNum, diaNum);
                if (data.isBefore(LocalDate.now(ZONA_BRASIL))) {
                    data = data.plusYears(1);
                }
                return data;
            }
            if (partes.length == 3) {
                int diaNum = Integer.parseInt(partes[0].trim());
                int mesNum = Integer.parseInt(partes[1].trim());
                int anoNum = Integer.parseInt(partes[2].trim());
                return LocalDate.of(anoNum, mesNum, diaNum);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    // ── AGUARDANDO_HORARIO ──────────────────────────────────────────────────

    private String tratarAguardandoHorario(String texto, ConversaWhatsApp conversa) {
        BotContexto contexto = carregarContexto(conversa);
        if (contexto == null || contexto.getHorariosDisponiveis() == null || contexto.getHorariosDisponiveis().isEmpty()) {
            resetar(conversa);
            return "Perdi o fio da conversa aqui — pode digitar *agendar* de novo?";
        }

        Integer indice = parseIndice(texto, contexto.getHorariosDisponiveis().size());
        if (indice == null) {
            return "Não entendi 🤔 Responda só com o número do horário que apareceu na lista.";
        }

        String horarioEscolhido = contexto.getHorariosDisponiveis().get(indice - 1);
        contexto.setHorarioEscolhido(horarioEscolhido);
        salvarContexto(conversa, contexto);
        conversa.setEstado("CONFIRMANDO");

        LocalDate dia = LocalDate.parse(contexto.getDataEscolhida());

        return "Confere pra mim:\n\n" +
                "📋 *Serviço:* " + contexto.getServicoNome() + "\n" +
                "💇 *Profissional:* " + contexto.getProfissionalNome() + "\n" +
                "📅 *Data:* " + dia.format(FMT_DATA_LONGA) + "\n" +
                "🕐 *Horário:* " + horarioEscolhido + "\n\n" +
                "1 - Sim, confirmar ✅\n" +
                "2 - Não, cancelar ❌";
    }

    // ── CONFIRMANDO ─────────────────────────────────────────────────────────

    private String tratarConfirmando(String texto, Cliente cliente, Estabelecimento estabelecimento, ConversaWhatsApp conversa) {
        String t = normalizar(texto);

        if (t.equals("2") || t.contains("nao") || t.contains("cancelar")) {
            resetar(conversa);
            return "Sem problemas, não confirmei nada. Quando quiser recomeçar, digite *agendar* 😊";
        }

        if (!t.equals("1") && !t.contains("sim") && !t.contains("confirmar")) {
            return "Não entendi. Responda *1* para confirmar ou *2* para cancelar.";
        }

        BotContexto contexto = carregarContexto(conversa);
        if (contexto == null || contexto.getServicoId() == null || contexto.getProfissionalId() == null
                || contexto.getDataEscolhida() == null || contexto.getHorarioEscolhido() == null) {
            resetar(conversa);
            return "Perdi o fio da conversa aqui — pode digitar *agendar* de novo?";
        }

        Servico servico = servicoService.buscarPorId(contexto.getServicoId(), estabelecimento.getId());
        Profissional profissional = profissionalService.buscarPorId(contexto.getProfissionalId(), estabelecimento.getId());
        LocalDate dia = LocalDate.parse(contexto.getDataEscolhida());
        LocalTime hora = LocalTime.parse(contexto.getHorarioEscolhido());
        LocalDateTime inicio = LocalDateTime.of(dia, hora);

        try {
            Agendamento agendamento = agendamentoService.agendarPeloCliente(
                    cliente, profissional, servico, estabelecimento, inicio, false);
            resetar(conversa);
            // Resposta curta de propósito: agendarPeloCliente() já dispara, por
            // conta própria, a notificação de confirmação de sempre (WhatsAppService),
            // com todos os detalhes — não repetir aqui pra não duplicar mensagem.
            String primeiroNome = cliente.getNome() != null && !cliente.getNome().isBlank()
                    ? cliente.getNome().split(" ")[0]
                    : "";
            return "🎉 Prontinho" + (primeiroNome.isBlank() ? "" : ", " + primeiroNome) +
                    "! Já confirmei seu agendamento (nº " + agendamento.getId() + ") — os detalhes chegam em seguida por aqui.";
        } catch (RuntimeException e) {
            resetar(conversa);
            return "Não consegui concluir o agendamento: " + e.getMessage() +
                    "\n\nVamos tentar de novo? Digite *agendar* para recomeçar.";
        }
    }

    // ── REAGENDAMENTO SELF-SERVICE (item 3) ─────────────────────────────────
    //
    // LIVRE -> [0 ou 1 agendamento: pula direto] / [2+: AGUARDANDO_ESCOLHA_AGENDAMENTO]
    //       -> AGUARDANDO_NOVO_HORARIO -> (cancela o antigo, cria o novo, volta pra LIVRE)
    //
    // Decisão de design (ver seção 0/5 do prompt mestre — não era regra de negócio
    // validada, escolha padrão minha): reagendar = CANCELAR o agendamento antigo +
    // CRIAR um novo, reaproveitando cancelar()/agendarPeloCliente() já testados.
    // Isso também significa que os disparos automáticos de cancelamento e de
    // confirmação de sempre (WhatsAppService/NotificacaoService) acontecem
    // sozinhos, sem esse serviço precisar duplicar nada — mesmo padrão do
    // restante do bot. O reagendamento fica sempre com o MESMO profissional do
    // agendamento original (o cliente não escolhe profissional de novo aqui).
    //
    // Quando há mais de um agendamento futuro elegível, listamos todos (também
    // escolha padrão minha) em vez de assumir sempre o próximo — mais seguro:
    // evita reagendar o compromisso errado por adivinhação.

    private String iniciarReagendamento(Cliente cliente, Estabelecimento estabelecimento, ConversaWhatsApp conversa) {
        List<Agendamento> futuros = agendamentoService.listarFuturosAtivosDoCliente(cliente.getId(), estabelecimento.getId());

        if (futuros.isEmpty()) {
            return "Não encontrei nenhum agendamento futuro seu por aqui pra remarcar. Se quiser marcar um novo horário, digite *agendar*.";
        }

        if (futuros.size() == 1) {
            return prepararEscolhaDeNovoHorario(futuros.get(0), conversa);
        }

        BotContexto contexto = new BotContexto();
        contexto.setAgendamentosParaEscolherIds(futuros.stream().map(Agendamento::getId).toList());
        salvarContexto(conversa, contexto);
        conversa.setEstado(ConversaWhatsApp.ESTADO_AGUARDANDO_ESCOLHA_AGENDAMENTO);

        StringBuilder sb = new StringBuilder("Encontrei mais de um agendamento seu. Qual deles você quer remarcar?\n\n");
        for (int i = 0; i < futuros.size(); i++) {
            sb.append(i + 1).append(" - ").append(resumoAgendamento(futuros.get(i))).append("\n");
        }
        sb.append("\nResponda só com o número da opção.");
        return sb.toString();
    }

    // ── CANCELAMENTO PELO MENU (item 4, opção 3) ────────────────────────────
    //
    // Reaproveita agendamentoService.listarFuturosAtivosDoCliente/cancelar,
    // já testados no fluxo de lembrete — mesmo padrão do reagendamento
    // (iniciarReagendamento), inclusive na escolha quando há mais de um
    // agendamento futuro ativo. Diferente do reagendamento, aqui não há um
    // segundo passo (escolher novo horário): ao identificar o agendamento,
    // já cancela direto.

    private String iniciarCancelamento(Cliente cliente, Estabelecimento estabelecimento, ConversaWhatsApp conversa) {
        List<Agendamento> futuros = agendamentoService.listarFuturosAtivosDoCliente(cliente.getId(), estabelecimento.getId());

        if (futuros.isEmpty()) {
            resetar(conversa);
            return "Não encontrei nenhum agendamento futuro seu por aqui para cancelar. Se quiser marcar um novo horário, digite *agendar*.";
        }

        if (futuros.size() == 1) {
            return efetivarCancelamento(futuros.get(0), estabelecimento, conversa);
        }

        BotContexto contexto = new BotContexto();
        contexto.setAgendamentosParaCancelarIds(futuros.stream().map(Agendamento::getId).toList());
        salvarContexto(conversa, contexto);
        conversa.setEstado(ConversaWhatsApp.ESTADO_AGUARDANDO_ESCOLHA_CANCELAMENTO);

        StringBuilder sb = new StringBuilder("Encontrei mais de um agendamento seu. Qual deles você quer cancelar?\n\n");
        for (int i = 0; i < futuros.size(); i++) {
            sb.append(i + 1).append(" - ").append(resumoAgendamento(futuros.get(i))).append("\n");
        }
        sb.append("\nResponda só com o número da opção, ou digite *cancelar* para desistir.");
        return sb.toString();
    }

    private String tratarAguardandoEscolhaCancelamento(String texto, Estabelecimento estabelecimento, ConversaWhatsApp conversa) {
        BotContexto contexto = carregarContexto(conversa);
        if (contexto == null || contexto.getAgendamentosParaCancelarIds().isEmpty()) {
            resetar(conversa);
            return "Perdi o fio da conversa aqui — pode digitar *cancelar* de novo, ou *agendar* para marcar um novo horário.";
        }

        Integer indice = parseIndice(texto, contexto.getAgendamentosParaCancelarIds().size());
        if (indice == null) {
            return "Não entendi 🤔 Responda só com o número do agendamento que apareceu na lista.";
        }

        Long agendamentoId = contexto.getAgendamentosParaCancelarIds().get(indice - 1);
        Agendamento agendamento = agendamentoService.buscarPorId(agendamentoId, estabelecimento.getId());
        return efetivarCancelamento(agendamento, estabelecimento, conversa);
    }

    private String efetivarCancelamento(Agendamento agendamento, Estabelecimento estabelecimento, ConversaWhatsApp conversa) {
        String resumo = resumoAgendamento(agendamento);
        resetar(conversa);
        try {
            // agendamentoService.cancelar já dispara, por conta própria, a
            // notificação de cancelamento de sempre — resposta do bot
            // propositalmente curta para não duplicar informação (mesmo
            // padrão documentado no topo da classe).
            agendamentoService.cancelar(agendamento.getId(), estabelecimento.getId());
        } catch (Exception e) {
            return "Não consegui cancelar: " + e.getMessage() + ".";
        }
        return "Prontinho, cancelei este agendamento:\n" + resumo +
                "\n\nSe quiser marcar um novo horário, é só digitar *agendar* 😊";
    }

    // ── AGUARDANDO_ESCOLHA_AGENDAMENTO ──────────────────────────────────────

    private String tratarAguardandoEscolhaAgendamento(String texto, Estabelecimento estabelecimento, ConversaWhatsApp conversa) {
        BotContexto contexto = carregarContexto(conversa);
        if (contexto == null || contexto.getAgendamentosParaEscolherIds().isEmpty()) {
            resetar(conversa);
            return "Perdi o fio da conversa aqui — pode digitar *remarcar* de novo?";
        }

        Integer indice = parseIndice(texto, contexto.getAgendamentosParaEscolherIds().size());
        if (indice == null) {
            return "Não entendi 🤔 Responda só com o número do agendamento que apareceu na lista.";
        }

        Long agendamentoId = contexto.getAgendamentosParaEscolherIds().get(indice - 1);
        Agendamento agendamento = agendamentoService.buscarPorId(agendamentoId, estabelecimento.getId());
        return prepararEscolhaDeNovoHorario(agendamento, conversa);
    }

    /**
     * Gera os próximos horários livres do MESMO profissional do agendamento
     * original (reaproveitando gerarSlotsDisponiveis, igual ao item 1) e coloca
     * a conversa em AGUARDANDO_NOVO_HORARIO. Compartilhado pelos dois casos:
     * só um agendamento futuro (pula direto pra cá) e mais de um (depois da
     * escolha em AGUARDANDO_ESCOLHA_AGENDAMENTO).
     */
    private String prepararEscolhaDeNovoHorario(Agendamento agendamento, ConversaWhatsApp conversa) {
        Long profissionalId = agendamento.getProfissional().getId();
        int duracaoMinutos = (int) java.time.Duration.between(
                agendamento.getDataHoraInicio(), agendamento.getDataHoraFim()).toMinutes();

        LocalDate hoje = LocalDate.now(ZONA_BRASIL);

        BotContexto contexto = new BotContexto();
        contexto.setAgendamentoOriginalId(agendamento.getId());
        contexto.setReagendamentoProfissionalId(profissionalId);
        contexto.setReagendamentoServicoDuracaoMinutos(duracaoMinutos);
        contexto.setDataEscolhida(hoje.toString());

        String resultado = buscarProximosHorariosParaReagendamento(profissionalId, agendamento.getEstabelecimento().getId(),
                duracaoMinutos, hoje, contexto);
        if (resultado != null) {
            // Nenhum horário livre nos próximos dias — devolve a mensagem de erro
            // e mantém a conversa livre (não trava o cliente num fluxo sem saída).
            resetar(conversa);
            return resultado;
        }

        salvarContexto(conversa, contexto);
        conversa.setEstado(ConversaWhatsApp.ESTADO_AGUARDANDO_NOVO_HORARIO);

        LocalDate dia = LocalDate.parse(contexto.getDataEscolhida());
        return montarMensagemHorarios(contexto, dia, resumoAgendamento(agendamento));
    }

    // Escolha padrão minha (não validada): tenta achar horários livres olhando
    // dia a dia, a partir de hoje, até MAX_DIAS_BUSCA_REAGENDAMENTO dias à frente,
    // parando no primeiro dia com pelo menos 1 horário livre. Evita mostrar uma
    // lista vazia quando o dia de hoje já não tem mais vaga.
    private static final int MAX_DIAS_BUSCA_REAGENDAMENTO = 14;

    /** Retorna null se achou horários (já salvos em contexto), ou uma mensagem de erro caso não ache nenhum. */
    private String buscarProximosHorariosParaReagendamento(Long profissionalId, Long estabelecimentoId,
                                                           int duracaoMinutos, LocalDate diaInicial, BotContexto contexto) {
        LocalDate dia = diaInicial;
        for (int i = 0; i < MAX_DIAS_BUSCA_REAGENDAMENTO; i++) {
            List<LocalTime> livres;
            try {
                livres = agendamentoService.gerarSlotsDisponiveis(profissionalId, estabelecimentoId, dia, duracaoMinutos);
            } catch (RuntimeException e) {
                dia = dia.plusDays(1);
                continue; // sem atendimento nesse dia (ex: domingo) — tenta o próximo
            }
            if (!livres.isEmpty()) {
                List<LocalTime> exibidos = livres.size() > MAX_HORARIOS_EXIBIDOS
                        ? livres.subList(0, MAX_HORARIOS_EXIBIDOS)
                        : livres;
                contexto.setDataEscolhida(dia.toString());
                contexto.setHorariosDisponiveis(exibidos.stream().map(LocalTime::toString).toList());
                return null;
            }
            dia = dia.plusDays(1);
        }
        return "Não encontrei nenhum horário livre com esse profissional nos próximos dias 😕 " +
                "Vou avisar a equipe do salão para te ajudar a encontrar um horário.";
    }

    private String montarMensagemHorarios(BotContexto contexto, LocalDate dia, String resumoAgendamentoAtual) {
        StringBuilder sb = new StringBuilder();
        sb.append("Combinado! Vamos remarcar:\n").append(resumoAgendamentoAtual).append("\n\n");
        sb.append("Horários livres em ").append(dia.format(FMT_DATA_CURTA)).append(":\n\n");
        List<String> horarios = contexto.getHorariosDisponiveis();
        for (int i = 0; i < horarios.size(); i++) {
            sb.append(i + 1).append(" - ").append(horarios.get(i)).append("\n");
        }
        sb.append("\nResponda com o número do horário, ou digite *cancelar* para desistir da remarcação.");
        return sb.toString();
    }

    // ── AGUARDANDO_NOVO_HORARIO ──────────────────────────────────────────────

    private String tratarAguardandoNovoHorario(String texto, Cliente cliente, Estabelecimento estabelecimento, ConversaWhatsApp conversa) {
        BotContexto contexto = carregarContexto(conversa);
        if (contexto == null || contexto.getHorariosDisponiveis() == null || contexto.getHorariosDisponiveis().isEmpty()
                || contexto.getAgendamentoOriginalId() == null) {
            resetar(conversa);
            return "Perdi o fio da conversa aqui — pode digitar *remarcar* de novo?";
        }

        Integer indice = parseIndice(texto, contexto.getHorariosDisponiveis().size());
        if (indice == null) {
            return "Não entendi 🤔 Responda só com o número do horário que apareceu na lista.";
        }

        String horarioEscolhido = contexto.getHorariosDisponiveis().get(indice - 1);
        LocalDate dia = LocalDate.parse(contexto.getDataEscolhida());
        LocalDateTime novoInicio = LocalDateTime.of(dia, LocalTime.parse(horarioEscolhido));

        Agendamento original = agendamentoService.buscarPorId(contexto.getAgendamentoOriginalId(), estabelecimento.getId());
        Long agendamentoOriginalId = original.getId();

        try {
            // Cancela o antigo primeiro (libera o horário e dispara a notificação
            // de cancelamento de sempre); se o novo falhar por algum motivo (ex:
            // conflito de última hora), o cliente já foi avisado do cancelamento
            // e pode digitar *agendar* pra marcar manualmente — cenário raro dado
            // que acabamos de consultar os slots livres, mas possível em corrida.
            agendamentoService.cancelar(agendamentoOriginalId, estabelecimento.getId());

            Agendamento novo = agendamentoService.agendarPeloCliente(
                    cliente, original.getProfissional(), original.getServico(), estabelecimento, novoInicio, false);

            resetar(conversa);
            // Resposta curta de propósito, mesmo padrão do item 1: agendarPeloCliente()
            // já dispara a notificação de confirmação de sempre com os detalhes.
            String primeiroNome = cliente.getNome() != null && !cliente.getNome().isBlank()
                    ? cliente.getNome().split(" ")[0]
                    : "";
            return "🎉 Prontinho" + (primeiroNome.isBlank() ? "" : ", " + primeiroNome) +
                    "! Remarquei seu agendamento (nº " + novo.getId() + ") — os detalhes chegam em seguida por aqui.";
        } catch (RuntimeException e) {
            resetar(conversa);
            return "Não consegui concluir a remarcação: " + e.getMessage() +
                    "\n\nSeu agendamento antigo pode ter sido cancelado nesse processo — dá uma olhada e, se precisar, digite *agendar* para marcar um novo horário.";
        }
    }

    private String resumoAgendamento(Agendamento agendamento) {
        LocalDateTime inicio = agendamento.getDataHoraInicio();
        return agendamento.getServico().getNome() + " com " + agendamento.getProfissional().getNome() +
                " em " + inicio.toLocalDate().format(FMT_DATA_LONGA) + " às " + inicio.toLocalTime();
    }

    // ── AGUARDANDO_CONFIRMACAO_LEMBRETE (item 2) ────────────────────────────
    // Estado disparado por LembreteConfirmacaoService quando um lembrete de
    // 24h/2h é enviado com o prompt "responda 1 para confirmar, 2 para cancelar".

    private String tratarConfirmacaoLembrete(String textoNormalizado, Estabelecimento estabelecimento, ConversaWhatsApp conversa) {
        BotContexto contexto = carregarContexto(conversa);
        if (contexto == null || contexto.getAgendamentoId() == null) {
            resetar(conversa);
            return null; // nada a fazer, silencioso
        }

        boolean confirmar = textoNormalizado.equals("1") || textoNormalizado.contains("confirmar") || textoNormalizado.contains("sim");
        boolean cancelar = textoNormalizado.equals("2") || textoNormalizado.contains("cancelar") || textoNormalizado.contains("nao");

        if (!confirmar && !cancelar) {
            // Mantém o estado — o cliente pode responder de novo, corretamente,
            // à mesma pergunta (não reseta o fluxo por uma resposta não reconhecida).
            return "Não entendi 🤔 Responda *1* para confirmar presença ou *2* para cancelar o agendamento.";
        }

        Long agendamentoId = contexto.getAgendamentoId();
        resetar(conversa);

        if (confirmar) {
            try {
                agendamentoService.confirmarPresenca(agendamentoId, estabelecimento.getId());
                return "Prontinho! Presença confirmada ✅ Te esperamos! 😊";
            } catch (RuntimeException e) {
                return "Não consegui confirmar: " + e.getMessage() + ".";
            }
        }

        // cancelar
        try {
            // agendamentoService.cancelar já dispara, por conta própria, a
            // notificação de cancelamento de sempre (WhatsAppService) — resposta
            // do bot aqui é só um reconhecimento curto, sem duplicar detalhes.
            agendamentoService.cancelar(agendamentoId, estabelecimento.getId());
            return "Ok, cancelei seu agendamento. Os detalhes chegam em seguida por aqui. Quando quiser remarcar, digite *agendar*.";
        } catch (RuntimeException e) {
            return "Não consegui cancelar: " + e.getMessage() + ".";
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void resetar(ConversaWhatsApp conversa) {
        conversa.setEstado(ConversaWhatsApp.ESTADO_LIVRE);
        conversa.setContexto(null);
    }

    private void salvarContexto(ConversaWhatsApp conversa, BotContexto contexto) {
        try {
            conversa.setContexto(objectMapper.writeValueAsString(contexto));
        } catch (Exception e) {
            throw new RuntimeException("Falha ao serializar contexto do bot", e);
        }
    }

    private BotContexto carregarContexto(ConversaWhatsApp conversa) {
        if (conversa.getContexto() == null || conversa.getContexto().isBlank()) return null;
        try {
            return objectMapper.readValue(conversa.getContexto(), BotContexto.class);
        } catch (Exception e) {
            log.warn("[Bot Agendamento] Contexto ilegível na conversa id={}: {}", conversa.getId(), e.getMessage());
            return null;
        }
    }

    private boolean isCancelar(String textoNormalizado) {
        return textoNormalizado.equals("cancelar") || textoNormalizado.equals("parar")
                || textoNormalizado.equals("sair") || textoNormalizado.equals("desistir");
    }

    /** Índice 1-based dentro de [1, tamanho]. Retorna null se inválido. */
    private Integer parseIndice(String texto, int tamanho) {
        try {
            int n = Integer.parseInt(texto.trim());
            if (n < 1 || n > tamanho) return null;
            return n;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Igual a parseIndice, mas aceita 0 como valor especial (ex: "sem preferência"). */
    private Integer parseIndiceComZero(String texto, int tamanho) {
        try {
            int n = Integer.parseInt(texto.trim());
            if (n < 0 || n > tamanho) return null;
            return n;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String nomeDoDia(LocalDate dia) {
        DayOfWeek dow = dia.getDayOfWeek();
        return dow.getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
    }

    /** Minúsculo + sem acento, pra casar palavras-chave digitadas de qualquer jeito. */
    private String normalizar(String texto) {
        if (texto == null) return "";
        String semAcento = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase().trim();
    }
}