package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.entity.Chamada;
import java.math.BigDecimal;

public record FrequenciaDisciplinaResponse(
        Long turmaId, String nomeTurma, Integer anoLetivo, Long disciplinaId, String nomeDisciplina,
        long aulasRegistradas, long presencas, long faltas, BigDecimal percentualPresenca
) {
    public static FrequenciaDisciplinaResponse from(
            Chamada referencia, long aulas, long presencas, BigDecimal percentual
    ) {
        return new FrequenciaDisciplinaResponse(
                referencia.getTurma().getId(), referencia.getTurma().getNome(),
                referencia.getTurma().getAnoLetivo(), referencia.getDisciplina().getId(),
                referencia.getDisciplina().getNome(), aulas, presencas, aulas - presencas, percentual);
    }
}
