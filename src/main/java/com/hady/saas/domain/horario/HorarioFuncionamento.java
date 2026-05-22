package com.hady.saas.domain.horario;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "horarios_funcionamento")
public class HorarioFuncionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek diaSemana; // MONDAY, TUESDAY... SUNDAY

    @Column(nullable = false)
    private LocalTime horaAbertura; // ex: 08:00

    @Column(nullable = false)
    private LocalTime horaFechamento; // ex: 18:00

    // Intervalo de almoço (opcional)
    private LocalTime inicioAlmoco; // ex: 12:00
    private LocalTime fimAlmoco;    // ex: 13:00

    // Intervalo entre atendimentos em minutos (ex: 10 minutos de folga entre clientes)
    private int intervaloMinutos = 0;

    private boolean aberto = true; // false = estabelecimento fechado nesse dia

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    // Construtor vazio obrigatório para JPA
    public HorarioFuncionamento() {}

    // GETTERS
    public Long getId() { return id; }
    public DayOfWeek getDiaSemana() { return diaSemana; }
    public LocalTime getHoraAbertura() { return horaAbertura; }
    public LocalTime getHoraFechamento() { return horaFechamento; }
    public LocalTime getInicioAlmoco() { return inicioAlmoco; }
    public LocalTime getFimAlmoco() { return fimAlmoco; }
    public int getIntervaloMinutos() { return intervaloMinutos; }
    public boolean isAberto() { return aberto; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }

    // SETTERS
    public void setId(Long id) { this.id = id; }
    public void setDiaSemana(DayOfWeek diaSemana) { this.diaSemana = diaSemana; }
    public void setHoraAbertura(LocalTime horaAbertura) { this.horaAbertura = horaAbertura; }
    public void setHoraFechamento(LocalTime horaFechamento) { this.horaFechamento = horaFechamento; }
    public void setInicioAlmoco(LocalTime inicioAlmoco) { this.inicioAlmoco = inicioAlmoco; }
    public void setFimAlmoco(LocalTime fimAlmoco) { this.fimAlmoco = fimAlmoco; }
    public void setIntervaloMinutos(int intervaloMinutos) { this.intervaloMinutos = intervaloMinutos; }
    public void setAberto(boolean aberto) { this.aberto = aberto; }
    public void setEstabelecimento(Estabelecimento estabelecimento) { this.estabelecimento = estabelecimento; }
}
