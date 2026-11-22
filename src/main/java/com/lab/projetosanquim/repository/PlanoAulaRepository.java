package com.lab.projetosanquim.repository;

import com.lab.projetosanquim.entity.PlanoAula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface PlanoAulaRepository extends JpaRepository<PlanoAula, Long> {
    @Query("""
            select p from PlanoAula p
            join fetch p.turma
            join fetch p.disciplina
            where p.dataAula <= :hoje
            and exists (
                select m.id from Matricula m
                where m.aluno.id = :alunoId and m.turma = p.turma and m.ativa = true
            )
            order by p.dataAula desc, p.id desc
            """)
    List<PlanoAula> consultarDoAluno(@Param("alunoId") Long alunoId, @Param("hoje") LocalDate hoje);
}
