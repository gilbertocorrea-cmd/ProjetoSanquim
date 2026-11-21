package com.lab.projetosanquim.repository;

import com.lab.projetosanquim.entity.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    boolean existsByAlunoIdAndTurmaId(Long alunoId, Long turmaId);
}
