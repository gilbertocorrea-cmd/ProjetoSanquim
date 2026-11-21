package com.lab.projetosanquim.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GestorRequest(
        @NotBlank @Size(max = 255) String nome,
        @NotBlank @Size(max = 255) String cargo,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 72) @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) String senha
) {
}
