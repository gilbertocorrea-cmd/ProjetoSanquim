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
public class GestorService {
    private final GestorRepository gestores;
    private final UsuarioService usuarios;

    @Transactional
    public GestorResponse cadastrarGestor(GestorRequest request) {
        Usuario usuario = usuarios.criarUsuario(request.email(), request.senha(), Role.ROLE_GESTAO);
        Gestor gestor = gestores.saveAndFlush(Gestor.builder()
                .nome(request.nome().strip())
                .cargo(request.cargo().strip())
                .usuario(usuario)
                .build());
        return GestorResponse.from(gestor);
    }

    public PaginaResponse<GestorResponse> listarGestores(Pageable paginacao) {
        return PaginaResponse.from(gestores.findAll(paginacao).map(GestorResponse::from));
    }
}
