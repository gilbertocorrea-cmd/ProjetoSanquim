package com.lab.projetosanquim.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "chamadas", uniqueConstraints = @UniqueConstraint(name = "uk_chamada_turma_disciplina_data", columnNames = {"turma_id", "disciplina_id", "data_aula"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chamada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turma_id", nullable = false)
    private Turma turma;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "disciplina_id", nullable = false)
    private Disciplina disciplina;

    @Column(nullable = false)
    private LocalDate dataAula;
}
