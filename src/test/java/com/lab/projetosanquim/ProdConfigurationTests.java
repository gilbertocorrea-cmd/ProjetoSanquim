package com.lab.projetosanquim;

import com.lab.projetosanquim.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class ProdConfigurationTests {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment().getPropertySources()
                    .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(JwtService.class);

    @Test
    void desenvolvimentoMantemConfiguracaoAnterior() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(JwtService.class);
            assertThat(context.getEnvironment().getProperty("spring.flyway.enabled")).isEqualTo("false");
            assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
            assertThat(context.getEnvironment().getProperty("spring.jpa.show-sql")).isEqualTo("true");
        });
    }

    @Test
    void producaoExigeSegredoAusenteOuVazio() {
        runner.withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("spring.profiles.active=prod", "SANQUIM_JWT_SECRET=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void producaoConfiguraFlywayValidacaoEDesativaSwagger() {
        runner.withPropertyValues("spring.profiles.active=prod",
                        "SANQUIM_JWT_SECRET=" + ProjetoSanquimApplicationTests.CHAVE_TESTE)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(JwtService.class);
                    var environment = context.getEnvironment();
                    assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("true");
                    assertThat(environment.getProperty("spring.flyway.baseline-on-migrate")).isEqualTo("true");
                    assertThat(environment.getProperty("spring.flyway.baseline-version")).isEqualTo("1");
                    assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
                    assertThat(environment.getProperty("spring.jpa.show-sql")).isEqualTo("false");
                    assertThat(environment.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
                    assertThat(environment.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
                });
    }
}
