package com.lab.projetosanquim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PlanoAulaRequest(
        @NotNull @Positive Long turmaId,
        @NotNull @Positive Long disciplinaId,
        @NotNull LocalDate dataAula,
        @NotBlank @Size(max = 10000) String topicos
) {
}
