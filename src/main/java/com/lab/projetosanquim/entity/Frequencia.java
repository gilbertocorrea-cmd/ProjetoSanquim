package com.lab.projetosanquim.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "frequencias", uniqueConstraints = @UniqueConstraint(name = "uk_frequencia_chamada_aluno", columnNames = {"chamada_id", "aluno_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Frequencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chamada_id", nullable = false)
    private Chamada chamada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @Column(nullable = false)
    private boolean presente;
}
