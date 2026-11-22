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
public class TurmaService {
    private final TurmaRepository turmas;

    @Transactional
    public TurmaResponse cadastrarTurma(TurmaRequest request) {
        Turma turma = turmas.saveAndFlush(Turma.builder()
                .nome(request.nome().strip())
                .anoLetivo(request.anoLetivo())
                .ativa(request.ativa())
                .build());
        return TurmaResponse.from(turma);
    }

    public PaginaResponse<TurmaResponse> listarTurmas(Pageable paginacao) {
        return PaginaResponse.from(turmas.findAll(paginacao).map(TurmaResponse::from));
    }

    public Turma buscarAtiva(Long id) {
        Turma turma = turmas.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma não encontrada"));
        if (!turma.isAtiva()) {
            throw new RegraNegocioException("A turma está inativa");
        }
        return turma;
    }
}
