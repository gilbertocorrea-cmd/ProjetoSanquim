package com.lab.projetosanquim.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "gestores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gestor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String cargo;

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;
}