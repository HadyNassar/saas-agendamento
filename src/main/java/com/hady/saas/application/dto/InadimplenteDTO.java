package com.hady.saas.application.dto;

import java.time.LocalDate;

// Um estabelecimento inadimplente — plano vencido e ainda não renovado.
public class InadimplenteDTO {

    private Long estabelecimentoId;
    private String nomeEstabelecimento;
    private String plano;
    private LocalDate planoValidoAte;
    private long diasEmAtraso;
    private String telefone;
    private String whatsappContato;

    public InadimplenteDTO() {}

    public InadimplenteDTO(Long estabelecimentoId, String nomeEstabelecimento, String plano,
                           LocalDate planoValidoAte, long diasEmAtraso,
                           String telefone, String whatsappContato) {
        this.estabelecimentoId = estabelecimentoId;
        this.nomeEstabelecimento = nomeEstabelecimento;
        this.plano = plano;
        this.planoValidoAte = planoValidoAte;
        this.diasEmAtraso = diasEmAtraso;
        this.telefone = telefone;
        this.whatsappContato = whatsappContato;
    }

    public Long getEstabelecimentoId()                { return estabelecimentoId; }
    public void setEstabelecimentoId(Long v)          { this.estabelecimentoId = v; }

    public String getNomeEstabelecimento()                { return nomeEstabelecimento; }
    public void setNomeEstabelecimento(String v)          { this.nomeEstabelecimento = v; }

    public String getPlano()                          { return plano; }
    public void setPlano(String plano)                { this.plano = plano; }

    public LocalDate getPlanoValidoAte()              { return planoValidoAte; }
    public void setPlanoValidoAte(LocalDate v)        { this.planoValidoAte = v; }

    public long getDiasEmAtraso()                      { return diasEmAtraso; }
    public void setDiasEmAtraso(long diasEmAtraso)      { this.diasEmAtraso = diasEmAtraso; }

    public String getTelefone()                        { return telefone; }
    public void setTelefone(String telefone)            { this.telefone = telefone; }

    public String getWhatsappContato()                        { return whatsappContato; }
    public void setWhatsappContato(String whatsappContato)    { this.whatsappContato = whatsappContato; }
}