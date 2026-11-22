package com.lab.projetosanquim.exception;

public class LoginRateLimitException extends RuntimeException {
    private final long retryAfter;

    public LoginRateLimitException(long retryAfter) {
        super("Limite de tentativas de login excedido");
        this.retryAfter = retryAfter;
    }

    public long getRetryAfter() {
        return retryAfter;
    }
}
