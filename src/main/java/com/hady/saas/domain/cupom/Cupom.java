package com.hady.saas.domain.cupom;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cupons")
public class Cupom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(name = "tipo_desconto", nullable = false)
    private String tipoDesconto; // PERCENTUAL | VALOR_FIXO

    @Column(name = "valor_desconto", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorDesconto;

    @Column(name = "plano_restrito")
    private String planoRestrito; // null = vale pra qualquer plano

    @Column(name = "validade_data")
    private LocalDate validadeData; // null = sem expiração

    @Column(name = "limite_usos")
    private Integer limiteUsos; // null = ilimitado

    @Column(name = "usos_atuais", nullable = false)
    private int usosAtuais = 0;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    public Cupom() {}

    // Regras de validade centralizadas na entidade — evita duplicar essa
    // lógica em múltiplos lugares do service.
    public boolean estaValido() {
        if (!ativo) return false;
        if (validadeData != null && LocalDate.now().isAfter(validadeData)) return false;
        if (limiteUsos != null && usosAtuais >= limiteUsos) return false;
        return true;
    }

    public boolean valeParaPlano(String plano) {
        return planoRestrito == null || planoRestrito.isBlank() || planoRestrito.equalsIgnoreCase(plano);
    }

    public BigDecimal calcularValorComDesconto(BigDecimal valorOriginal) {
        if ("PERCENTUAL".equals(tipoDesconto)) {
            BigDecimal desconto = valorOriginal.multiply(valorDesconto).divide(BigDecimal.valueOf(100));
            return valorOriginal.subtract(desconto).max(BigDecimal.ZERO);
        }
        // VALOR_FIXO
        return valorOriginal.subtract(valorDesconto).max(BigDecimal.ZERO);
    }

    public Long getId()                                { return id; }
    public String getCodigo()                          { return codigo; }
    public String getTipoDesconto()                    { return tipoDesconto; }
    public BigDecimal getValorDesconto()               { return valorDesconto; }
    public String getPlanoRestrito()                   { return planoRestrito; }
    public LocalDate getValidadeData()                 { return validadeData; }
    public Integer getLimiteUsos()                     { return limiteUsos; }
    public int getUsosAtuais()                          { return usosAtuais; }
    public boolean isAtivo()                            { return ativo; }
    public LocalDateTime getCriadoEm()                  { return criadoEm; }

    public void setId(Long id)                                     { this.id = id; }
    public void setCodigo(String codigo)                           { this.codigo = codigo; }
    public void setTipoDesconto(String tipoDesconto)               { this.tipoDesconto = tipoDesconto; }
    public void setValorDesconto(BigDecimal valorDesconto)         { this.valorDesconto = valorDesconto; }
    public void setPlanoRestrito(String planoRestrito)             { this.planoRestrito = planoRestrito; }
    public void setValidadeData(LocalDate validadeData)            { this.validadeData = validadeData; }
    public void setLimiteUsos(Integer limiteUsos)                  { this.limiteUsos = limiteUsos; }
    public void setUsosAtuais(int usosAtuais)                      { this.usosAtuais = usosAtuais; }
    public void setAtivo(boolean ativo)                            { this.ativo = ativo; }
    public void setCriadoEm(LocalDateTime criadoEm)                { this.criadoEm = criadoEm; }
}