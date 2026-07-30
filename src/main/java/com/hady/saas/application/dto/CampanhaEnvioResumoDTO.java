package com.hady.saas.application.dto;

import com.hady.saas.domain.cupom.CupomEstabelecimento;
import com.hady.saas.domain.enums.EstagioCliente;
import com.hady.saas.domain.profissional.Profissional;
import com.hady.saas.domain.whatsapp.CampanhaEnvio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resumo de uma CampanhaEnvio para exibição no painel (criar, listar,
 * buscar, confirmar, cancelar — CampanhaEnvioController).
 *
 * Espelha os campos de leitura de CampanhaEnvio; não expõe a entidade JPA
 * diretamente para evitar acoplar a API ao modelo de persistência.
 */
public class CampanhaEnvioResumoDTO {

    private Long id;
    private EstagioCliente filtroEstagio;
    private String mensagem;
    private boolean usouTemplate;
    private String origem;
    private String status;
    private int totalDestinatarios;
    private int totalEnviados;
    private int totalErros;
    private LocalDateTime criadoEm;
    private LocalDateTime concluidoEm;
    private boolean todosProfissionais;
    private List<ProfissionalResumoDTO> profissionais;
    private CupomResumoDTO cupom;

    public CampanhaEnvioResumoDTO() {}

    public CampanhaEnvioResumoDTO(CampanhaEnvio campanha) {
        this.id = campanha.getId();
        this.filtroEstagio = campanha.getFiltroEstagio();
        this.mensagem = campanha.getMensagem();
        this.usouTemplate = campanha.isUsouTemplate();
        this.origem = campanha.getOrigem();
        this.status = campanha.getStatus();
        this.totalDestinatarios = campanha.getTotalDestinatarios();
        this.totalEnviados = campanha.getTotalEnviados();
        this.totalErros = campanha.getTotalErros();
        this.criadoEm = campanha.getCriadoEm();
        this.concluidoEm = campanha.getConcluidoEm();
        this.todosProfissionais = campanha.isTodosProfissionais();
        this.profissionais = campanha.isTodosProfissionais()
                ? List.of()
                : campanha.getProfissionais().stream().map(ProfissionalResumoDTO::new).toList();
        this.cupom = campanha.getCupomEstabelecimento() != null
                ? new CupomResumoDTO(campanha.getCupomEstabelecimento())
                : null;
    }

    public Long getId()                         { return id; }
    public EstagioCliente getFiltroEstagio()     { return filtroEstagio; }
    public String getMensagem()                  { return mensagem; }
    public boolean isUsouTemplate()               { return usouTemplate; }
    public String getOrigem()                    { return origem; }
    public String getStatus()                    { return status; }
    public int getTotalDestinatarios()           { return totalDestinatarios; }
    public int getTotalEnviados()                { return totalEnviados; }
    public int getTotalErros()                   { return totalErros; }
    public LocalDateTime getCriadoEm()           { return criadoEm; }
    public LocalDateTime getConcluidoEm()        { return concluidoEm; }
    public boolean isTodosProfissionais()        { return todosProfissionais; }
    public List<ProfissionalResumoDTO> getProfissionais() { return profissionais; }
    public CupomResumoDTO getCupom()             { return cupom; }

    public void setId(Long id)                                     { this.id = id; }
    public void setFiltroEstagio(EstagioCliente filtroEstagio)     { this.filtroEstagio = filtroEstagio; }
    public void setMensagem(String mensagem)                       { this.mensagem = mensagem; }
    public void setUsouTemplate(boolean usouTemplate)               { this.usouTemplate = usouTemplate; }
    public void setOrigem(String origem)                           { this.origem = origem; }
    public void setStatus(String status)                           { this.status = status; }
    public void setTotalDestinatarios(int totalDestinatarios)      { this.totalDestinatarios = totalDestinatarios; }
    public void setTotalEnviados(int totalEnviados)                { this.totalEnviados = totalEnviados; }
    public void setTotalErros(int totalErros)                      { this.totalErros = totalErros; }
    public void setCriadoEm(LocalDateTime criadoEm)                { this.criadoEm = criadoEm; }
    public void setConcluidoEm(LocalDateTime concluidoEm)          { this.concluidoEm = concluidoEm; }
    public void setTodosProfissionais(boolean todosProfissionais)  { this.todosProfissionais = todosProfissionais; }
    public void setProfissionais(List<ProfissionalResumoDTO> profissionais) { this.profissionais = profissionais; }
    public void setCupom(CupomResumoDTO cupom)                     { this.cupom = cupom; }

    // ── Sub-DTOs enxutos, só com o que o painel de campanhas precisa ───────

    public static class ProfissionalResumoDTO {
        private Long id;
        private String nome;

        public ProfissionalResumoDTO() {}

        public ProfissionalResumoDTO(Profissional profissional) {
            this.id = profissional.getId();
            this.nome = profissional.getNome();
        }

        public Long getId()          { return id; }
        public String getNome()      { return nome; }
        public void setId(Long id)   { this.id = id; }
        public void setNome(String nome) { this.nome = nome; }
    }

    public static class CupomResumoDTO {
        private Long id;
        private String codigo;
        private String tipoDesconto;
        private BigDecimal valorDesconto;
        private LocalDate validadeData;
        private Integer limiteUsos;
        private int usosAtuais;

        public CupomResumoDTO() {}

        public CupomResumoDTO(CupomEstabelecimento cupom) {
            this.id = cupom.getId();
            this.codigo = cupom.getCodigo();
            this.tipoDesconto = cupom.getTipoDesconto();
            this.valorDesconto = cupom.getValorDesconto();
            this.validadeData = cupom.getValidadeData();
            this.limiteUsos = cupom.getLimiteUsos();
            this.usosAtuais = cupom.getUsosAtuais();
        }

        public Long getId()                       { return id; }
        public String getCodigo()                 { return codigo; }
        public String getTipoDesconto()           { return tipoDesconto; }
        public BigDecimal getValorDesconto()      { return valorDesconto; }
        public LocalDate getValidadeData()        { return validadeData; }
        public Integer getLimiteUsos()            { return limiteUsos; }
        public int getUsosAtuais()                 { return usosAtuais; }

        public void setId(Long id)                                   { this.id = id; }
        public void setCodigo(String codigo)                         { this.codigo = codigo; }
        public void setTipoDesconto(String tipoDesconto)             { this.tipoDesconto = tipoDesconto; }
        public void setValorDesconto(BigDecimal valorDesconto)       { this.valorDesconto = valorDesconto; }
        public void setValidadeData(LocalDate validadeData)          { this.validadeData = validadeData; }
        public void setLimiteUsos(Integer limiteUsos)                 { this.limiteUsos = limiteUsos; }
        public void setUsosAtuais(int usosAtuais)                     { this.usosAtuais = usosAtuais; }
    }
}