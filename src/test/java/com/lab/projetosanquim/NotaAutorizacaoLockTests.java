package com.lab.projetosanquim;

import com.lab.projetosanquim.entity.*;
import com.lab.projetosanquim.repository.*;
import com.lab.projetosanquim.security.UsuarioDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:nota-autorizacao-lock;DB_CLOSE_DELAY=-1;MODE=MySQL;LOCK_TIMEOUT=20000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false", "sanquim.demo.seed=false",
        "SANQUIM_JWT_SECRET=" + ProjetoSanquimApplicationTests.CHAVE_TESTE
})
@AutoConfigureMockMvc
class NotaAutorizacaoLockTests {
    @Autowired private MockMvc mvc;
    @Autowired private ProfessorRepository professores;
    @Autowired private TurmaRepository turmas;
    @Autowired private DisciplinaRepository disciplinas;
    @Autowired private AvaliacaoRepository avaliacoes;
    @Autowired private NotaRepository notas;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void professorSemAtribuicaoRecebe403EnquantoOutraTransacaoMantemLock() throws Exception {
        Professor semVinculo = professores.saveAndFlush(Professor.builder().nome("Sem atribuição")
                .usuario(Usuario.builder().email("professor@lock.test").role(Role.ROLE_PROFESSOR)
                        .senhaHash("fixture-sem-login").build()).build());
        Turma turma = turmas.saveAndFlush(Turma.builder().nome("Lock").anoLetivo(2026).ativa(true).build());
        Disciplina disciplina = disciplinas.saveAndFlush(Disciplina.builder().nome("Matemática").build());
        long id = avaliacoes.saveAndFlush(Avaliacao.builder().turma(turma).disciplina(disciplina)
                .titulo("Avaliação bloqueada").tipo("PROVA").notaMaxima(BigDecimal.TEN)
                .peso(BigDecimal.ONE).dataEntrega(LocalDate.of(2026, 11, 10)).build()).getId();
        CountDownLatch bloqueioObtido = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> holder = executor.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
            avaliacoes.buscarParaAtualizacao(id).orElseThrow();
            bloqueioObtido.countDown();
            try {
                if (!liberar.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Lock não liberado pelo teste");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            return null;
        }));
        try {
            assertThat(bloqueioObtido.await(10, TimeUnit.SECONDS)).isTrue();
            Future<Integer> resposta = executor.submit(() -> mvc.perform(post("/api/diario/avaliacoes/" + id + "/notas")
                            .with(user(new UsuarioDetails(semVinculo.getUsuario())))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"alunoId\":1,\"valor\":8}"))
                    .andReturn().getResponse().getStatus());
            // O holder só libera no finally. A autorização precisa terminar antes disso.
            assertThat(resposta.get(5, TimeUnit.SECONDS)).isEqualTo(403);
            assertThat(holder.isDone()).isFalse();
            assertThat(notas.count()).isZero();
        } finally {
            liberar.countDown();
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) executor.shutdownNow();
            holder.get(5, TimeUnit.SECONDS);
        }
    }
}
