package com.lab.projetosanquim.repository;

import com.lab.projetosanquim.entity.Gestor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GestorRepository extends JpaRepository<Gestor, Long> {
    Optional<Gestor> findByUsuarioEmail(String email);
}