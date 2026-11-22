package com.lab.projetosanquim.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AvaliacaoRequest(
        @NotNull @Positive Long turmaId,
        @NotNull @Positive Long disciplinaId,
        @NotBlank @Size(max = 255) String titulo,
        @NotBlank @Size(max = 255) String tipo,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 15, fraction = 4) BigDecimal notaMaxima,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 15, fraction = 4) BigDecimal peso,
        @NotNull LocalDate dataEntrega
) {
}
