package com.lab.projetosanquim.repository;

import com.lab.projetosanquim.entity.Nota;
import com.lab.projetosanquim.entity.Avaliacao;
import com.lab.projetosanquim.entity.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;

public interface NotaRepository extends JpaRepository<Nota, Long> {
    List<Nota> findByAlunoId(Long alunoId);
    Optional<Nota> findByAvaliacaoAndAluno(Avaliacao avaliacao, Aluno aluno);

    // Leitura atual após o lock da avaliação, inclusive em MySQL REPEATABLE READ.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Nota n where n.avaliacao = :avaliacao and n.aluno = :aluno")
    Optional<Nota> buscarParaAtualizacao(@Param("avaliacao") Avaliacao avaliacao, @Param("aluno") Aluno aluno);
}
