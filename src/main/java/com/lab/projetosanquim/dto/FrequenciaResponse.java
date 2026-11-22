package com.lab.projetosanquim.dto;

import java.math.BigDecimal;
import java.util.List;

public record FrequenciaResponse(
        Long alunoId, long aulasRegistradas, long presencas, long faltas,
        BigDecimal percentualGlobal, List<FrequenciaDisciplinaResponse> disciplinas
) {
}
