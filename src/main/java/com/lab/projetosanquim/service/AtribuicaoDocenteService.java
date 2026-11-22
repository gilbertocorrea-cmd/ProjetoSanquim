package com.lab.projetosanquim.service;

import com.lab.projetosanquim.dto.*;
import com.lab.projetosanquim.entity.*;
import com.lab.projetosanquim.repository.*;
import com.lab.projetosanquim.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AtribuicaoDocenteService {
    private final AtribuicaoDocenteRepository atribuicoes;
    private final ProfessorRepository professores;
    private final DisciplinaRepository disciplinas;
    private final TurmaService turmas;

    @Transactional
    public AtribuicaoDocenteResponse cadastrarAtribuicao(AtribuicaoDocenteRequest request) {
        Professor professor = professores.findById(request.professorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor não encontrado"));
        Turma turma = turmas.buscarAtiva(request.turmaId());
        Disciplina disciplina = disciplinas.findById(request.disciplinaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Disciplina não encontrada"));
        if (atribuicoes.existsByProfessorIdAndTurmaIdAndDisciplinaId(
                professor.getId(), turma.getId(), disciplina.getId())) {
            throw new RegraNegocioException("O professor já está atribuído a esta turma e disciplina");
        }

        AtribuicaoDocente atribuicao = atribuicoes.saveAndFlush(AtribuicaoDocente.builder()
                .professor(professor).turma(turma).disciplina(disciplina).build());
        return AtribuicaoDocenteResponse.from(atribuicao);
    }

    public PaginaResponse<AtribuicaoDocenteResponse> listarAtribuicoes(Pageable paginacao) {
        return PaginaResponse.from(atribuicoes.findAll(paginacao).map(AtribuicaoDocenteResponse::from));
    }
}
