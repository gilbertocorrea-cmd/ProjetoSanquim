package com.lab.projetosanquim.security;

import com.lab.projetosanquim.config.LoginRateLimitProperties;
import com.lab.projetosanquim.exception.LoginRateLimitException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class InMemoryLoginAttemptLimiterTests {
    @Test
    void janelaFixaExpiraELimpaMemoria() {
        var clock = new Relogio();
        var limiter = new InMemoryLoginAttemptLimiter(
                new LoginRateLimitProperties(5, 20, Duration.ofMinutes(15)), clock);
        falhar(limiter, " Nome@sanquim.br ");
        clock.agora = clock.agora.plusSeconds(60);
        for (int i = 0; i < 4; i++) {
            falhar(limiter, "nome@sanquim.br");
        }
        assertThatThrownBy(() -> limiter.executar("nome@sanquim.br", "ip", () -> "ok"))
                .isInstanceOfSatisfying(LoginRateLimitException.class,
                        exception -> assertThat(exception.getRetryAfter()).isEqualTo(840));
        clock.agora = clock.agora.plusSeconds(840);
        limiter.limparExpirados();
        assertThat((Map<?, ?>) ReflectionTestUtils.getField(limiter, "emails")).isEmpty();
        assertThat((Map<?, ?>) ReflectionTestUtils.getField(limiter, "ips")).isEmpty();
        assertThat(limiter.executar("nome@sanquim.br", "ip", () -> "ok")).isEqualTo("ok");
    }

    @Test
    void concorrenciaNaoPermiteAutenticarAposLimite() throws Exception {
        var limiter = new InMemoryLoginAttemptLimiter(
                new LoginRateLimitProperties(5, 20, Duration.ofMinutes(15)));
        var autenticacoes = new AtomicInteger();
        var tarefas = new ArrayList<Callable<Void>>();
        for (int i = 0; i < 20; i++) {
            tarefas.add(() -> {
                try {
                    limiter.executar("nome@sanquim.br", "ip", () -> {
                        autenticacoes.incrementAndGet();
                        throw new BadCredentialsException("inválida");
                    });
                } catch (BadCredentialsException | LoginRateLimitException expected) {
                    // Ambos são resultados esperados das tentativas concorrentes.
                }
                return null;
            });
        }
        ExecutorService executor = Executors.newFixedThreadPool(5);
        try {
            for (Future<Void> future : executor.invokeAll(tarefas)) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(autenticacoes).hasValue(5);
    }

    @Test
    void propriedadesAlteramLimitesEJanela() {
        var limiter = new InMemoryLoginAttemptLimiter(
                new LoginRateLimitProperties(1, 1, Duration.ofSeconds(60)), new Relogio());
        falhar(limiter, "nome@sanquim.br");
        assertThatThrownBy(() -> limiter.executar("outro@sanquim.br", "ip", () -> "ok"))
                .isInstanceOfSatisfying(LoginRateLimitException.class,
                        exception -> assertThat(exception.getRetryAfter()).isEqualTo(60));
    }

    private void falhar(InMemoryLoginAttemptLimiter limiter, String email) {
        assertThatThrownBy(() -> limiter.executar(email, "ip", () -> {
            throw new BadCredentialsException("inválida");
        })).isInstanceOf(BadCredentialsException.class);
    }

    private static class Relogio extends Clock {
        private Instant agora = Instant.EPOCH;
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }
}
