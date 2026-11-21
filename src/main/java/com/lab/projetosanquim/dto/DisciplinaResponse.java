package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.entity.Disciplina;

public record DisciplinaResponse(Long id, String nome) {
    public static DisciplinaResponse from(Disciplina disciplina) {
        return new DisciplinaResponse(disciplina.getId(), disciplina.getNome());
    }
}
