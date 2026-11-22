package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.entity.AtribuicaoDocente;

public record MinhasTurmasResponse(Long turmaId, String nomeTurma, Integer anoLetivo, boolean ativa,
                                   Long disciplinaId, String nomeDisciplina) {
    public static MinhasTurmasResponse from(AtribuicaoDocente atribuicao) {
        var turma = atribuicao.getTurma();
        var disciplina = atribuicao.getDisciplina();
        return new MinhasTurmasResponse(turma.getId(), turma.getNome(), turma.getAnoLetivo(), turma.isAtiva(),
                disciplina.getId(), disciplina.getNome());
    }
}
