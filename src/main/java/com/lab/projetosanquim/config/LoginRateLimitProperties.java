package com.lab.projetosanquim.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("sanquim.auth.login-rate-limit")
public record LoginRateLimitProperties(
        @DefaultValue("5") int emailLimit,
        @DefaultValue("20") int ipLimit,
        @DefaultValue("15m") Duration window
) {
    public LoginRateLimitProperties {
        if (emailLimit <= 0 || ipLimit <= 0 || window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Os limites e a janela de login devem ser positivos");
        }
    }
}
