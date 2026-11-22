package com.lab.projetosanquim.repository;

import com.lab.projetosanquim.entity.Nota;
import com.lab.projetosanquim.entity.Avaliacao;
import com.lab.projetosanquim.entity.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface NotaRepository extends JpaRepository<Nota, Long> {
    List<Nota> findByAlunoId(Long alunoId);
    Optional<Nota> findByAvaliacaoAndAluno(Avaliacao avaliacao, Aluno aluno);
}
