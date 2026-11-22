package com.lab.projetosanquim.repository;

import com.lab.projetosanquim.entity.Chamada;
import com.lab.projetosanquim.entity.Turma;
import com.lab.projetosanquim.entity.Disciplina;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;

public interface ChamadaRepository extends JpaRepository<Chamada, Long> {
    boolean existsByTurmaAndDisciplinaAndDataAula(Turma turma, Disciplina disciplina, LocalDate dataAula);
}
