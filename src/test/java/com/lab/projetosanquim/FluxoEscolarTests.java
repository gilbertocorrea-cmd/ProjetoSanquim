package com.lab.projetosanquim;

import com.lab.projetosanquim.entity.Gestor;
import com.lab.projetosanquim.entity.Role;
import com.lab.projetosanquim.entity.Usuario;
import com.lab.projetosanquim.repository.GestorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.Clock;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fluxo-escolar;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "sanquim.demo.seed=false",
        "SANQUIM_JWT_SECRET=" + ProjetoSanquimApplicationTests.CHAVE_TESTE
})
@AutoConfigureMockMvc
class FluxoEscolarTests {
    private static final String SENHA_TESTE = "SenhaFixture123!";

    @Autowired private MockMvc mvc;
    @Autowired private Clock clock;
    @Autowired private ObjectMapper mapper;
    @Autowired private GestorRepository gestores;
    @Autowired private PasswordEncoder encoder;

    @Test
    void gestaoCadastraProfessorLancaEAlunoConsultaComJwtReal() throws Exception {
        Usuario usuario = Usuario.builder().email("gestao@fluxo.test")
                .senhaHash(encoder.encode(SENHA_TESTE)).role(Role.ROLE_GESTAO).build();
        gestores.saveAndFlush(Gestor.builder().nome("Gestor do fluxo").cargo("Secretaria")
                .usuario(usuario).build());
        String gestor = login("gestao@fluxo.test");

        long professorId = cadastrarPessoa("professores", "professor@fluxo.test", gestor);
        long alunoId = cadastrarPessoa("alunos", "aluno@fluxo.test", gestor);
        cadastrarPessoa("alunos", "outro@fluxo.test", gestor);
        long turmaId = enviar("/api/admin/turmas", Map.of("nome", "Turma do fluxo",
                "anoLetivo", 2026, "ativa", true), gestor, 201).get("id").asLong();
        long disciplinaId = enviar("/api/admin/disciplinas", Map.of("nome", "Matemática do fluxo"),
                gestor, 201).get("id").asLong();
        enviar("/api/admin/matriculas", Map.of("alunoId", alunoId, "turmaId", turmaId), gestor, 201);
        enviar("/api/admin/atribuicoes", Map.of("professorId", professorId,
                "turmaId", turmaId, "disciplinaId", disciplinaId), gestor, 201);

        String professor = login("professor@fluxo.test");
        mvc.perform(get("/api/diario/minhas-turmas").header("Authorization", "Bearer " + professor))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].turmaId").value(turmaId))
                .andExpect(jsonPath("$[0].disciplinaId").value(disciplinaId));
        String hoje = LocalDate.now(clock).toString();
        Map<String, Object> chamada = Map.of("turmaId", turmaId, "disciplinaId", disciplinaId,
                "dataAula", hoje, "registros", java.util.List.of(Map.of("alunoId", alunoId, "presente", false)));
        enviar("/api/diario/chamada", chamada, professor, 201);
        enviar("/api/diario/chamada", chamada, professor, 409);
        enviar("/api/diario/planos-aula", Map.of("turmaId", turmaId, "disciplinaId", disciplinaId,
                "dataAula", hoje, "topicos", "Equações do primeiro grau"), professor, 201);
        long prova = avaliacao(turmaId, disciplinaId, 20, 2, professor);
        long trabalho = avaliacao(turmaId, disciplinaId, 10, 1, professor);
        avaliacao(turmaId, disciplinaId, 10, 100, professor);
        enviar("/api/diario/avaliacoes/" + prova + "/notas", Map.of("alunoId", alunoId, "valor", 12), professor, 200);
        enviar("/api/diario/avaliacoes/" + trabalho + "/notas", Map.of("alunoId", alunoId, "valor", 10), professor, 200);

        String aluno = login("aluno@fluxo.test");
        mvc.perform(get("/api/aluno/boletim").header("Authorization", "Bearer " + aluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$.alunoId").value(alunoId))
                .andExpect(jsonPath("$.disciplinas[0].avaliacoes.length()").value(3))
                .andExpect(jsonPath("$.disciplinas[0].mediaParcial").value(7.3333));
        enviar("/api/diario/avaliacoes/" + prova + "/notas", Map.of("alunoId", alunoId, "valor", 16), professor, 200);
        mvc.perform(get("/api/aluno/boletim").header("Authorization", "Bearer " + aluno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disciplinas[0].avaliacoes.length()").value(3))
                .andExpect(jsonPath("$.disciplinas[0].mediaParcial").value(8.6667));
        mvc.perform(get("/api/aluno/frequencia").header("Authorization", "Bearer " + aluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$.aulasRegistradas").value(1))
                .andExpect(jsonPath("$.faltas").value(1)).andExpect(jsonPath("$.percentualGlobal").value(0));
        mvc.perform(get("/api/aluno/aulas").header("Authorization", "Bearer " + aluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].topicos").value("Equações do primeiro grau"));
        mvc.perform(get("/api/admin/alunos").header("Authorization", "Bearer " + aluno))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/aluno/boletim").header("Authorization", "Bearer " + professor))
                .andExpect(status().isForbidden());
        String outro = login("outro@fluxo.test");
        mvc.perform(get("/api/aluno/boletim").header("Authorization", "Bearer " + outro))
                .andExpect(status().isOk()).andExpect(jsonPath("$.disciplinas").isEmpty());
        mvc.perform(get("/api/aluno/boletim")).andExpect(status().isUnauthorized());
    }

    private long cadastrarPessoa(String recurso, String email, String token) throws Exception {
        return enviar("/api/admin/" + recurso,
                Map.of("nome", "Pessoa do fluxo", "email", email, "senha", SENHA_TESTE), token, 201)
                .get("id").asLong();
    }

    private long avaliacao(long turma, long disciplina, int maxima, int peso, String token) throws Exception {
        return enviar("/api/diario/avaliacoes", Map.of("turmaId", turma, "disciplinaId", disciplina,
                "titulo", "Atividade do fluxo", "tipo", "PROVA", "notaMaxima", maxima,
                "peso", peso, "dataEntrega", LocalDate.now().toString()), token, 201).get("id").asLong();
    }

    private String login(String email) throws Exception {
        return enviar("/api/auth/login", Map.of("email", email, "senha", SENHA_TESTE), null, 200)
                .get("token").asText();
    }

    private JsonNode enviar(String rota, Object corpo, String token, int esperado) throws Exception {
        var request = post(rota).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(corpo));
        if (token != null) request.header("Authorization", "Bearer " + token);
        String resposta = mvc.perform(request).andExpect(status().is(esperado))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return mapper.readTree(resposta);
    }
}
