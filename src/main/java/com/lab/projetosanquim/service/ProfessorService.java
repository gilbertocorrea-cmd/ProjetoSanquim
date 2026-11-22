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
public class ProfessorService {
    private final ProfessorRepository professores;
    private final UsuarioService usuarios;

    @Transactional
    public ProfessorResponse cadastrarProfessor(ProfessorRequest request) {
        Usuario usuario = usuarios.criarUsuario(request.email(), request.senha(), Role.ROLE_PROFESSOR);
        Professor professor = professores.saveAndFlush(Professor.builder()
                .nome(request.nome().strip())
                .usuario(usuario)
                .build());
        return ProfessorResponse.from(professor);
    }

    public PaginaResponse<ProfessorResponse> listarProfessores(Pageable paginacao) {
        return PaginaResponse.from(professores.findAll(paginacao).map(ProfessorResponse::from));
    }
}
