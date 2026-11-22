package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.entity.PlanoAula;
import java.time.LocalDate;

public record AulaResponse(Long id, Long turmaId, Long disciplinaId, LocalDate dataAula, String topicos) {
    public static AulaResponse from(PlanoAula plano) {
        return new AulaResponse(plano.getId(), plano.getTurma().getId(), plano.getDisciplina().getId(),
                plano.getDataAula(), plano.getTopicos());
    }
}
