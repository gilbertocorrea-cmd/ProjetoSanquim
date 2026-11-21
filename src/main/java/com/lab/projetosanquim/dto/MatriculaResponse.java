package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.entity.Matricula;

public record MatriculaResponse(Long id, Long alunoId, Long turmaId, boolean ativa) {
    public static MatriculaResponse from(Matricula registro) {
        return new MatriculaResponse(registro.getId(), registro.getAluno().getId(), registro.getTurma().getId(), registro.isAtiva());
    }
}
