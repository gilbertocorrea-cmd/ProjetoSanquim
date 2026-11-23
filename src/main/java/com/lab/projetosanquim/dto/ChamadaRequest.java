package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.validation.DataNaoFutura;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record ChamadaRequest(
        @NotNull @Positive Long turmaId,
        @NotNull @Positive Long disciplinaId,
        @NotNull @DataNaoFutura LocalDate dataAula,
        @NotEmpty @Size(max = 500) List<@NotNull @Valid RegistroPresenca> registros
) {
}
