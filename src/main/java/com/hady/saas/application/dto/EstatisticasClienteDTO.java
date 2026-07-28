package com.hady.saas.application.dto;

import com.hady.saas.domain.enums.EstagioCliente;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EstatisticasClienteDTO {

    private final EstagioCliente estagio;
    private final long totalVisitas;
    private final BigDecimal ticketMedio;
    private final LocalDateTime ultimaVisita;

    public EstatisticasClienteDTO(EstagioCliente estagio, long totalVisitas,
                                  BigDecimal ticketMedio, LocalDateTime ultimaVisita) {
        this.estagio = estagio;
        this.totalVisitas = totalVisitas;
        this.ticketMedio = ticketMedio;
        this.ultimaVisita = ultimaVisita;
    }

    public EstagioCliente getEstagio() { return estagio; }
    public long getTotalVisitas() { return totalVisitas; }
    public BigDecimal getTicketMedio() { return ticketMedio; }
    public LocalDateTime getUltimaVisita() { return ultimaVisita; }
}