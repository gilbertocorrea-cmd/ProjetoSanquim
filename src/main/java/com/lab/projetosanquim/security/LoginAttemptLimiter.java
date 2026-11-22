package com.lab.projetosanquim.security;

import java.util.function.Supplier;

public interface LoginAttemptLimiter {
    <T> T executar(String email, String ip, Supplier<T> autenticar);
}
