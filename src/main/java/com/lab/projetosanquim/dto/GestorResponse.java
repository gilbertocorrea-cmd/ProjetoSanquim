package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.entity.Gestor;
import com.lab.projetosanquim.entity.Role;

public record GestorResponse(Long id, Long usuarioId, String nome, String cargo, String email, Role role) {
    public static GestorResponse from(Gestor gestor) {
        var usuario = gestor.getUsuario();
        return new GestorResponse(gestor.getId(), usuario.getId(), gestor.getNome(),
                gestor.getCargo(), usuario.getEmail(), usuario.getRole());
    }
}
