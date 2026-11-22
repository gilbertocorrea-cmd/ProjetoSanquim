package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.entity.Chamada;
import java.time.LocalDate;
import java.util.List;

public record ChamadaResponse(Long id, Long turmaId, Long disciplinaId, LocalDate dataAula,
                              List<RegistroPresenca> registros) {
    public static ChamadaResponse from(Chamada chamada, List<RegistroPresenca> registros) {
        return new ChamadaResponse(chamada.getId(), chamada.getTurma().getId(),
                chamada.getDisciplina().getId(), chamada.getDataAula(), List.copyOf(registros));
    }
}
