package com.lab.projetosanquim.repository;

import com.lab.projetosanquim.entity.Avaliacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    @Query("""
            select a from Avaliacao a
            join fetch a.turma t
            join fetch a.disciplina d
            where exists (
                select m.id from Matricula m
                where m.aluno.id = :alunoId and m.turma = a.turma and m.ativa = true
            ) or exists (
                select n.id from Nota n where n.aluno.id = :alunoId and n.avaliacao = a
            )
            order by t.id, d.id, a.dataEntrega, a.id
            """)
    List<Avaliacao> consultarDoAluno(@Param("alunoId") Long alunoId);
    Optional<Avaliacao> findFirstByTurmaIdAndDisciplinaIdAndTituloOrderByIdAsc(
            Long turmaId, Long disciplinaId, String titulo);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Avaliacao a where a.id = :id")
    Optional<Avaliacao> buscarParaAtualizacao(@Param("id") Long id);
}
