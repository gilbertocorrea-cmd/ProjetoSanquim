package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.entity.Nota;
import java.math.BigDecimal;

public record NotaResponse(Long id, Long avaliacaoId, Long alunoId, BigDecimal valor) {
    public static NotaResponse from(Nota nota) {
        return new NotaResponse(nota.getId(), nota.getAvaliacao().getId(), nota.getAluno().getId(), nota.getValor());
    }
}
