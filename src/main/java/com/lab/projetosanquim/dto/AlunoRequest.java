package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.validation.MaxBytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlunoRequest(
        @NotBlank @Size(max = 255) String nome,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @MaxBytes @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) String senha
) {
}
