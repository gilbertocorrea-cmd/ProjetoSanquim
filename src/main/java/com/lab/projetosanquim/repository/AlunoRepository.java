package com.lab.projetosanquim.repository;

import com.lab.projetosanquim.entity.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {
    Optional<Aluno> findByUsuarioEmail(String email);
    Optional<Aluno> findByUsuarioId(Long usuarioId);
}