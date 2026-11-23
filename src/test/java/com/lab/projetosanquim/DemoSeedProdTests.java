package com.lab.projetosanquim;

import com.lab.projetosanquim.config.DataSeeder;
import com.lab.projetosanquim.config.DiarioDataSeeder;
import com.lab.projetosanquim.repository.*;
import com.lab.projetosanquim.service.DiarioService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DemoSeedProdTests {
    @ParameterizedTest
    @ValueSource(classes = {DataSeeder.class, DiarioDataSeeder.class})
    void cadaSeederFalhaNaInicializacaoEmProd(Class<?> seeder) {
        contexto(seeder, "prod", true).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasStackTraceContaining("A carga de demonstração não pode ser executada no perfil prod");
        });
    }

    @ParameterizedTest
    @ValueSource(classes = {DataSeeder.class, DiarioDataSeeder.class})
    void foraDeProdSeederContinuaDisponivel(Class<?> seeder) {
        contexto(seeder, "dev", true).run(context -> assertThat(context).hasNotFailed().hasSingleBean(seeder));
    }

    @ParameterizedTest
    @ValueSource(classes = {DataSeeder.class, DiarioDataSeeder.class})
    void prodSemDemoNaoCriaSeeder(Class<?> seeder) {
        contexto(seeder, "prod", false).run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(seeder));
    }

    private ApplicationContextRunner contexto(Class<?> seeder, String perfil, boolean seed) {
        return new ApplicationContextRunner().withUserConfiguration(seeder)
                .withPropertyValues("spring.profiles.active=" + perfil, "sanquim.demo.seed=" + seed,
                        "SANQUIM_DEMO_PASSWORD=fixture-apenas-teste")
                .withBean(UsuarioRepository.class, () -> mock(UsuarioRepository.class))
                .withBean(GestorRepository.class, () -> mock(GestorRepository.class))
                .withBean(ProfessorRepository.class, () -> mock(ProfessorRepository.class))
                .withBean(AlunoRepository.class, () -> mock(AlunoRepository.class))
                .withBean(TurmaRepository.class, () -> mock(TurmaRepository.class))
                .withBean(DisciplinaRepository.class, () -> mock(DisciplinaRepository.class))
                .withBean(MatriculaRepository.class, () -> mock(MatriculaRepository.class))
                .withBean(AtribuicaoDocenteRepository.class, () -> mock(AtribuicaoDocenteRepository.class))
                .withBean(ChamadaRepository.class, () -> mock(ChamadaRepository.class))
                .withBean(AvaliacaoRepository.class, () -> mock(AvaliacaoRepository.class))
                .withBean(NotaRepository.class, () -> mock(NotaRepository.class))
                .withBean(DiarioService.class, () -> mock(DiarioService.class))
                .withBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class));
    }
}
