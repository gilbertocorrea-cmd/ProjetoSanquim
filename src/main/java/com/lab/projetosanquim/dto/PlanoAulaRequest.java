package com.lab.projetosanquim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record PlanoAulaRequest(
        @NotNull @Positive Long turmaId,
        @NotNull @Positive Long disciplinaId,
        @NotNull LocalDate dataAula,
        @NotBlank String topicos
) {
}
