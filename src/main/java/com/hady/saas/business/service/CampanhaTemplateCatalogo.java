package com.hady.saas.business.service;

import com.hady.saas.application.dto.CampoTemplateCampanhaDTO;
import com.hady.saas.application.dto.CampanhaTemplateDTO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Catálogo fixo de campanhas predefinidas (pedido do dono do produto:
 * "pelo menos umas 20 campanhas predefinidas"). Vive no back de propósito —
 * é dado, não é UI: front, app mobile ou qualquer cliente futuro consomem
 * o mesmo catálogo via GET /painel/whatsapp/campanhas/templates sem
 * duplicar texto/regra em cada lugar.
 *
 * Cada template é genérico o suficiente pra qualquer TipoEstabelecimento
 * (salão, barbearia, clínica de estética, personal trainer, psicólogo,
 * odontologia, pilates, etc.) — nunca menciona um serviço específico de um
 * segmento só, sempre "atendimento", "horário", "aqui na {{nome}}". Por
 * isso não existe 1 catálogo por TipoEstabelecimento: um catálogo genérico
 * bem escrito cobre todos eles, e evita manter 20+ templates × 23 tipos.
 *
 * mensagemBase é o texto livre que entra no campo `mensagem` de
 * CriarCampanhaEnvioDTO — a saudação com o nome do cliente já é adicionada
 * automaticamente depois por CampanhaEnvioService, então nenhum template
 * aqui começa com "Olá" ou repete o nome do cliente.
 */
@Component
public class CampanhaTemplateCatalogo {

    private static final List<CampanhaTemplateDTO> TEMPLATES = List.of(

        template("promocao_geral", "Promoção", "Promoção com desconto",
            "Ideal pra divulgar um desconto por tempo limitado.",
            "🎉 Preparamos uma novidade especial pra você: {{oferta}}\n\n" +
            "{{condicoes}}\n\nResponda essa mensagem pra garantir o seu horário!",
            campo("oferta", "Qual é a oferta?", "Ex: 20% de desconto em qualquer atendimento", true, false),
            campo("condicoes", "Condições / validade", "Ex: válido até domingo, não cumulativo com outras promoções", false, true)
        ),

        template("reativacao_inativo", "Reativação", "Reconquistar cliente inativo",
            "Pra clientes que não aparecem há um tempo (segmento INATIVO).",
            "🥺 Sentimos sua falta por aqui! Faz tempo que a gente não se vê.\n\n" +
            "{{oferta}}\n\nQue tal marcar um novo horário? É só responder essa mensagem 📅",
            campo("oferta", "O que oferecer pra ele voltar?", "Ex: preparamos uma condição especial pra sua volta", true, false)
        ),

        template("aniversario_cliente", "Data especial", "Feliz aniversário",
            "Mensagem de aniversário com um mimo.",
            "🎂 Muitas felicidades pelo seu dia! Ficamos muito felizes em fazer parte da sua vida.\n\n" +
            "{{presente}}\n\nEsperamos te ver em breve pra comemorar juntos! 🎁",
            campo("presente", "Qual o presente/condição especial?", "Ex: 15% de desconto válido durante todo o mês do seu aniversário", true, false)
        ),

        template("novo_servico", "Novidade", "Lançamento de novo serviço",
            "Anunciar um novo serviço ou produto do catálogo.",
            "✨ Novidade por aqui! Agora também temos: {{novidade}}\n\n" +
            "{{detalhes}}\n\nQuer ser um dos primeiros a experimentar? Responda essa mensagem!",
            campo("novidade", "Qual é a novidade?", "Ex: um novo tipo de atendimento", true, false),
            campo("detalhes", "Mais detalhes (opcional)", "Ex: valores, duração, o que inclui", false, true)
        ),

        template("indicacao_amigo", "Indicação", "Traga um amigo",
            "Incentivar indicação de novos clientes.",
            "🙌 Você conhece alguém que ia adorar conhecer a gente?\n\n" +
            "{{beneficio}}\n\nÉ só chamar aqui que a gente te explica como funciona!",
            campo("beneficio", "Qual o benefício da indicação?", "Ex: você e seu amigo ganham desconto no próximo atendimento", true, false)
        ),

        template("agenda_ociosa_hoje", "Agenda", "Horário livre hoje/amanhã",
            "Preencher horários vagos de última hora.",
            "📅 Temos um horário livre {{quando}} e pensamos em você!\n\n" +
            "{{condicao}}\n\nTopa aproveitar? Responde aqui rapidinho!",
            campo("quando", "Quando é o horário livre?", "Ex: hoje à tarde, amanhã de manhã", true, false),
            campo("condicao", "Alguma condição especial? (opcional)", "Ex: com um desconto especial pra quem fechar hoje", false, false)
        ),

        template("pacote_combo", "Promoção", "Pacote / combo de atendimentos",
            "Divulgar um pacote fechado de serviços.",
            "📦 Criamos um pacote especial pensando em você: {{pacote}}\n\n" +
            "{{vantagem}}\n\nQuer saber mais? Responda essa mensagem!",
            campo("pacote", "O que inclui o pacote?", "Ex: 4 atendimentos com condição especial", true, false),
            campo("vantagem", "Qual a vantagem de fechar o pacote?", "Ex: economia em relação a pagar avulso", true, false)
        ),

        template("data_comemorativa", "Data especial", "Data comemorativa (Natal, Dia das Mães, etc.)",
            "Campanha sazonal ligada a uma data do calendário.",
            "🎊 {{data}} está chegando e preparamos algo especial pra você!\n\n" +
            "{{oferta}}\n\nGaranta seu horário antes que as vagas acabem!",
            campo("data", "Qual data comemorativa?", "Ex: o Dia das Mães", true, false),
            campo("oferta", "Qual é a condição especial?", "Ex: condições especiais durante toda a semana", true, false)
        ),

        template("boas_vindas_primeira_visita", "Relacionamento", "Boas-vindas pós primeira visita",
            "Agradecer a primeira visita e convidar pra voltar.",
            "😊 Foi um prazer receber você pela primeira vez aqui!\n\n" +
            "{{convite}}\n\nEsperamos te ver de novo em breve! ✨",
            campo("convite", "Convite ou condição pra próxima visita", "Ex: no seu próximo atendimento você ganha um mimo especial", false, false)
        ),

        template("fidelidade_cliente_frequente", "Relacionamento", "Agradecimento a cliente fiel",
            "Reconhecer quem já é cliente frequente (segmento VIP).",
            "🌟 Você é um dos nossos clientes mais especiais e queríamos te agradecer por isso!\n\n" +
            "{{reconhecimento}}\n\nContinue contando com a gente sempre que precisar 💛",
            campo("reconhecimento", "Como recompensar a fidelidade?", "Ex: preparamos uma cortesia especial no seu próximo atendimento", true, false)
        ),

        template("novo_profissional", "Novidade", "Chegada de novo profissional",
            "Anunciar um novo profissional na equipe.",
            "👋 Novidade na equipe! {{apresentacao}}\n\n" +
            "{{convite}}\n\nQuer conhecer? Responda essa mensagem pra agendar!",
            campo("apresentacao", "Apresentação do novo profissional", "Ex: agora contamos com mais um especialista na equipe", true, true),
            campo("convite", "Convite/condição especial (opcional)", "Ex: condição especial pra quem for um dos primeiros a agendar", false, false)
        ),

        template("pesquisa_satisfacao", "Relacionamento", "Pesquisa de satisfação",
            "Pedir feedback sobre um atendimento recente.",
            "💬 Sua opinião é muito importante pra gente!\n\n" +
            "{{pedido}}\n\nPode responder aqui mesmo, é rapidinho. Muito obrigado! 🙏",
            campo("pedido", "O que perguntar?", "Ex: conta pra gente como foi sua experiência no último atendimento", true, true)
        ),

        template("lembrete_retorno_periodico", "Relacionamento", "Lembrete de retorno periódico",
            "Lembrar o cliente de agendar um novo atendimento de rotina.",
            "📅 Já faz um tempinho desde seu último atendimento por aqui!\n\n" +
            "{{sugestao}}\n\nQue tal já garantir seu próximo horário? É só responder essa mensagem!",
            campo("sugestao", "Sugestão de retorno", "Ex: geralmente o ideal é retornar a cada 30 dias", false, false)
        ),

        template("urgencia_cupom_relampago", "Promoção", "Cupom relâmpago (urgência 24h)",
            "Criar urgência com prazo bem curto.",
            "⚡ Oferta relâmpago, só por hoje!\n\n" +
            "{{oferta}}\n\nCorre que é por tempo bem limitado. Responda essa mensagem pra garantir!",
            campo("oferta", "Qual é a oferta relâmpago?", "Ex: condição especial válida somente até meia-noite de hoje", true, false)
        ),

        template("fim_de_semana_especial", "Promoção", "Condição especial de fim de semana",
            "Divulgar horários ou condições de sábado/domingo.",
            "🌞 Fim de semana chegando e preparamos uma condição especial!\n\n" +
            "{{oferta}}\n\nGaranta seu horário! Responda essa mensagem.",
            campo("oferta", "Qual a condição do fim de semana?", "Ex: condição especial válida sábado e domingo", true, false)
        ),

        template("meio_semana_baixa_demanda", "Agenda", "Incentivo em dias de menor movimento",
            "Estimular agendamentos em dias/horários de baixa demanda.",
            "📉 Terça e quarta costumam ter mais horários livres por aqui!\n\n" +
            "{{oferta}}\n\nQue tal aproveitar? Responda essa mensagem pra agendar.",
            campo("oferta", "Qual o incentivo?", "Ex: condição especial pra quem agendar de terça a quinta", true, false)
        ),

        template("lista_espera_pre_lancamento", "Novidade", "Lista de espera / pré-venda",
            "Gerar antecipação antes de um lançamento oficial.",
            "🚀 Em breve vem novidade por aqui: {{novidade}}\n\n" +
            "{{chamada}}\n\nQuer entrar na lista de espera? Responda essa mensagem!",
            campo("novidade", "O que está por vir?", "Ex: um novo serviço que estamos preparando", true, false),
            campo("chamada", "Chamada pra ação", "Ex: quem entrar na lista de espera garante prioridade no lançamento", true, false)
        ),

        template("ultimas_vagas_promocao", "Promoção", "Encerramento de promoção (últimas vagas)",
            "Reforçar uma promoção que já está acabando.",
            "⏰ Últimos dias da nossa promoção: {{oferta}}\n\n" +
            "{{prazo}}\n\nNão perca! Responda essa mensagem pra garantir seu horário.",
            campo("oferta", "Qual era a promoção?", "Ex: condição especial em qualquer atendimento", true, false),
            campo("prazo", "Até quando vale?", "Ex: válido só até sexta-feira", true, false)
        ),

        template("reagendamento_em_massa", "Aviso", "Aviso de mudança (endereço/horário)",
            "Avisar clientes sobre uma mudança operacional relevante.",
            "📢 Um aviso importante: {{aviso}}\n\n" +
            "{{instrucao}}\n\nQualquer dúvida, é só responder essa mensagem!",
            campo("aviso", "Qual é o aviso?", "Ex: mudamos de endereço / mudamos nosso horário de funcionamento", true, true),
            campo("instrucao", "O que o cliente precisa fazer?", "Ex: se você tem um horário marcado, vamos entrar em contato pra confirmar", false, true)
        ),

        template("agradecimento_pos_atendimento", "Relacionamento", "Agradecimento após atendimento",
            "Mensagem de agradecimento com convite de retorno.",
            "🙏 Muito obrigado por ter vindo até a gente!\n\n" +
            "{{convite}}\n\nEsperamos te ver novamente em breve!",
            campo("convite", "Convite pra próxima visita (opcional)", "Ex: já pode garantir seu próximo horário quando quiser", false, false)
        ),

        template("sazonal_verao_inverno", "Data especial", "Campanha sazonal (verão/inverno)",
            "Campanha ligada a uma estação do ano.",
            "☀️ Chegou a época perfeita pra cuidar de você!\n\n" +
            "{{oferta}}\n\nGaranta seu horário! Responda essa mensagem.",
            campo("oferta", "Qual a oferta da estação?", "Ex: condição especial preparada pra essa época do ano", true, false)
        ),

        template("convite_evento_workshop", "Novidade", "Convite para evento/workshop",
            "Divulgar um evento, workshop ou dia especial.",
            "🎤 Vem novidade por aqui: {{evento}}\n\n" +
            "{{detalhes}}\n\nQuer participar? Responda essa mensagem que a gente te conta mais!",
            campo("evento", "Qual é o evento?", "Ex: um workshop especial que vamos realizar", true, false),
            campo("detalhes", "Detalhes (data, local, vagas)", "Ex: dia, horário e como garantir vaga", false, true)
        ),

        template("reconquista_carrinho_abandonado", "Reativação", "Retomar agendamento não concluído",
            "Pra quem começou a agendar mas não finalizou.",
            "👋 Vimos que você chegou a dar uma olhada nos nossos horários!\n\n" +
            "{{convite}}\n\nPosso te ajudar a finalizar o agendamento? É só responder aqui.",
            campo("convite", "Convite/incentivo pra finalizar", "Ex: os horários ainda estão disponíveis, e se fechar hoje tem condição especial", false, false)
        )
    );

    private static CampanhaTemplateDTO template(String id, String categoria, String titulo,
                                                String descricaoUso, String mensagemBase,
                                                CampoTemplateCampanhaDTO... campos) {
        return new CampanhaTemplateDTO(id, categoria, titulo, descricaoUso, mensagemBase, List.of(campos));
    }

    private static CampoTemplateCampanhaDTO campo(String chave, String label, String placeholder,
                                                   boolean obrigatorio, boolean multilinha) {
        return new CampoTemplateCampanhaDTO(chave, label, placeholder, obrigatorio, multilinha);
    }

    public List<CampanhaTemplateDTO> listar() {
        return TEMPLATES;
    }
}
