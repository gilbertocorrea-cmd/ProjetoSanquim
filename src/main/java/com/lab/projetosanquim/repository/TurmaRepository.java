package com.lab.projetosanquim.repository;

import com.lab.projetosanquim.entity.Turma;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TurmaRepository extends JpaRepository<Turma, Long> {
    java.util.Optional<Turma> findFirstByNomeAndAnoLetivoOrderByIdAsc(String nome, Integer anoLetivo);

}
