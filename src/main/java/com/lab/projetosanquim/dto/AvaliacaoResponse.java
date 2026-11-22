package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.entity.Avaliacao;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AvaliacaoResponse(Long id, Long turmaId, Long disciplinaId, String titulo, String tipo,
                                BigDecimal notaMaxima, BigDecimal peso, LocalDate dataEntrega) {
    public static AvaliacaoResponse from(Avaliacao avaliacao) {
        return new AvaliacaoResponse(avaliacao.getId(), avaliacao.getTurma().getId(),
                avaliacao.getDisciplina().getId(), avaliacao.getTitulo(), avaliacao.getTipo(),
                avaliacao.getNotaMaxima(), avaliacao.getPeso(), avaliacao.getDataEntrega());
    }
}
