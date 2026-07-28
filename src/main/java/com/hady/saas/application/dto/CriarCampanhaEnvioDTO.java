package com.hady.saas.application.dto;

import com.hady.saas.domain.enums.EstagioCliente;

/**
 * Payload de criação de uma campanha manual (seção 5-B).
 *
 * Nesta primeira versão o único filtro suportado é EstagioCliente (NOVO,
 * RECORRENTE, VIP, INATIVO) — cobre o caso de uso descrito no documento
 * ("todos os VIP", "todos os INATIVO"). Um filtro mais rico (ticket médio,
 * serviço mais usado) fica para uma versão futura, sem impacto neste DTO.
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