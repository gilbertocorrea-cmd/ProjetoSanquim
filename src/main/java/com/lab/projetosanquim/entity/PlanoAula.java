package com.lab.projetosanquim.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Length;
import java.time.LocalDate;

@Entity
@Table(name = "planos_aula")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanoAula {
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

    @Column(nullable = false, length = Length.LONG32)
    private String topicos;
}
