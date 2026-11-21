package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.entity.Aluno;
import com.lab.projetosanquim.entity.Role;

public record AlunoResponse(Long id, Long usuarioId, String nome, String email, Role role) {
    public static AlunoResponse from(Aluno aluno) {
        var usuario = aluno.getUsuario();
        return new AlunoResponse(aluno.getId(), usuario.getId(), aluno.getNome(),
                usuario.getEmail(), usuario.getRole());
    }
}
