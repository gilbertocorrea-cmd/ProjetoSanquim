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
public class MatriculaService {
    private final MatriculaRepository matriculas;
    private final AlunoRepository alunos;
    private final TurmaService turmas;

    @Transactional
    public MatriculaResponse cadastrarMatricula(MatriculaRequest request) {
        Aluno aluno = alunos.findById(request.alunoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado"));
        Turma turma = turmas.buscarAtiva(request.turmaId());
        if (matriculas.existsByAlunoIdAndTurmaId(aluno.getId(), turma.getId())) {
            throw new RegraNegocioException("O aluno já possui matrícula nesta turma");
        }

        Matricula matricula = matriculas.saveAndFlush(Matricula.builder()
                .aluno(aluno).turma(turma).ativa(true).build());
        return MatriculaResponse.from(matricula);
    }

    public PaginaResponse<MatriculaResponse> listarMatriculas(Pageable paginacao) {
        return PaginaResponse.from(matriculas.findAll(paginacao).map(MatriculaResponse::from));
    }
}
