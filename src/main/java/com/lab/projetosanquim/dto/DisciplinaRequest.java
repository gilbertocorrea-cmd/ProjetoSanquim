package com.lab.projetosanquim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DisciplinaRequest(@NotBlank @Size(max = 255) String nome) {
}
