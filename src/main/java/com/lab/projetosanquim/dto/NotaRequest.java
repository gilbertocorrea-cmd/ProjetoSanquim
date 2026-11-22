package com.lab.projetosanquim.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record NotaRequest(
        @NotNull @Positive Long alunoId,
        @NotNull @DecimalMin("0") @Digits(integer = 15, fraction = 4) BigDecimal valor
) {
}
