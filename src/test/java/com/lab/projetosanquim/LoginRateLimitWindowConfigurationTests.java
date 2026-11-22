package com.lab.projetosanquim;

import com.lab.projetosanquim.config.LoginRateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:janela30m;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "sanquim.demo.seed=false",
        "sanquim.auth.login-rate-limit.window=30m",
        "SANQUIM_JWT_SECRET=" + ProjetoSanquimApplicationTests.CHAVE_TESTE
})
class LoginRateLimitWindowConfigurationTests {
    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private LoginRateLimitProperties properties;

    @Autowired
    private ScheduledAnnotationBeanPostProcessor scheduler;

    @Test
    void aplicacaoIniciaEAgendaLimpezaComJanelaNoFormatoBoot() {
        assertThat(context.isActive()).isTrue();
        assertThat(properties.window()).isEqualTo(Duration.ofMinutes(30));
        assertThat(scheduler.getScheduledTasks()).hasSize(1);
    }
}
