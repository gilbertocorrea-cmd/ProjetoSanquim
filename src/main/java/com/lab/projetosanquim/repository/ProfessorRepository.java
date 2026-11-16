package com.lab.projetosanquim.repository;

import com.lab.projetosanquim.entity.Professor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {
    Optional<Professor> findByUsuarioEmail(String email);
    Optional<Professor> findByUsuarioId(Long usuarioId);
}
