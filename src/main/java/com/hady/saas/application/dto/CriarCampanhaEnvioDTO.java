package com.hady.saas.application.dto;

import com.hady.saas.domain.enums.EstagioCliente;

import java.util.List;

/**
 * Payload de criação de uma campanha manual (seção 5-B).
 *
 * filtroEstagio aceita NOVO, RECORRENTE, VIP, INATIVO (segmentos calculados
 * por ClienteSegmentacaoService) ou TODOS (pseudo-segmento que ignora
 * estagioAtual e pega todo mundo cadastrado no estabelecimento, exceto
 * bloqueados). Um filtro mais rico (ticket médio, serviço mais usado) fica
 * para uma versão futura, sem impacto neste DTO.
 *
 * O campo mensagem é sempre o TEXTO LIVRE digitado pelo dono (seja escrito
 * do zero, seja montado no front a partir de um CampanhaTemplateDTO com os
 * {{blanks}} já substituídos). Se o estabelecimento tiver um
 * templateMensagemCampanha configurado, o service interpola esse texto
 * livre dentro do template automaticamente — o dono não precisa (e não
 * deve) colar o template aqui manualmente.
 *
 * profissionalIds: null ou lista vazia = campanha vale pra clientes de
 * TODOS os profissionais (comportamento padrão, igual ao que já existia).
 * Se vier preenchida, a campanha só alcança clientes que já tiveram algum
 * agendamento com pelo menos um dos profissionais informados.
 *
 * cupom: opcional. Quando informado, um único CupomEstabelecimento é criado
 * junto com o rascunho da campanha e o link já com o código é anexado à
 * mensagem de cada destinatário.
 */
public class CriarCampanhaEnvioDTO {

    private EstagioCliente filtroEstagio;
    private String mensagem;
    private List<Long> profissionalIds;
    private CupomCampanhaDTO cupom;
    private Long cupomEstabelecimentoId;

    public CriarCampanhaEnvioDTO() {}

    public EstagioCliente getFiltroEstagio()   { return filtroEstagio; }
    public String getMensagem()                { return mensagem; }
    public List<Long> getProfissionalIds()     { return profissionalIds; }
    public CupomCampanhaDTO getCupom()         { return cupom; }
    public Long getCupomEstabelecimentoId()    { return cupomEstabelecimentoId; }

    public void setFiltroEstagio(EstagioCliente filtroEstagio) { this.filtroEstagio = filtroEstagio; }
    public void setMensagem(String mensagem)                   { this.mensagem = mensagem; }
    public void setProfissionalIds(List<Long> profissionalIds) { this.profissionalIds = profissionalIds; }
    public void setCupom(CupomCampanhaDTO cupom)                { this.cupom = cupom; }
    public void setCupomEstabelecimentoId(Long cupomEstabelecimentoId) { this.cupomEstabelecimentoId = cupomEstabelecimentoId; }
}