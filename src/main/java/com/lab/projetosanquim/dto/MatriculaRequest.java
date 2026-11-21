package com.lab.projetosanquim.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MatriculaRequest(
        @NotNull @Positive Long alunoId,
        @NotNull @Positive Long turmaId
) {
}
