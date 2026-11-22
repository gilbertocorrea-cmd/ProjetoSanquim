package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.exception.ParametroPaginacaoException;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public record PaginacaoRequest(
        @Schema(defaultValue = "0", description = "Página a partir de zero") String page,
        @Schema(defaultValue = "20", description = "Quantidade entre 1 e 100") String size
) {
    public PageRequest toPageable() {
        int pagina = inteiro(page, 0, "page");
        int tamanho = inteiro(size, 20, "size");
        if (pagina < 0) {
            throw new ParametroPaginacaoException("page", "A página deve ser maior ou igual a zero");
        }
        if (tamanho < 1 || tamanho > 100) {
            throw new ParametroPaginacaoException("size", "O tamanho deve estar entre 1 e 100");
        }
        if ((long) pagina * tamanho > Integer.MAX_VALUE) {
            throw new ParametroPaginacaoException("page", "A página excede o intervalo suportado");
        }
        return PageRequest.of(pagina, tamanho, Sort.by("id"));
    }

    private static int inteiro(String valor, int padrao, String campo) {
        if (valor == null) {
            return padrao;
        }
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException exception) {
            throw new ParametroPaginacaoException(campo, "Informe um número inteiro válido");
        }
    }
}
