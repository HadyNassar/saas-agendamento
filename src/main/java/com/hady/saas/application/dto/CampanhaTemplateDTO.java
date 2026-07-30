package com.hady.saas.application.dto;

import java.util.List;

/**
 * Uma campanha predefinida (catálogo servido por CampanhaTemplateCatalogo).
 *
 * mensagemBase é o texto fixo do template, com tokens {{chave}} nos pontos
 * onde o dono preenche texto livre — cada token corresponde a um item de
 * `campos`, na mesma ordem em que aparecem no texto. Genérico o bastante
 * para qualquer tipo de estabelecimento (salão, barbearia, clínica,
 * personal trainer, psicólogo etc.): nunca menciona um segmento específico
 * como "cabelo" ou "corte", só "atendimento"/"horário"/nome do
 * estabelecimento.
 *
 * O front:
 *   1) busca GET /painel/whatsapp/campanhas/templates
 *   2) mostra a lista (agrupada por categoria, se quiser)
 *   3) ao escolher um, renderiza um campo de texto por item de `campos`
 *   4) substitui cada {{chave}} pelo texto digitado e monta a mensagem final
 *   5) envia essa mensagem final no campo `mensagem` de POST /campanhas,
 *      exatamente como já funcionava para o texto 100% livre
 *
 * Note que a saudação ("Olá, Fulano!") e o nome do cliente NÃO fazem parte
 * do template — isso já é adicionado automaticamente pelo backend
 * (CampanhaEnvioService.montarMensagemFinal), então nenhum template aqui
 * repete isso.
 */
public class CampanhaTemplateDTO {

    private final String id;
    private final String categoria;
    private final String titulo;
    private final String descricaoUso;
    private final String mensagemBase;
    private final List<CampoTemplateCampanhaDTO> campos;

    public CampanhaTemplateDTO(String id, String categoria, String titulo, String descricaoUso,
                               String mensagemBase, List<CampoTemplateCampanhaDTO> campos) {
        this.id = id;
        this.categoria = categoria;
        this.titulo = titulo;
        this.descricaoUso = descricaoUso;
        this.mensagemBase = mensagemBase;
        this.campos = campos;
    }

    public String getId()                                 { return id; }
    public String getCategoria()                          { return categoria; }
    public String getTitulo()                             { return titulo; }
    public String getDescricaoUso()                       { return descricaoUso; }
    public String getMensagemBase()                       { return mensagemBase; }
    public List<CampoTemplateCampanhaDTO> getCampos()      { return campos; }
}
