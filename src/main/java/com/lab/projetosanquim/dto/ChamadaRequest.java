package com.lab.projetosanquim.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;

public record ChamadaRequest(
        @NotNull @Positive Long turmaId,
        @NotNull @Positive Long disciplinaId,
        @NotNull LocalDate dataAula,
        @NotEmpty List<@NotNull @Valid RegistroPresenca> registros
) {
}
