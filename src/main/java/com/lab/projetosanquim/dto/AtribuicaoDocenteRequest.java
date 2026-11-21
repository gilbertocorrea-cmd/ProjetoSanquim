package com.lab.projetosanquim.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AtribuicaoDocenteRequest(
        @NotNull @Positive Long professorId,
        @NotNull @Positive Long turmaId,
        @NotNull @Positive Long disciplinaId
) {
}
