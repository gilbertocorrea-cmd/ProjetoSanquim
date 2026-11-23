package com.lab.projetosanquim.repository;

import com.lab.projetosanquim.entity.Matricula;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    boolean existsByAlunoIdAndTurmaId(Long alunoId, Long turmaId);
    boolean existsByAlunoIdAndTurmaIdAndAtivaTrue(Long alunoId, Long turmaId);
    List<Matricula> findByTurmaIdAndAtivaTrueAndAlunoIdIn(Long turmaId, Collection<Long> alunoIds);
    List<Matricula> findByTurmaIdAndAtivaTrue(Long turmaId);
}
