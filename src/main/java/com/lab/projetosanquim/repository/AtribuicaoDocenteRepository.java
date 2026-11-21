package com.lab.projetosanquim.repository;

import com.lab.projetosanquim.entity.AtribuicaoDocente;
import com.lab.projetosanquim.entity.Professor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtribuicaoDocenteRepository extends JpaRepository<AtribuicaoDocente, Long> {
    boolean existsByProfessorIdAndTurmaIdAndDisciplinaId(Long professorId, Long turmaId, Long disciplinaId);

    List<AtribuicaoDocente> findByProfessor(Professor professor);
}
