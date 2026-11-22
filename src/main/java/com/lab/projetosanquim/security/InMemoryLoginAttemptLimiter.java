package com.lab.projetosanquim.security;

import com.lab.projetosanquim.config.LoginRateLimitProperties;
import com.lab.projetosanquim.exception.LoginRateLimitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class InMemoryLoginAttemptLimiter implements LoginAttemptLimiter {
    private final LoginRateLimitProperties properties;
    private final Clock clock;
    private final Object lock = new Object();
    private final Map<String, Falhas> emails = new HashMap<>();
    private final Map<String, Falhas> ips = new HashMap<>();

    @Autowired
    public InMemoryLoginAttemptLimiter(LoginRateLimitProperties properties) {
        this(properties, Clock.systemUTC());
    }

    InMemoryLoginAttemptLimiter(LoginRateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public <T> T executar(String email, String ip, Supplier<T> autenticar) {
        String chaveEmail;
        Instant janelaEmail;
        Instant janelaIp;
        synchronized (lock) {
            chaveEmail = email.strip().toLowerCase(Locale.ROOT);
            Instant agora = clock.instant();
            removerExpirada(emails, chaveEmail, agora);
            removerExpirada(ips, ip, agora);
            long espera = Math.max(
                    espera(emails.get(chaveEmail), properties.emailLimit(), agora),
                    espera(ips.get(ip), properties.ipLimit(), agora));
            if (espera > 0) {
                throw new LoginRateLimitException(espera);
            }
            janelaEmail = reservar(emails, chaveEmail, agora);
            janelaIp = reservar(ips, ip, agora);
        }

        try {
            T resultado = autenticar.get();
            synchronized (lock) {
                Falhas falhasEmail = emails.get(chaveEmail);
                if (falhasEmail != null && falhasEmail.expiraEm().equals(janelaEmail)) {
                    emails.remove(chaveEmail);
                }
                desfazerReserva(ips, ip, janelaIp);
            }
            return resultado;
        } catch (BadCredentialsException exception) {
            // A reserva já contabilizou a falha de credencial.
            throw exception;
        } catch (RuntimeException | Error exception) {
            synchronized (lock) {
                desfazerReserva(emails, chaveEmail, janelaEmail);
                desfazerReserva(ips, ip, janelaIp);
            }
            throw exception;
        }
    }

    @Scheduled(fixedDelayString = "${sanquim.auth.login-rate-limit.window:PT15M}")
    public void limparExpirados() {
        synchronized (lock) {
            Instant agora = clock.instant();
            emails.values().removeIf(falhas -> !falhas.expiraEm().isAfter(agora));
            ips.values().removeIf(falhas -> !falhas.expiraEm().isAfter(agora));
        }
    }

    private void removerExpirada(Map<String, Falhas> contadores, String chave, Instant agora) {
        Falhas falhas = contadores.get(chave);
        if (falhas != null && !falhas.expiraEm().isAfter(agora)) {
            contadores.remove(chave);
        }
    }

    private Instant reservar(Map<String, Falhas> contadores, String chave, Instant agora) {
        Falhas anterior = contadores.get(chave);
        Falhas reserva;
        if (anterior == null) {
            reserva = new Falhas(1, agora.plus(properties.window()));
        } else {
            reserva = new Falhas(anterior.quantidade() + 1, anterior.expiraEm());
        }
        contadores.put(chave, reserva);
        return reserva.expiraEm();
    }

    private void desfazerReserva(Map<String, Falhas> contadores, String chave, Instant janela) {
        Falhas falhas = contadores.get(chave);
        if (falhas == null || !falhas.expiraEm().equals(janela)) {
            return;
        }
        if (falhas.quantidade() == 1) {
            contadores.remove(chave);
        } else {
            contadores.put(chave, new Falhas(falhas.quantidade() - 1, falhas.expiraEm()));
        }
    }

    private long espera(Falhas falhas, int limite, Instant agora) {
        if (falhas == null || falhas.quantidade() < limite) {
            return 0;
        }
        Duration restante = Duration.between(agora, falhas.expiraEm());
        return restante.getSeconds() + (restante.getNano() > 0 ? 1 : 0);
    }

    private record Falhas(int quantidade, Instant expiraEm) {
    }
}
