package com.hady.saas.application.dto;

/**
 * Descreve UM campo de texto livre que o front deve renderizar para o dono
 * preencher dentro de um {@link CampanhaTemplateDTO}. O back é a fonte da
 * verdade de "quais blanks existem e como validá-los" — o front só
 * desenha o formulário a partir disto, sem precisar hardcodar nada por
 * template.
 *
 * chave: identifica o token {{chave}} dentro de mensagemBase que este campo
 *        preenche.
 */
public class CampoTemplateCampanhaDTO {

    private final String chave;
    private final String label;
    private final String placeholder;
    private final boolean obrigatorio;
    private final boolean multilinha;

    public CampoTemplateCampanhaDTO(String chave, String label, String placeholder,
                                    boolean obrigatorio, boolean multilinha) {
        this.chave = chave;
        this.label = label;
        this.placeholder = placeholder;
        this.obrigatorio = obrigatorio;
        this.multilinha = multilinha;
    }

    public String getChave()        { return chave; }
    public String getLabel()        { return label; }
    public String getPlaceholder()  { return placeholder; }
    public boolean isObrigatorio()  { return obrigatorio; }
    public boolean isMultilinha()   { return multilinha; }
}
