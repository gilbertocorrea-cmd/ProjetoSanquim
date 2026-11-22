package com.lab.projetosanquim.service;

import com.lab.projetosanquim.dto.*;
import com.lab.projetosanquim.entity.*;
import com.lab.projetosanquim.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DisciplinaService {
    private final DisciplinaRepository disciplinas;

    @Transactional
    public DisciplinaResponse cadastrarDisciplina(DisciplinaRequest request) {
        Disciplina disciplina = disciplinas.saveAndFlush(
                Disciplina.builder().nome(request.nome().strip()).build());
        return DisciplinaResponse.from(disciplina);
    }

    public PaginaResponse<DisciplinaResponse> listarDisciplinas(Pageable paginacao) {
        return PaginaResponse.from(disciplinas.findAll(paginacao).map(DisciplinaResponse::from));
    }
}
