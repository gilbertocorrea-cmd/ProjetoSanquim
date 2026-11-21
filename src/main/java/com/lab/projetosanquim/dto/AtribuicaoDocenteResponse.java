package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.entity.AtribuicaoDocente;

public record AtribuicaoDocenteResponse(Long id, Long professorId, Long turmaId, Long disciplinaId) {
    public static AtribuicaoDocenteResponse from(AtribuicaoDocente registro) {
        return new AtribuicaoDocenteResponse(registro.getId(), registro.getProfessor().getId(), registro.getTurma().getId(), registro.getDisciplina().getId());
    }
}
