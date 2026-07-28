package com.hady.saas.domain.cupom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cupom de desconto criado pelo DONO DO ESTABELECIMENTO para os próprios
 * clientes (seção 5-B). Entidade separada de {@link Cupom} de propósito —
 * aquela é do admin-master da plataforma (cupom de assinatura/plano) e
 * confirmadamente não tem estabelecimento_id; misturar os dois conceitos
 * numa entidade só ia confundir "desconto na mensalidade do salão" com
 * "desconto que o salão dá pro cliente dele".
 *
 * Regras de validade centralizadas aqui (mesmo padrão de Cupom.estaValido()),
 * evitando duplicar a lógica no service.
 */
@Entity
@Table(name = "cupom_estabelecimento")
public class CupomEstabelecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(name = "tipo_desconto", nullable = false, length = 20)
    private String tipoDesconto; // PERCENTUAL | VALOR_FIXO

    @Column(name = "valor_desconto", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorDesconto;

    // NULL = sem expiração — decisão do dono do salão por cupom.
    @Column(name = "validade_data")
    private LocalDate validadeData;

    // NULL = sem limite de usos — decisão do dono do salão por cupom.
    @Column(name = "limite_usos")
    private Integer limiteUsos;

    @Column(name = "usos_atuais", nullable = false)
    private int usosAtuais = 0;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public CupomEstabelecimento() {}

    public boolean estaValido() {
        if (!ativo) return false;
        if (validadeData != null && LocalDate.now().isAfter(validadeData)) return false;
        if (limiteUsos != null && usosAtuais >= limiteUsos) return false;
        return true;
    }

    public BigDecimal calcularValorComDesconto(BigDecimal valorOriginal) {
        if (valorOriginal == null) return null;
        BigDecimal resultado;
        if ("PERCENTUAL".equals(tipoDesconto)) {
            BigDecimal desconto = valorOriginal.multiply(valorDesconto).divide(BigDecimal.valueOf(100));
            resultado = valorOriginal.subtract(desconto);
        } else {
            resultado = valorOriginal.subtract(valorDesconto);
        }
        return resultado.max(BigDecimal.ZERO);
    }

    public BigDecimal calcularValorDesconto(BigDecimal valorOriginal) {
        return valorOriginal.subtract(calcularValorComDesconto(valorOriginal));
    }

    public Long getId()                          { return id; }
    public Estabelecimento getEstabelecimento()  { return estabelecimento; }
    public String getCodigo()                    { return codigo; }
    public String getTipoDesconto()              { return tipoDesconto; }
    public BigDecimal getValorDesconto()         { return valorDesconto; }
    public LocalDate getValidadeData()           { return validadeData; }
    public Integer getLimiteUsos()               { return limiteUsos; }
    public int getUsosAtuais()                    { return usosAtuais; }
    public boolean isAtivo()                      { return ativo; }
    public LocalDateTime getCriadoEm()            { return criadoEm; }

    public void setId(Long id)                                     { this.id = id; }
    public void setEstabelecimento(Estabelecimento estabelecimento){ this.estabelecimento = estabelecimento; }
    public void setCodigo(String codigo)                           { this.codigo = codigo; }
    public void setTipoDesconto(String tipoDesconto)               { this.tipoDesconto = tipoDesconto; }
    public void setValorDesconto(BigDecimal valorDesconto)         { this.valorDesconto = valorDesconto; }
    public void setValidadeData(LocalDate validadeData)            { this.validadeData = validadeData; }
    public void setLimiteUsos(Integer limiteUsos)                  { this.limiteUsos = limiteUsos; }
    public void setUsosAtuais(int usosAtuais)                      { this.usosAtuais = usosAtuais; }
    public void setAtivo(boolean ativo)                            { this.ativo = ativo; }
    public void setCriadoEm(LocalDateTime criadoEm)                { this.criadoEm = criadoEm; }
}