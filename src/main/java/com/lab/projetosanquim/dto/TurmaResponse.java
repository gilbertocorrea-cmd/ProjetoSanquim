package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.entity.Turma;

public record TurmaResponse(Long id, String nome, Integer anoLetivo, boolean ativa) {
    public static TurmaResponse from(Turma turma) {
        return new TurmaResponse(turma.getId(), turma.getNome(), turma.getAnoLetivo(), turma.isAtiva());
    }
}
