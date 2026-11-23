package com.lab.projetosanquim;

import com.lab.projetosanquim.dto.NotaRequest;
import com.lab.projetosanquim.dto.NotaResponse;
import com.lab.projetosanquim.entity.*;
import com.lab.projetosanquim.repository.*;
import com.lab.projetosanquim.security.UsuarioDetails;
import com.lab.projetosanquim.service.DiarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Usa exclusivamente uma instância MySQL descartável, conforme ProdMySqlTests. */
@EnabledIfEnvironmentVariable(named = "SANQUIM_MYSQL_TEST_URL", matches = ".+")
class MySqlDiarioTests {
    private ConfigurableApplicationContext context;
    private Connection admin;
    private String banco;
    private boolean bancoCriado;
    private Fixture fixture;

    @BeforeEach
    void prepararBancoIsolado() throws Exception {
        String servidor = System.getenv("SANQUIM_MYSQL_TEST_URL");
        String usuario = System.getenv("SANQUIM_MYSQL_TEST_USER");
        String senha = System.getenv().getOrDefault("SANQUIM_MYSQL_TEST_PASSWORD", "");
        banco = "sanquim_diario_test_" + UUID.randomUUID().toString().replace("-", "");
        admin = DriverManager.getConnection(servidor, usuario, senha);
        try (var statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE " + banco);
            bancoCriado = true;
        }
        context = new SpringApplicationBuilder(ProjetoSanquimApplication.class).profiles("prod")
                .run("--spring.datasource.url=" + servidor + "/" + banco,
                        "--spring.datasource.username=" + usuario,
                        "--spring.datasource.password=" + senha,
                        "--spring.datasource.hikari.transaction-isolation=TRANSACTION_REPEATABLE_READ",
                        "--SANQUIM_JWT_SECRET=" + ProjetoSanquimApplicationTests.CHAVE_TESTE,
                        "--sanquim.demo.seed=false", "--server.port=0");
        fixture = transacao().execute(status -> {
            Professor autorizado = professor("autorizado");
            Professor semAtribuicao = professor("sem-atribuicao");
            Aluno aluno = context.getBean(AlunoRepository.class).saveAndFlush(Aluno.builder().nome("Aluno")
                    .usuario(usuario("aluno", Role.ROLE_ALUNO)).build());
            Turma turma = context.getBean(TurmaRepository.class).saveAndFlush(Turma.builder()
                    .nome("Turma MySQL").anoLetivo(2026).ativa(true).build());
            Disciplina disciplina = context.getBean(DisciplinaRepository.class).saveAndFlush(
                    Disciplina.builder().nome("Matemática").build());
            context.getBean(MatriculaRepository.class).saveAndFlush(Matricula.builder()
                    .aluno(aluno).turma(turma).ativa(true).build());
            context.getBean(AtribuicaoDocenteRepository.class).saveAndFlush(AtribuicaoDocente.builder()
                    .professor(autorizado).turma(turma).disciplina(disciplina).build());
            Avaliacao avaliacao = context.getBean(AvaliacaoRepository.class).saveAndFlush(Avaliacao.builder()
                    .turma(turma).disciplina(disciplina).titulo("Concorrência MySQL").tipo("PROVA")
                    .notaMaxima(BigDecimal.TEN).peso(BigDecimal.ONE).dataEntrega(LocalDate.of(2026, 11, 10)).build());
            return new Fixture(avaliacao.getId(), aluno.getId(), new UsuarioDetails(autorizado.getUsuario()),
                    new UsuarioDetails(semAtribuicao.getUsuario()));
        });
    }

    @AfterEach
    void removerSomenteBancoCriadoPeloTeste() throws Exception {
        try {
            if (context != null) context.close();
        } finally {
            if (admin != null) {
                try (Connection connection = admin) {
                    if (bancoCriado) {
                        if (!banco.matches("sanquim_diario_test_[a-f0-9]{32}")) {
                            throw new IllegalStateException("Nome de banco de teste inválido");
                        }
                        try (var statement = connection.createStatement()) { statement.execute("DROP DATABASE " + banco); }
                    }
                }
            }
        }
    }

    @Test
    void professorSemAtribuicaoRecebe403ComLockAindaRetidoNoMySql() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context)
                .apply(springSecurity()).build();
        CountDownLatch bloqueado = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> holder = executor.submit(() -> transacao().execute(status -> {
            context.getBean(AvaliacaoRepository.class).buscarParaAtualizacao(fixture.avaliacaoId()).orElseThrow();
            bloqueado.countDown();
            aguardar(liberar);
            return null;
        }));
        try {
            assertThat(bloqueado.await(10, TimeUnit.SECONDS)).isTrue();
            Future<Integer> resposta = executor.submit(() -> mvc.perform(post(
                            "/api/diario/avaliacoes/" + fixture.avaliacaoId() + "/notas")
                            .with(user(fixture.semAtribuicao())).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"alunoId\":" + fixture.alunoId() + ",\"valor\":8}"))
                    .andReturn().getResponse().getStatus());
            assertThat(resposta.get(5, TimeUnit.SECONDS)).isEqualTo(403);
            assertThat(holder.isDone()).isFalse();
            assertThat(context.getBean(NotaRepository.class).count()).isZero();
        } finally {
            encerrar(executor, liberar, holder);
        }
    }

    @Test
    void notaConcorrenteUsaEstadoAtualMesmoComSnapshotAnteriorEmRepeatableRead() throws Exception {
        DiarioService diario = context.getBean(DiarioService.class);
        CountDownLatch inseridaSemCommit = new CountDownLatch(1);
        CountDownLatch snapshotPronto = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<NotaResponse> primeira = executor.submit(() -> transacao().execute(status -> {
            NotaResponse nota = diario.lancarNota(fixture.avaliacaoId(),
                    new NotaRequest(fixture.alunoId(), BigDecimal.valueOf(8)), fixture.professor());
            inseridaSemCommit.countDown();
            aguardar(liberar);
            return nota;
        }));
        try {
            assertThat(inseridaSemCommit.await(10, TimeUnit.SECONDS)).isTrue();
            Future<NotaResponse> segunda = executor.submit(() -> transacao().execute(status -> {
                assertThat(new JdbcTemplate(context.getBean(DataSource.class))
                        .queryForObject("SELECT @@transaction_isolation", String.class)).isEqualTo("REPEATABLE-READ");
                // Estabelece uma visão que ainda não inclui a nota inserida pela outra transação.
                context.getBean(AvaliacaoRepository.class).findById(fixture.avaliacaoId()).orElseThrow();
                snapshotPronto.countDown();
                return diario.lancarNota(fixture.avaliacaoId(),
                        new NotaRequest(fixture.alunoId(), BigDecimal.valueOf(9)), fixture.professor());
            }));
            assertThat(snapshotPronto.await(10, TimeUnit.SECONDS)).isTrue();
            liberar.countDown();
            NotaResponse inicial = primeira.get(10, TimeUnit.SECONDS);
            NotaResponse atualizada = segunda.get(10, TimeUnit.SECONDS);
            assertThat(atualizada.id()).isEqualTo(inicial.id());
            NotaRepository notas = context.getBean(NotaRepository.class);
            assertThat(notas.count()).isEqualTo(1);
            assertThat(notas.findAll().get(0).getValor()).isEqualByComparingTo("9");
        } finally {
            encerrar(executor, liberar, primeira);
        }
    }

    private TransactionTemplate transacao() {
        return new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
    }

    private Professor professor(String nome) {
        return context.getBean(ProfessorRepository.class).saveAndFlush(Professor.builder().nome(nome)
                .usuario(usuario(nome, Role.ROLE_PROFESSOR)).build());
    }

    private Usuario usuario(String nome, Role role) {
        return Usuario.builder().email(nome + "@mysql.fixture").role(role).senhaHash("fixture-sem-login").build();
    }

    private void aguardar(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout de sincronização do teste");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private void encerrar(ExecutorService executor, CountDownLatch liberar, Future<?> holder) throws Exception {
        liberar.countDown();
        executor.shutdown();
        try { holder.get(10, TimeUnit.SECONDS); }
        finally { if (!executor.awaitTermination(10, TimeUnit.SECONDS)) executor.shutdownNow(); }
    }

    private record Fixture(Long avaliacaoId, Long alunoId, UsuarioDetails professor, UsuarioDetails semAtribuicao) {}
}
