package com.lab.projetosanquim.repository;

import com.lab.projetosanquim.entity.Disciplina;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisciplinaRepository extends JpaRepository<Disciplina, Long> {
    java.util.Optional<Disciplina> findFirstByNomeOrderByIdAsc(String nome);

}
