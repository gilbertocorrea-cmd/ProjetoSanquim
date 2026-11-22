package com.lab.projetosanquim;

import com.lab.projetosanquim.entity.*;
import com.lab.projetosanquim.repository.*;
import com.lab.projetosanquim.security.UsuarioDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:consulta-aluno;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "sanquim.demo.seed=false",
        "SANQUIM_JWT_SECRET=" + ProjetoSanquimApplicationTests.CHAVE_TESTE
})
@AutoConfigureMockMvc
@Transactional
class AlunoControllerTests {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private AlunoRepository alunos;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private TurmaRepository turmas;
    @Autowired private DisciplinaRepository disciplinas;
    @Autowired private MatriculaRepository matriculas;
    @Autowired private AvaliacaoRepository avaliacoes;
    @Autowired private NotaRepository notas;
    @Autowired private ChamadaRepository chamadas;
    @Autowired private FrequenciaRepository frequencias;
    @Autowired private PlanoAulaRepository planos;

    private UsuarioDetails alunoA;
    private UsuarioDetails alunoB;
    private UsuarioDetails alunoVazio;
    private UsuarioDetails semPerfil;
    private UsuarioDetails professor;
    private UsuarioDetails gestor;
    private Aluno perfilA;
    private Aluno perfilB;
    private Turma turmaA;
    private Turma turmaB;
    private Turma turmaAntiga;
    private Disciplina matematica;
    private Disciplina historia;
    private Avaliacao provaPendente;
    private Avaliacao provaZero;
    private Long ultimoPlanoHoje;

    @BeforeEach
    void preparar() {
        professor = new UsuarioDetails(usuarios.saveAndFlush(usuario("professor@fixture.br", Role.ROLE_PROFESSOR)));
        gestor = new UsuarioDetails(usuarios.saveAndFlush(usuario("gestor@fixture.br", Role.ROLE_GESTAO)));
        perfilA = aluno("a@fixture.br");
        perfilB = aluno("b@fixture.br");
        alunoA = new UsuarioDetails(perfilA.getUsuario());
        alunoB = new UsuarioDetails(perfilB.getUsuario());
        alunoVazio = new UsuarioDetails(aluno("vazio@fixture.br").getUsuario());
        semPerfil = new UsuarioDetails(usuarios.saveAndFlush(usuario("sem-perfil@fixture.br", Role.ROLE_ALUNO)));
        turmaA = turma("Turma A", 2026);
        turmaB = turma("Turma B", 2026);
        turmaAntiga = turma("Turma antiga", 2025);
        matematica = disciplinas.saveAndFlush(Disciplina.builder().nome("Matemática").build());
        historia = disciplinas.saveAndFlush(Disciplina.builder().nome("História").build());
        matricular(perfilA, turmaA, true);
        matricular(perfilB, turmaA, true);
        matricular(perfilB, turmaB, true);
        matricular(perfilA, turmaAntiga, false);

        Avaliacao primeira = avaliacao(turmaA, matematica, "Prova 10", "10", "2");
        Avaliacao segunda = avaliacao(turmaA, matematica, "Prova 20", "20", "1");
        provaZero = avaliacao(turmaA, matematica, "Prova zero", "10", "1");
        provaPendente = avaliacao(turmaA, matematica, "Pendente", "10", "100");
        nota(perfilA, primeira, "8");
        nota(perfilA, segunda, "15");
        nota(perfilA, provaZero, "0");
        nota(perfilB, primeira, "10");
        nota(perfilB, segunda, "20");
        nota(perfilB, provaZero, "10");
        nota(perfilB, provaPendente, "10");
        avaliacao(turmaA, historia, "História pendente", "10", "1");
        nota(perfilA, avaliacao(turmaAntiga, matematica, "Histórica", "10", "1"), "7");
        avaliacao(turmaAntiga, matematica, "Sem nota na turma antiga", "10", "1");
        nota(perfilB, avaliacao(turmaB, matematica, "Só do B", "10", "1"), "3");

        LocalDate hoje = LocalDate.now();
        Chamada primeiraChamada = chamada(turmaA, matematica, hoje.minusDays(3));
        frequencia(perfilA, primeiraChamada, true);
        frequencia(perfilB, primeiraChamada, false);
        Chamada segundaChamada = chamada(turmaA, matematica, hoje.minusDays(2));
        frequencia(perfilA, segundaChamada, false);
        frequencia(perfilB, segundaChamada, true);
        frequencia(perfilB, chamada(turmaA, matematica, hoje.minusDays(1)), true);
        Chamada chamadaHistoria = chamada(turmaA, historia, hoje.minusDays(3));
        frequencia(perfilA, chamadaHistoria, true);
        frequencia(perfilB, chamadaHistoria, false);
        frequencia(perfilA, chamada(turmaAntiga, matematica, hoje.minusYears(1)), false);
        frequencia(perfilB, chamada(turmaB, matematica, hoje.minusDays(3)), true);
        frequencia(perfilB, chamada(turmaB, matematica, hoje.minusDays(2)), true);
        plano(turmaA, hoje.minusDays(3), "Conteúdo anterior");
        plano(turmaA, hoje, "Conteúdo de hoje 1");
        ultimoPlanoHoje = plano(turmaA, hoje, "Conteúdo de hoje 2").getId();
        plano(turmaA, hoje.plusDays(1), "Aula futura");
        plano(turmaB, hoje.minusDays(1), "Conteúdo exclusivo do B");
        plano(turmaAntiga, hoje.minusYears(1), "Conteúdo da matrícula inativa");
    }

    @Test
    void boletimNormalizaPonderaEDistingueZeroDeNotaNaoLancada() throws Exception {
        JsonNode boletim = consultar("/boletim", alunoA);
        assertThat(boletim.get("alunoId").asLong()).isEqualTo(perfilA.getId());
        JsonNode matematicaAtual = grupo(boletim, turmaA.getId(), matematica.getId());
        assertThat(matematicaAtual.get("mediaParcial").decimalValue()).isEqualByComparingTo("5.8750");
        assertThat(matematicaAtual.get("avaliacoes").size()).isEqualTo(4);
        JsonNode zero = porId(matematicaAtual.get("avaliacoes"), "avaliacaoId", provaZero.getId());
        assertThat(zero.get("valor").decimalValue()).isEqualByComparingTo("0");
        assertThat(zero.get("situacao").asString()).isEqualTo("LANCADA");
        JsonNode pendente = porId(matematicaAtual.get("avaliacoes"), "avaliacaoId", provaPendente.getId());
        assertThat(pendente.get("valor").isNull()).isTrue();
        assertThat(pendente.get("notaNormalizada").isNull()).isTrue();
        assertThat(pendente.get("situacao").asString()).isEqualTo("NAO_LANCADA");
        assertThat(grupo(boletim, turmaA.getId(), historia.getId()).get("mediaParcial").isNull()).isTrue();
        assertThat(grupo(boletim, turmaAntiga.getId(), matematica.getId()).get("mediaParcial").decimalValue())
                .isEqualByComparingTo("7");
        assertThat(grupo(boletim, turmaAntiga.getId(), matematica.getId()).get("avaliacoes").size()).isEqualTo(1);
        assertThat(boletim.toString()).doesNotContain("Só do B", "senhaHash", "b@fixture.br");
        JsonNode boletimB = consultar("/boletim", alunoB);
        assertThat(grupo(boletimB, turmaA.getId(), matematica.getId()).get("mediaParcial").decimalValue())
                .isEqualByComparingTo("10");
    }

    @Test
    void frequenciaContaSomenteRegistrosDoAlunoESeparaTurmas() throws Exception {
        JsonNode response = consultar("/frequencia", alunoA);
        assertThat(response.get("alunoId").asLong()).isEqualTo(perfilA.getId());
        assertThat(response.get("aulasRegistradas").asLong()).isEqualTo(4);
        assertThat(response.get("presencas").asLong()).isEqualTo(2);
        assertThat(response.get("faltas").asLong()).isEqualTo(2);
        assertThat(response.get("percentualGlobal").decimalValue()).isEqualByComparingTo("50");
        JsonNode atual = grupo(response, turmaA.getId(), matematica.getId());
        assertThat(atual.get("aulasRegistradas").asLong()).isEqualTo(2);
        assertThat(atual.get("faltas").asLong()).isEqualTo(1);
        assertThat(atual.get("percentualPresenca").decimalValue()).isEqualByComparingTo("50");
        assertThat(grupo(response, turmaAntiga.getId(), matematica.getId()).get("faltas").asLong()).isEqualTo(1);
        assertThat(response.get("disciplinas").size()).isEqualTo(3);
        assertThat(response.toString()).doesNotContain("Turma B");
        assertThat(consultar("/frequencia", alunoB).get("percentualGlobal").decimalValue())
                .isEqualByComparingTo("66.6667");
    }

    @Test
    void mediaUsaPrecisaoIntermediariaEArredondaSomenteOResultado() throws Exception {
        Disciplina disciplina = disciplinas.saveAndFlush(Disciplina.builder().nome("Arredondamento").build());
        nota(perfilA, avaliacao(turmaA, disciplina, "Primeira", "20", "1"), "2.4689");
        nota(perfilA, avaliacao(turmaA, disciplina, "Segunda", "20", "1"), "2.4687");
        JsonNode grupo = grupo(consultar("/boletim", alunoA), turmaA.getId(), disciplina.getId());
        assertThat(grupo.get("avaliacoes").get(0).get("notaNormalizada").decimalValue())
                .isEqualByComparingTo("1.2345");
        assertThat(grupo.get("avaliacoes").get(1).get("notaNormalizada").decimalValue())
                .isEqualByComparingTo("1.2344");
        assertThat(grupo.get("mediaParcial").decimalValue()).isEqualByComparingTo("1.2344");
    }

    @Test
    void aulasUsamMatriculasAtivasExcluemFuturasEOrdenamDatasEIds() throws Exception {
        JsonNode response = consultar("/aulas", alunoA);
        assertThat(response.size()).isEqualTo(3);
        assertThat(response.get(0).get("id").asLong()).isEqualTo(ultimoPlanoHoje);
        assertThat(response.get(0).get("dataAula").asString()).isEqualTo(LocalDate.now().toString());
        assertThat(response.toString()).doesNotContain("Aula futura", "exclusivo do B", "matrícula inativa");
    }

    @Test
    void consultaVaziaNaoInventaNotaFrequenciaOuFalta() throws Exception {
        assertThat(consultar("/boletim", alunoVazio).get("disciplinas").isEmpty()).isTrue();
        JsonNode frequencia = consultar("/frequencia", alunoVazio);
        assertThat(frequencia.get("aulasRegistradas").asLong()).isZero();
        assertThat(frequencia.get("faltas").asLong()).isZero();
        assertThat(frequencia.get("percentualGlobal").isNull()).isTrue();
        assertThat(consultar("/aulas", alunoVazio).isEmpty()).isTrue();
    }

    @Test
    void idForjadoNaoMudaOAlunoDoToken() throws Exception {
        for (String path : List.of("/boletim", "/frequencia")) {
            mvc.perform(get("/api/aluno" + path).with(user(alunoA)).param("alunoId", perfilB.getId().toString()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.alunoId").value(perfilA.getId()));
        }
        mvc.perform(get("/api/aluno/aulas").with(user(alunoA)).param("alunoId", perfilB.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/aluno/boletim/" + perfilB.getId()).with(user(alunoA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void soAlunoAutenticadoAcessaConsultasESemPerfilRecebe404() throws Exception {
        for (String path : List.of("/boletim", "/frequencia", "/aulas")) {
            mvc.perform(get("/api/aluno" + path)).andExpect(status().isUnauthorized());
            for (UsuarioDetails principal : List.of(professor, gestor)) {
                mvc.perform(get("/api/aluno" + path).with(user(principal)))
                        .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
            }
            mvc.perform(get("/api/aluno" + path).with(user(semPerfil)))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
            mvc.perform(post("/api/aluno" + path).with(user(alunoA))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    private JsonNode consultar(String path, UsuarioDetails principal) throws Exception {
        var result = mvc.perform(get("/api/aluno" + path).with(user(principal)))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode grupo(JsonNode response, Long turmaId, Long disciplinaId) {
        for (JsonNode grupo : response.get("disciplinas")) {
            if (grupo.get("turmaId").asLong() == turmaId && grupo.get("disciplinaId").asLong() == disciplinaId) {
                return grupo;
            }
        }
        throw new AssertionError("Grupo de turma e disciplina ausente");
    }

    private JsonNode porId(JsonNode registros, String campo, Long id) {
        for (JsonNode registro : registros) {
            if (registro.get(campo).asLong() == id) { return registro; }
        }
        throw new AssertionError("Registro ausente");
    }

    private Aluno aluno(String email) {
        return alunos.saveAndFlush(Aluno.builder().nome(email).usuario(usuario(email, Role.ROLE_ALUNO)).build());
    }

    private Usuario usuario(String email, Role role) {
        return Usuario.builder().email(email).role(role).senhaHash("fixture-sem-autenticacao").build();
    }

    private Turma turma(String nome, int ano) {
        return turmas.saveAndFlush(Turma.builder().nome(nome).anoLetivo(ano).ativa(true).build());
    }

    private void matricular(Aluno aluno, Turma turma, boolean ativa) {
        matriculas.saveAndFlush(Matricula.builder().aluno(aluno).turma(turma).ativa(ativa).build());
    }

    private Avaliacao avaliacao(Turma turma, Disciplina disciplina, String titulo, String maxima, String peso) {
        return avaliacoes.saveAndFlush(Avaliacao.builder().turma(turma).disciplina(disciplina)
                .titulo(titulo).tipo("PROVA").notaMaxima(new BigDecimal(maxima)).peso(new BigDecimal(peso))
                .dataEntrega(LocalDate.now()).build());
    }

    private void nota(Aluno aluno, Avaliacao avaliacao, String valor) {
        notas.saveAndFlush(Nota.builder().aluno(aluno).avaliacao(avaliacao).valor(new BigDecimal(valor)).build());
    }

    private Chamada chamada(Turma turma, Disciplina disciplina, LocalDate data) {
        return chamadas.saveAndFlush(Chamada.builder().turma(turma).disciplina(disciplina).dataAula(data).build());
    }

    private void frequencia(Aluno aluno, Chamada chamada, boolean presente) {
        frequencias.saveAndFlush(Frequencia.builder().aluno(aluno).chamada(chamada).presente(presente).build());
    }

    private PlanoAula plano(Turma turma, LocalDate data, String topicos) {
        return planos.saveAndFlush(PlanoAula.builder().turma(turma).disciplina(matematica)
                .dataAula(data).topicos(topicos).build());
    }
}
