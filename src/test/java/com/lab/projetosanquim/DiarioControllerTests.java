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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:diario;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "sanquim.demo.seed=false",
        "SANQUIM_JWT_SECRET=" + ProjetoSanquimApplicationTests.CHAVE_TESTE
})
@AutoConfigureMockMvc
class DiarioControllerTests {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ProfessorRepository professores;
    @Autowired private AlunoRepository alunos;
    @Autowired private GestorRepository gestores;
    @Autowired private TurmaRepository turmas;
    @Autowired private DisciplinaRepository disciplinas;
    @Autowired private MatriculaRepository matriculas;
    @Autowired private AtribuicaoDocenteRepository atribuicoes;
    @Autowired private ChamadaRepository chamadas;
    @Autowired private FrequenciaRepository frequencias;
    @Autowired private AvaliacaoRepository avaliacoes;
    @Autowired private NotaRepository notas;
    @Autowired private PlanoAulaRepository planos;

    private UsuarioDetails professor;
    private UsuarioDetails outroProfessor;
    private UsuarioDetails gestor;
    private UsuarioDetails alunoLogado;
    private Turma turma;
    private Turma outraTurma;
    private Disciplina disciplina;
    private Aluno aluno;
    private Aluno segundoAluno;
    private Aluno alunoFora;

    @BeforeEach
    void preparar() {
        notas.deleteAllInBatch();
        frequencias.deleteAllInBatch();
        planos.deleteAllInBatch();
        chamadas.deleteAllInBatch();
        avaliacoes.deleteAllInBatch();
        matriculas.deleteAllInBatch();
        atribuicoes.deleteAllInBatch();
        professores.deleteAllInBatch();
        alunos.deleteAllInBatch();
        gestores.deleteAllInBatch();
        usuarios.deleteAllInBatch();
        Professor primeiro = professores.saveAndFlush(Professor.builder()
                .nome("Professor").usuario(usuario("professor@fixture.br", Role.ROLE_PROFESSOR)).build());
        Professor segundo = professores.saveAndFlush(Professor.builder()
                .nome("Outro professor").usuario(usuario("outro@fixture.br", Role.ROLE_PROFESSOR)).build());
        professor = new UsuarioDetails(primeiro.getUsuario());
        outroProfessor = new UsuarioDetails(segundo.getUsuario());
        Gestor gestao = gestores.saveAndFlush(Gestor.builder().nome("Gestão").cargo("Secretaria")
                .usuario(usuario("gestao@fixture.br", Role.ROLE_GESTAO)).build());
        gestor = new UsuarioDetails(gestao.getUsuario());
        aluno = novoAluno("aluno@fixture.br");
        segundoAluno = novoAluno("segundo@fixture.br");
        alunoFora = novoAluno("fora@fixture.br");
        alunoLogado = new UsuarioDetails(aluno.getUsuario());
        turma = turmas.saveAndFlush(Turma.builder().nome("Turma A").anoLetivo(2026).ativa(true).build());
        outraTurma = turmas.saveAndFlush(Turma.builder().nome("Turma B").anoLetivo(2026).ativa(true).build());
        disciplina = disciplinas.saveAndFlush(Disciplina.builder().nome("Matemática").build());
        atribuicoes.saveAndFlush(AtribuicaoDocente.builder()
                .professor(primeiro).turma(turma).disciplina(disciplina).build());
        atribuicoes.saveAndFlush(AtribuicaoDocente.builder()
                .professor(segundo).turma(outraTurma).disciplina(disciplina).build());
        matricular(aluno, turma);
        matricular(segundoAluno, turma);
        matricular(alunoFora, outraTurma);
    }

    @Test
    void minhasTurmasMostraSomenteAtribuicoesDoProfessor() throws Exception {
        mvc.perform(get("/api/diario/minhas-turmas").with(user(professor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].turmaId").value(turma.getId()))
                .andExpect(jsonPath("$[0].disciplinaId").value(disciplina.getId()));
        mvc.perform(get("/api/diario/minhas-turmas").with(user(outroProfessor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].turmaId").value(outraTurma.getId()));
        mvc.perform(get("/api/diario/minhas-turmas").with(user(gestor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void chamadaGravaLoteEBloqueiaDuplicidade() throws Exception {
        JsonNode response = enviar("/chamada", chamada(), professor, 201);
        assertThat(response.get("registros").size()).isEqualTo(2);
        assertThat(chamadas.count()).isEqualTo(1);
        assertThat(frequencias.count()).isEqualTo(2);
        assertThat(frequencias.findAll()).extracting(Frequencia::isPresente).containsExactlyInAnyOrder(true, false);
        enviar("/chamada", chamada(), professor, 409);
        assertThat(chamadas.count()).isEqualTo(1);
        assertThat(frequencias.count()).isEqualTo(2);
    }

    @Test
    void chamadaInvalidaNaoGravaParcialmente() throws Exception {
        Map<String, Object> request = chamada();
        request.put("registros", List.of(presenca(aluno.getId(), true), presenca(alunoFora.getId(), false)));
        enviar("/chamada", request, professor, 409);
        request.put("registros", List.of(presenca(aluno.getId(), true), presenca(Long.MAX_VALUE, false)));
        enviar("/chamada", request, professor, 404);
        request.put("registros", List.of(presenca(aluno.getId(), true), presenca(aluno.getId(), false)));
        enviar("/chamada", request, professor, 409);
        assertThat(chamadas.count()).isZero();
        assertThat(frequencias.count()).isZero();
    }

    @Test
    void professorNaoPodeLancarEmTurmaSemAtribuicao() throws Exception {
        long avaliacaoId = enviar("/avaliacoes", avaliacao(), gestor, 201).get("id").asLong();
        enviar("/chamada", chamada(), outroProfessor, 403);
        enviar("/avaliacoes", avaliacao(), outroProfessor, 403);
        enviar("/avaliacoes/" + avaliacaoId + "/notas", nota(aluno.getId(), 8), outroProfessor, 403);
        enviar("/planos-aula", plano(), outroProfessor, 403);
        assertThat(chamadas.count()).isZero();
        assertThat(notas.count()).isZero();
        assertThat(planos.count()).isZero();
    }

    @Test
    void alunoESemAutenticacaoNaoAcessamDiario() throws Exception {
        mvc.perform(get("/api/diario/minhas-turmas").with(user(alunoLogado))).andExpect(status().isForbidden());
        mvc.perform(get("/api/diario/minhas-turmas")).andExpect(status().isUnauthorized());
        for (String path : List.of("/chamada", "/avaliacoes", "/avaliacoes/1/notas", "/planos-aula")) {
            enviar(path, Map.of(), alunoLogado, 403);
            mvc.perform(post("/api/diario" + path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void notaZeroEAtualizacaoUsamMesmoRegistro() throws Exception {
        long avaliacaoId = enviar("/avaliacoes", avaliacao(), professor, 201).get("id").asLong();
        String path = "/avaliacoes/" + avaliacaoId + "/notas";
        long notaId = enviar(path, nota(aluno.getId(), 0), professor, 200).get("id").asLong();
        JsonNode atualizada = enviar(path, nota(aluno.getId(), 8), professor, 200);
        assertThat(atualizada.get("id").asLong()).isEqualTo(notaId);
        assertThat(atualizada.get("valor").decimalValue()).isEqualByComparingTo("8");
        assertThat(notas.count()).isEqualTo(1);
        enviar(path, nota(aluno.getId(), 11), professor, 409);
        enviar(path, nota(aluno.getId(), -1), professor, 400);
        enviar(path, nota(alunoFora.getId(), 8), professor, 409);
        enviar(path, nota(Long.MAX_VALUE, 8), professor, 404);
        assertThat(notas.findById(notaId).orElseThrow().getValor()).isEqualByComparingTo("8");
    }

    @Test
    void validacaoDeCamposEPlanoDeAula() throws Exception {
        Map<String, Object> request = chamada();
        request.put("registros", List.of(Map.of("alunoId", aluno.getId())));
        JsonNode erro = enviar("/chamada", request, professor, 400);
        assertThat(erro.get("camposInvalidos").toString()).contains("presente");
        Map<String, Object> invalida = avaliacao();
        invalida.put("peso", 0);
        enviar("/avaliacoes", invalida, professor, 400);
        invalida.put("peso", 1);
        invalida.put("notaMaxima", 0);
        enviar("/avaliacoes", invalida, professor, 400);
        JsonNode aula = enviar("/planos-aula", plano(), professor, 201);
        assertThat(aula.get("topicos").asString()).isEqualTo("Equações do primeiro grau");
        assertThat(planos.count()).isEqualTo(1);
    }

    @Test
    void turmaInativaEVinculosInexistentesSaoRejeitados() throws Exception {
        JsonNode erro = enviar("/avaliacoes/invalido/notas", nota(aluno.getId(), 8), professor, 400);
        assertThat(erro.get("camposInvalidos").get(0).get("campo").asString()).isEqualTo("avaliacaoId");
        enviar("/avaliacoes/" + Long.MAX_VALUE + "/notas", nota(aluno.getId(), 8), professor, 404);
        Map<String, Object> request = avaliacao();
        request.put("turmaId", Long.MAX_VALUE);
        enviar("/avaliacoes", request, gestor, 404);
        request.put("turmaId", turma.getId());
        request.put("disciplinaId", Long.MAX_VALUE);
        enviar("/avaliacoes", request, gestor, 404);
        turma.setAtiva(false);
        turmas.saveAndFlush(turma);
        enviar("/chamada", chamada(), professor, 409);
        enviar("/avaliacoes", avaliacao(), professor, 409);
        enviar("/planos-aula", plano(), professor, 409);
        assertThat(chamadas.count()).isZero();
    }

    @Test
    void matriculaInativaNaoPermitePresencaNemNota() throws Exception {
        Matricula matricula = matriculas.findAll().stream()
                .filter(item -> item.getAluno().getId().equals(aluno.getId())).findFirst().orElseThrow();
        matricula.setAtiva(false);
        matriculas.saveAndFlush(matricula);
        enviar("/chamada", chamada(), professor, 409);
        long id = enviar("/avaliacoes", avaliacao(), professor, 201).get("id").asLong();
        enviar("/avaliacoes/" + id + "/notas", nota(aluno.getId(), 8), professor, 409);
    }

    @Test
    void chamadasConcorrentesNaoDuplicamRegistros() throws Exception {
        List<Integer> statuses = simultaneas(() -> enviarSemVerificar("/chamada", chamada()));
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(chamadas.count()).isEqualTo(1);
        assertThat(frequencias.count()).isEqualTo(2);
    }

    @Test
    void notasConcorrentesNaoCriamDuasLinhas() throws Exception {
        long id = enviar("/avaliacoes", avaliacao(), professor, 201).get("id").asLong();
        List<Integer> statuses = simultaneas(
                () -> enviarSemVerificar("/avaliacoes/" + id + "/notas", nota(aluno.getId(), 8)));
        assertThat(statuses).containsExactly(200, 200);
        assertThat(notas.count()).isEqualTo(1);
    }

    private List<Integer> simultaneas(Callable<Integer> action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            Callable<Integer> tentativa = () -> {
                if (!iniciar.await(10, TimeUnit.SECONDS)) { throw new IllegalStateException("Timeout do teste"); }
                return action.call();
            };
            Future<Integer> primeira = executor.submit(tentativa);
            Future<Integer> segunda = executor.submit(tentativa);
            iniciar.countDown();
            return List.of(primeira.get(10, TimeUnit.SECONDS), segunda.get(10, TimeUnit.SECONDS));
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
        }
    }

    private JsonNode enviar(String path, Object body, UsuarioDetails principal, int esperado) throws Exception {
        var result = mvc.perform(post("/api/diario" + path).with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().is(esperado)).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private int enviarSemVerificar(String path, Object body) throws Exception {
        return mvc.perform(post("/api/diario" + path).with(user(professor))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andReturn().getResponse().getStatus();
    }

    private Usuario usuario(String email, Role role) {
        return Usuario.builder().email(email).role(role).senhaHash("fixture-de-teste-sem-login").build();
    }

    private Aluno novoAluno(String email) {
        return alunos.saveAndFlush(Aluno.builder().nome("Aluno").usuario(usuario(email, Role.ROLE_ALUNO)).build());
    }

    private void matricular(Aluno registro, Turma destino) {
        matriculas.saveAndFlush(Matricula.builder().aluno(registro).turma(destino).ativa(true).build());
    }

    private Map<String, Object> chamada() {
        var request = base();
        request.put("dataAula", "2026-11-10");
        request.put("registros", List.of(presenca(aluno.getId(), true), presenca(segundoAluno.getId(), false)));
        return request;
    }

    private Map<String, Object> avaliacao() {
        var request = base();
        request.putAll(Map.of("titulo", "Prova 1", "tipo", "PROVA", "notaMaxima", 10,
                "peso", 1, "dataEntrega", "2026-11-10"));
        return request;
    }

    private Map<String, Object> plano() {
        var request = base();
        request.putAll(Map.of("dataAula", "2026-11-10", "topicos", "Equações do primeiro grau"));
        return request;
    }

    private Map<String, Object> base() {
        return new HashMap<>(Map.of("turmaId", turma.getId(), "disciplinaId", disciplina.getId()));
    }

    private Map<String, Object> presenca(Long id, boolean presente) {
        return Map.of("alunoId", id, "presente", presente);
    }

    private Map<String, Object> nota(Long id, int valor) {
        return Map.of("alunoId", id, "valor", valor);
    }
}
