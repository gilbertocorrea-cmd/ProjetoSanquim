package com.lab.projetosanquim.security;

import com.lab.projetosanquim.config.LoginRateLimitProperties;
import com.lab.projetosanquim.exception.LoginRateLimitException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class InMemoryLoginAttemptLimiterReservationTests {
    @Test
    void autenticacaoLentaNaoSerializaOutroEmailEIp() throws Exception {
        var limiter = new InMemoryLoginAttemptLimiter(
                new LoginRateLimitProperties(5, 20, Duration.ofMinutes(15)));
        var primeiraIniciada = new CountDownLatch(1);
        var liberarPrimeira = new CountDownLatch(1);
        var segundaConcluida = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> primeira = executor.submit(() -> limiter.executar(
                    "primeiro@sanquim.br", "ip-1", () -> {
                        primeiraIniciada.countDown();
                        aguardar(liberarPrimeira);
                        return "primeira";
                    }));
            assertThat(primeiraIniciada.await(2, TimeUnit.SECONDS)).isTrue();
            Future<String> segunda = executor.submit(() -> {
                String resultado = limiter.executar("segundo@sanquim.br", "ip-2", () -> "segunda");
                segundaConcluida.countDown();
                return resultado;
            });
            assertThat(segundaConcluida.await(2, TimeUnit.SECONDS))
                    .as("outro e-mail e IP devem concluir enquanto a primeira autenticação aguarda")
                    .isTrue();
            assertThat(segunda.get(2, TimeUnit.SECONDS)).isEqualTo("segunda");
            liberarPrimeira.countDown();
            assertThat(primeira.get(2, TimeUnit.SECONDS)).isEqualTo("primeira");
        } finally {
            liberarPrimeira.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void sucessoDesfazReservaDoIpEPreservaFalhasAnteriores() {
        var limiter = new InMemoryLoginAttemptLimiter(
                new LoginRateLimitProperties(5, 2, Duration.ofMinutes(15)));
        falhar(limiter, "falha@sanquim.br", "ip");
        assertThat(limiter.executar("sucesso@sanquim.br", "ip", () -> "ok")).isEqualTo("ok");
        falhar(limiter, "outra-falha@sanquim.br", "ip");
        assertThatThrownBy(() -> limiter.executar("novo@sanquim.br", "ip", () -> "ok"))
                .isInstanceOf(LoginRateLimitException.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void conclusaoAntigaNaoAlteraEmailNemIpDeJanelaNova(boolean sucesso) throws Exception {
        var clock = new Relogio();
        var limiter = new InMemoryLoginAttemptLimiter(
                new LoginRateLimitProperties(1, 1, Duration.ofMinutes(15)), clock);
        var iniciada = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> antiga = executor.submit(() -> limiter.executar("mesmo@sanquim.br", "ip", () -> {
                iniciada.countDown();
                aguardar(liberar);
                if (!sucesso) {
                    throw new IllegalStateException("falha de infraestrutura");
                }
                return "ok";
            }));
            assertThat(iniciada.await(2, TimeUnit.SECONDS)).isTrue();
            clock.agora.set(Instant.EPOCH.plusSeconds(901));
            falhar(limiter, "mesmo@sanquim.br", "ip");
            liberar.countDown();
            if (sucesso) {
                assertThat(antiga.get(2, TimeUnit.SECONDS)).isEqualTo("ok");
            } else {
                assertThatThrownBy(() -> antiga.get(2, TimeUnit.SECONDS))
                        .isInstanceOf(ExecutionException.class)
                        .hasCauseInstanceOf(IllegalStateException.class);
            }
            assertThatThrownBy(() -> limiter.executar("mesmo@sanquim.br", "outro-ip", () -> "ok"))
                    .isInstanceOf(LoginRateLimitException.class);
            assertThatThrownBy(() -> limiter.executar("outro@sanquim.br", "ip", () -> "ok"))
                    .isInstanceOf(LoginRateLimitException.class);
        } finally {
            liberar.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void excecaoDeInfraestruturaNaoConsomeTentativa() {
        var limiter = new InMemoryLoginAttemptLimiter(
                new LoginRateLimitProperties(1, 1, Duration.ofMinutes(15)));
        var original = new IllegalStateException("falha de banco");
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> limiter.executar("email@sanquim.br", "ip", () -> {
                throw original;
            })).isSameAs(original);
        }
        assertThat(limiter.executar("email@sanquim.br", "ip", () -> "ok")).isEqualTo("ok");
    }

    @Test
    void reservasLimitamVinteAutenticacoesSimultaneasPorIp() throws Exception {
        var limiter = new InMemoryLoginAttemptLimiter(
                new LoginRateLimitProperties(5, 20, Duration.ofMinutes(15)));
        var iniciadas = new CountDownLatch(20);
        var liberar = new CountDownLatch(1);
        var autenticacoes = new AtomicInteger();
        var bloqueadas = new AtomicInteger();
        var resultados = new ArrayList<Future<?>>();
        ExecutorService executor = Executors.newFixedThreadPool(40);
        try {
            for (int i = 0; i < 40; i++) {
                String email = "concorrente" + i + "@sanquim.br";
                resultados.add(executor.submit(() -> {
                    try {
                        limiter.executar(email, "mesmo-ip", () -> {
                            autenticacoes.incrementAndGet();
                            iniciadas.countDown();
                            aguardar(liberar);
                            throw new BadCredentialsException("inválida");
                        });
                    } catch (LoginRateLimitException expected) {
                        bloqueadas.incrementAndGet();
                    } catch (BadCredentialsException expected) {
                        // Falha já contabilizada pela reserva.
                    }
                }));
            }
            assertThat(iniciadas.await(2, TimeUnit.SECONDS)).isTrue();
            liberar.countDown();
            for (Future<?> resultado : resultados) {
                resultado.get(2, TimeUnit.SECONDS);
            }
            assertThat(autenticacoes).hasValue(20);
            assertThat(bloqueadas).hasValue(20);
        } finally {
            liberar.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static void falhar(InMemoryLoginAttemptLimiter limiter, String email, String ip) {
        assertThatThrownBy(() -> limiter.executar(email, ip, () -> {
            throw new BadCredentialsException("inválida");
        })).isInstanceOf(BadCredentialsException.class);
    }

    private static void aguardar(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timeout do latch de teste");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static class Relogio extends Clock {
        private final AtomicReference<Instant> agora = new AtomicReference<>(Instant.EPOCH);
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora.get(); }
    }
}
