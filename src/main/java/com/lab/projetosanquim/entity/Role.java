package com.lab.projetosanquim.entity;

import jakarta.persistence.EnumeratedValue;

public enum Role {
    ROLE_GESTAO("ROLE_GESTOR"),
    ROLE_PROFESSOR("ROLE_PROFESSOR"),
    ROLE_ALUNO("ROLE_ALUNO");

    // Preserva o valor de gestão nas bases MySQL criadas pela versão anterior.
    @EnumeratedValue
    private final String valorPersistido;

    Role(String valorPersistido) {
        this.valorPersistido = valorPersistido;
    }
}
