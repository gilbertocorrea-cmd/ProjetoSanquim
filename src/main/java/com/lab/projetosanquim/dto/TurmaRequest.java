package com.lab.projetosanquim.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TurmaRequest(
        @NotBlank @Size(max = 255) String nome,
        @NotNull @Min(1) @Max(9999) Integer anoLetivo,
        @NotNull Boolean ativa
) {
}
