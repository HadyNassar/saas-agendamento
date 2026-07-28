package com.hady.saas.application.dto;

import java.math.BigDecimal;

// Resposta do endpoint público de validação do cupom do ESTABELECIMENTO
// (seção 5-B) — usado na tela de agendamento público quando o link já vem
// com ?cupom=CODIGO, pra exibir o valor com desconto antes de confirmar.
// Não confundir com ValidarCupomResponseDTO, que é do cupom de assinatura
// da plataforma (admin-master).
public class ValidarCupomEstabelecimentoResponseDTO {

    private boolean valido;
    private String mensagem;
    private String codigo;
    private String tipoDesconto;
    private BigDecimal valorDesconto;
    private BigDecimal valorOriginal;
    private BigDecimal valorComDesconto;

    public ValidarCupomEstabelecimentoResponseDTO() {}

    public static ValidarCupomEstabelecimentoResponseDTO invalido(String mensagem) {
        ValidarCupomEstabelecimentoResponseDTO dto = new ValidarCupomEstabelecimentoResponseDTO();
        dto.valido = false;
        dto.mensagem = mensagem;
        return dto;
    }

    public static ValidarCupomEstabelecimentoResponseDTO valido(String codigo, String tipoDesconto,
                                                                BigDecimal valorDesconto,
                                                                BigDecimal valorOriginal,
                                                                BigDecimal valorComDesconto) {
        ValidarCupomEstabelecimentoResponseDTO dto = new ValidarCupomEstabelecimentoResponseDTO();
        dto.valido = true;
        dto.mensagem = "Cupom válido!";
        dto.codigo = codigo;
        dto.tipoDesconto = tipoDesconto;
        dto.valorDesconto = valorDesconto;
        dto.valorOriginal = valorOriginal;
        dto.valorComDesconto = valorComDesconto;
        return dto;
    }

    public boolean isValido()                        { return valido; }
    public String getMensagem()                       { return mensagem; }
    public String getCodigo()                         { return codigo; }
    public String getTipoDesconto()                   { return tipoDesconto; }
    public BigDecimal getValorDesconto()              { return valorDesconto; }
    public BigDecimal getValorOriginal()              { return valorOriginal; }
    public BigDecimal getValorComDesconto()           { return valorComDesconto; }

    public void setValido(boolean valido)                          { this.valido = valido; }
    public void setMensagem(String mensagem)                       { this.mensagem = mensagem; }
    public void setCodigo(String codigo)                           { this.codigo = codigo; }
    public void setTipoDesconto(String tipoDesconto)               { this.tipoDesconto = tipoDesconto; }
    public void setValorDesconto(BigDecimal valorDesconto)         { this.valorDesconto = valorDesconto; }
    public void setValorOriginal(BigDecimal valorOriginal)         { this.valorOriginal = valorOriginal; }
    public void setValorComDesconto(BigDecimal valorComDesconto)   { this.valorComDesconto = valorComDesconto; }
}