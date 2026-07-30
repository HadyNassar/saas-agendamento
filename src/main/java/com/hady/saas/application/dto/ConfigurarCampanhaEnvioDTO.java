package com.hady.saas.application.dto;

import jakarta.validation.constraints.Min;

/**
 * Configuração da campanha manual (seção 5-B), separada da edição geral do
 * perfil do estabelecimento (EditarEstabelecimentoDTO) por ser uma
 * responsabilidade diferente e editada em outra tela do painel.
 */
public class ConfigurarCampanhaEnvioDTO {

    // Template universal opcional. Aceita {{mensagem}} (obrigatório pra
    // encaixar o texto livre no lugar certo — se ausente, o texto livre é
    // anexado ao final), {{nome_cliente}} e {{nome_estabelecimento}}.
    // Enviar null ou string em branco remove o template (volta a mandar só
    // o texto livre puro).
    private String templateMensagemCampanha;

    @Min(value = 1, message = "O intervalo mínimo entre campanhas deve ser de pelo menos 1 dia")
    private Integer intervaloMinimoCampanhaDias;

    public ConfigurarCampanhaEnvioDTO() {}

    public String getTemplateMensagemCampanha()            { return templateMensagemCampanha; }
    public void setTemplateMensagemCampanha(String v)      { this.templateMensagemCampanha = v; }

    public Integer getIntervaloMinimoCampanhaDias()        { return intervaloMinimoCampanhaDias; }
    public void setIntervaloMinimoCampanhaDias(Integer v)  { this.intervaloMinimoCampanhaDias = v; }
}
