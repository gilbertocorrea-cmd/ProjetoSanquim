package com.lab.projetosanquim.repository;

import com.lab.projetosanquim.entity.Frequencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface FrequenciaRepository extends JpaRepository<Frequencia, Long> {
    @Query("""
            select f from Frequencia f
            join fetch f.chamada c
            join fetch c.turma t
            join fetch c.disciplina d
            where f.aluno.id = :alunoId
            order by t.id, d.id, c.dataAula, f.id
            """)
    List<Frequencia> consultarDoAluno(@Param("alunoId") Long alunoId);
}
