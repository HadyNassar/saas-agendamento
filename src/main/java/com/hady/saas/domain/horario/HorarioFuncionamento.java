package com.hady.saas.domain.horario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(name = "horarios_funcionamento")
public class HorarioFuncionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "varchar")
    private String diaSemana;

    @Column(nullable = false)
    private LocalTime horaAbertura;

    @Column(nullable = false)
    private LocalTime horaFechamento;

    private LocalTime inicioAlmoco;
    private LocalTime fimAlmoco;
    private int intervaloMinutos = 0;
    private boolean aberto = true;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    public HorarioFuncionamento() {}

    public Long getId() { return id; }
    public String getDiaSemana() { return diaSemana; }
    public LocalTime getHoraAbertura() { return horaAbertura; }
    public LocalTime getHoraFechamento() { return horaFechamento; }
    public LocalTime getInicioAlmoco() { return inicioAlmoco; }
    public LocalTime getFimAlmoco() { return fimAlmoco; }
    public int getIntervaloMinutos() { return intervaloMinutos; }
    public boolean isAberto() { return aberto; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }

    public void setId(Long id) { this.id = id; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }
    public void setHoraAbertura(LocalTime horaAbertura) { this.horaAbertura = horaAbertura; }
    public void setHoraFechamento(LocalTime horaFechamento) { this.horaFechamento = horaFechamento; }
    public void setInicioAlmoco(LocalTime inicioAlmoco) { this.inicioAlmoco = inicioAlmoco; }
    public void setFimAlmoco(LocalTime fimAlmoco) { this.fimAlmoco = fimAlmoco; }
    public void setIntervaloMinutos(int intervaloMinutos) { this.intervaloMinutos = intervaloMinutos; }
    public void setAberto(boolean aberto) { this.aberto = aberto; }
    public void setEstabelecimento(Estabelecimento estabelecimento) { this.estabelecimento = estabelecimento; }
}