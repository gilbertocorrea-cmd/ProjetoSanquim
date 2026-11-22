package com.lab.projetosanquim.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RegistroPresenca(@NotNull @Positive Long alunoId, @NotNull Boolean presente) {
}
