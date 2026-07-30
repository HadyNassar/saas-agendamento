package com.hady.saas.domain.whatsapp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hady.saas.domain.cupom.CupomEstabelecimento;
import com.hady.saas.domain.enums.EstagioCliente;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.profissional.Profissional;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cabeçalho de uma campanha de disparo em massa (itens 9/10 da camada
 * PLATINUM, e a parte de "campanha" da seção 5-B, sem a parte de cupom).
 *
 * Uma CampanhaEnvio nunca envia nada diretamente — ela só descreve a intenção
 * (filtro + mensagem) e guarda um resumo de progresso. O envio de verdade
 * acontece item a item em CampanhaEnvioItem, processado de forma assíncrona
 * por CampanhaEnvioProcessorJob (fila via banco + @Scheduled, sem
 * RabbitMQ/Kafka), pelos motivos já discutidos: qualquer volume real de
 * clientes precisa respeitar rate limit da Evolution API e não pode travar
 * a requisição HTTP que criou a campanha.
 *
 * IMPORTANTE — ainda não tem consumidor real: nenhum controller cria
 * CampanhaEnvio ainda (isso é a próxima etapa, seja via disparo manual do
 * dono do salão — seção 5-B — ou automático por regra — itens 9/10). Esta
 * classe só estabelece a fundação de fila/processamento.
 */
@Entity
@Table(name = "campanha_envio")
public class CampanhaEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    // Escolha de design: filtro simples por EstagioCliente nesta primeira
    // versão (cobre "todos os VIP", "todos os INATIVO" etc.), que já é
    // suficiente para os itens 9/10. Um filtro mais rico (ticket médio,
    // serviço mais usado, citado na seção 5-B) pode virar uma coluna JSON
    // separada depois, sem quebrar esta — não implementado ainda porque não
    // foi pedido nesta etapa.
    @Enumerated(EnumType.STRING)
    @Column(name = "filtro_estagio")
    private EstagioCliente filtroEstagio;

    // Texto livre digitado pelo dono (SEM o template aplicado). É o que fica
    // salvo aqui — o texto final por destinatário (com template +
    // interpolação + variação) vive em CampanhaEnvioItem.mensagemFinal.
    // Mantido o nome "mensagem" por compatibilidade com o que já existia.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    // Snapshot informativo: se true, o templateMensagemCampanha do
    // estabelecimento estava preenchido no momento da criação e foi aplicado
    // a todos os itens desta campanha. Puramente para exibição no histórico
    // (o dono pode mudar/remover o template depois sem afetar campanhas já
    // criadas).
    @Column(name = "usou_template", nullable = false)
    private boolean usouTemplate = false;

    // MANUAL (dono disparou pelo painel) ou AUTOMATICA (regra do sistema,
    // itens 9/10). Nenhuma das duas origens está implementada ainda — esta
    // coluna só existe para não precisar de migration nova quando a primeira
    // for construída.
    @Column(nullable = false, length = 20)
    private String origem = "MANUAL";

    // CRIADA -> EM_ANDAMENTO -> CONCLUIDA (ou CONCLUIDA_COM_ERROS se algum
    // item falhou). O job de processamento é quem transiciona estes estados.
    @Column(nullable = false, length = 20)
    private String status = "CRIADA";

    @Column(name = "total_destinatarios")
    private int totalDestinatarios = 0;

    @Column(name = "total_enviados")
    private int totalEnviados = 0;

    @Column(name = "total_erros")
    private int totalErros = 0;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    // Vinculação de profissionais (pedido do dono do produto): a campanha
    // pode mirar 1, vários ou TODOS os profissionais do estabelecimento.
    // true (padrão) = todos, ignora a lista abaixo por completo, inclusive
    // profissionais cadastrados DEPOIS da campanha (comportamento "todos"
    // sempre dinâmico). false = restringe aos profissionais da lista.
    @Column(name = "todos_profissionais", nullable = false)
    private boolean todosProfissionais = true;

    // Só relevante (e só preenchida) quando todosProfissionais = false.
    // Guardada apenas para exibição no histórico da campanha — o filtro de
    // destinatários já foi aplicado no momento da criação do rascunho
    // (CampanhaEnvioService), não é recalculado a partir desta lista depois.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "campanha_envio_profissional",
            joinColumns = @JoinColumn(name = "campanha_envio_id"),
            inverseJoinColumns = @JoinColumn(name = "profissional_id")
    )
    private List<Profissional> profissionais = new ArrayList<>();

    // Cupom gerado junto com a campanha (opcional). Um único cupom
    // compartilhado por todos os destinatários desta campanha — o link já
    // com o código vai anexado à mensagem de cada item (ver
    // CampanhaEnvioService.montarMensagemFinal). Null = campanha sem cupom.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cupom_estabelecimento_id")
    private CupomEstabelecimento cupomEstabelecimento;

    public CampanhaEnvio() {}

    public Long getId()                          { return id; }
    public Estabelecimento getEstabelecimento()  { return estabelecimento; }
    public EstagioCliente getFiltroEstagio()     { return filtroEstagio; }
    public String getMensagem()                  { return mensagem; }
    public boolean isUsouTemplate()              { return usouTemplate; }
    public String getOrigem()                    { return origem; }
    public String getStatus()                    { return status; }
    public int getTotalDestinatarios()           { return totalDestinatarios; }
    public int getTotalEnviados()                { return totalEnviados; }
    public int getTotalErros()                   { return totalErros; }
    public LocalDateTime getCriadoEm()            { return criadoEm; }
    public LocalDateTime getConcluidoEm()         { return concluidoEm; }
    public boolean isTodosProfissionais()         { return todosProfissionais; }
    public List<Profissional> getProfissionais()  { return profissionais; }
    public CupomEstabelecimento getCupomEstabelecimento() { return cupomEstabelecimento; }

    public void setId(Long id)                                     { this.id = id; }
    public void setEstabelecimento(Estabelecimento estabelecimento){ this.estabelecimento = estabelecimento; }
    public void setFiltroEstagio(EstagioCliente filtroEstagio)     { this.filtroEstagio = filtroEstagio; }
    public void setMensagem(String mensagem)                       { this.mensagem = mensagem; }
    public void setUsouTemplate(boolean usouTemplate)               { this.usouTemplate = usouTemplate; }
    public void setOrigem(String origem)                           { this.origem = origem; }
    public void setStatus(String status)                           { this.status = status; }
    public void setTotalDestinatarios(int totalDestinatarios)      { this.totalDestinatarios = totalDestinatarios; }
    public void setTotalEnviados(int totalEnviados)                { this.totalEnviados = totalEnviados; }
    public void setTotalErros(int totalErros)                      { this.totalErros = totalErros; }
    public void setCriadoEm(LocalDateTime criadoEm)                { this.criadoEm = criadoEm; }
    public void setConcluidoEm(LocalDateTime concluidoEm)          { this.concluidoEm = concluidoEm; }
    public void setTodosProfissionais(boolean todosProfissionais)  { this.todosProfissionais = todosProfissionais; }
    public void setProfissionais(List<Profissional> profissionais) { this.profissionais = profissionais; }
    public void setCupomEstabelecimento(CupomEstabelecimento cupomEstabelecimento) { this.cupomEstabelecimento = cupomEstabelecimento; }
}