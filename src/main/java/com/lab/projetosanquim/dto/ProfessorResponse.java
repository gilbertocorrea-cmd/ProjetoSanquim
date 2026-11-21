package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.entity.Professor;
import com.lab.projetosanquim.entity.Role;

public record ProfessorResponse(Long id, Long usuarioId, String nome, String email, Role role) {
    public static ProfessorResponse from(Professor professor) {
        var usuario = professor.getUsuario();
        return new ProfessorResponse(professor.getId(), usuario.getId(), professor.getNome(),
                usuario.getEmail(), usuario.getRole());
    }
}
