package com.lab.projetosanquim.dto;

import java.util.List;

public record ErroResponse(int status, String mensagem, List<CampoInvalido> camposInvalidos) {
    public ErroResponse(int status, String mensagem) {
        this(status, mensagem, List.of());
    }

    public record CampoInvalido(String campo, String mensagem) {
    }
}
