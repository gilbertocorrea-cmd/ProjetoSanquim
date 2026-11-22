package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.entity.Avaliacao;
import java.math.BigDecimal;
import java.util.List;

public record DisciplinaBoletimResponse(
        Long turmaId, String nomeTurma, Integer anoLetivo, Long disciplinaId, String nomeDisciplina,
        List<NotaBoletimResponse> avaliacoes, BigDecimal mediaParcial
) {
    public static DisciplinaBoletimResponse from(
            Avaliacao referencia, List<NotaBoletimResponse> avaliacoes, BigDecimal media
    ) {
        return new DisciplinaBoletimResponse(
                referencia.getTurma().getId(), referencia.getTurma().getNome(),
                referencia.getTurma().getAnoLetivo(), referencia.getDisciplina().getId(),
                referencia.getDisciplina().getNome(), List.copyOf(avaliacoes), media);
    }
}
