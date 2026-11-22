package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.entity.Avaliacao;
import com.lab.projetosanquim.entity.Nota;
import java.math.BigDecimal;
import java.time.LocalDate;

public record NotaBoletimResponse(
        Long avaliacaoId, String titulo, String tipo, LocalDate dataEntrega,
        BigDecimal notaMaxima, BigDecimal peso, BigDecimal valor, BigDecimal notaNormalizada, String situacao
) {
    public static NotaBoletimResponse from(Avaliacao avaliacao, Nota nota, BigDecimal normalizada) {
        return new NotaBoletimResponse(avaliacao.getId(), avaliacao.getTitulo(), avaliacao.getTipo(),
                avaliacao.getDataEntrega(), avaliacao.getNotaMaxima(), avaliacao.getPeso(),
                nota == null ? null : nota.getValor(), normalizada, nota == null ? "NAO_LANCADA" : "LANCADA");
    }
}
