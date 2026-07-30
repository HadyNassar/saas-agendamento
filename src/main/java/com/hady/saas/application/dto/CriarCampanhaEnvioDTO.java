package com.hady.saas.application.dto;

import com.hady.saas.domain.enums.EstagioCliente;

/**
 * Payload de criação de uma campanha manual (seção 5-B).
 *
 * filtroEstagio aceita NOVO, RECORRENTE, VIP, INATIVO (segmentos calculados
 * por ClienteSegmentacaoService) ou TODOS (pseudo-segmento que ignora
 * estagioAtual e pega todo mundo cadastrado no estabelecimento, exceto
 * bloqueados). Um filtro mais rico (ticket médio, serviço mais usado) fica
 * para uma versão futura, sem impacto neste DTO.
 *
 * O campo mensagem é sempre o TEXTO LIVRE digitado pelo dono. Se o
 * estabelecimento tiver um templateMensagemCampanha configurado, o service
 * interpola esse texto livre dentro do template automaticamente — o dono
 * não precisa (e não deve) colar o template aqui manualmente.
 */
public class CriarCampanhaEnvioDTO {

    private EstagioCliente filtroEstagio;
    private String mensagem;

    public CriarCampanhaEnvioDTO() {}

    public EstagioCliente getFiltroEstagio() { return filtroEstagio; }
    public String getMensagem()              { return mensagem; }

    public void setFiltroEstagio(EstagioCliente filtroEstagio) { this.filtroEstagio = filtroEstagio; }
    public void setMensagem(String mensagem)                   { this.mensagem = mensagem; }
}