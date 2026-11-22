package com.lab.projetosanquim.exception;

public class ParametroPaginacaoException extends RuntimeException {
    private final String campo;

    public ParametroPaginacaoException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
