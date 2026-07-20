package com.hady.saas.application.dto;

import java.math.BigDecimal;

// Resposta do endpoint público de validação de cupom, usado no fluxo de
// assinatura antes de confirmar o pagamento.
public class ValidarCupomResponseDTO {

    private boolean valido;
    private String mensagem;
    private String tipoDesconto;
    private BigDecimal valorDesconto;

    public ValidarCupomResponseDTO() {}

    public static ValidarCupomResponseDTO invalido(String mensagem) {
        ValidarCupomResponseDTO dto = new ValidarCupomResponseDTO();
        dto.setValido(false);
        dto.setMensagem(mensagem);
        return dto;
    }

    public static ValidarCupomResponseDTO valido(String tipoDesconto, BigDecimal valorDesconto) {
        ValidarCupomResponseDTO dto = new ValidarCupomResponseDTO();
        dto.setValido(true);
        dto.setMensagem("Cupom válido!");
        dto.setTipoDesconto(tipoDesconto);
        dto.setValorDesconto(valorDesconto);
        return dto;
    }

    public boolean isValido()                          { return valido; }
    public void setValido(boolean valido)               { this.valido = valido; }

    public String getMensagem()                         { return mensagem; }
    public void setMensagem(String mensagem)            { this.mensagem = mensagem; }

    public String getTipoDesconto()                     { return tipoDesconto; }
    public void setTipoDesconto(String v)               { this.tipoDesconto = v; }

    public BigDecimal getValorDesconto()                 { return valorDesconto; }
    public void setValorDesconto(BigDecimal v)           { this.valorDesconto = v; }
}