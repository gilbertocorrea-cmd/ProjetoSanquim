package com.lab.projetosanquim;

import com.lab.projetosanquim.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:gestao;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "sanquim.demo.seed=true",
        "SANQUIM_DEMO_PASSWORD=" + ProjetoSanquimApplicationTests.SENHA_TESTE,
        "SANQUIM_JWT_SECRET=" + ProjetoSanquimApplicationTests.CHAVE_TESTE
})
@AutoConfigureMockMvc
@Transactional
class AdminControllerTests {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PasswordEncoder encoder;

    @ParameterizedTest
    @CsvSource({
            "professores, ROLE_PROFESSOR",
            "alunos, ROLE_ALUNO",
            "gestores, ROLE_GESTAO"
    })
    void cadastroCriaLoginComSenhaProtegida(String recurso, String role) throws Exception {
        Map<String, String> request = Map.of(
                "nome", "Pessoa de teste",
                "email", "Novo@sanquim.br",
                "senha", "NovaSenha123!",
                "cargo", "Secretaria");
        JsonNode response = cadastrar(recurso, request);
        assertThat(response.get("role").asString()).isEqualTo(role);
        assertThat(response.has("senha")).isFalse();
        assertThat(response.has("senhaHash")).isFalse();
        assertThat(response.has("usuario")).isFalse();
        var usuario = usuarios.findByEmail("novo@sanquim.br").orElseThrow();
        assertThat(encoder.matches("NovaSenha123!", usuario.getSenhaHash())).isTrue();

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                Map.of("email", "NOVO@sanquim.br", "senha", "NovaSenha123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(role));

        mvc.perform(get("/api/admin/" + recurso).with(user("gestao").roles("GESTAO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'novo@sanquim.br')]").isNotEmpty())
                .andExpect(jsonPath("$[*].senhaHash").isEmpty());

        long quantidade = usuarios.count();
        mvc.perform(post("/api/admin/alunos").with(user("gestao").roles("GESTAO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        assertThat(usuarios.count()).isEqualTo(quantidade);
    }

    @Test
    void matriculaValidaAlunoTurmaAtivaEDuplicidade() throws Exception {
        long alunoId = cadastrar("alunos", Map.of(
                "nome", "Aluno", "email", "matricula@sanquim.br", "senha", "Teste123!")).get("id").asLong();
        long turmaId = cadastrar("turmas", Map.of(
                "nome", "1º Ano B", "anoLetivo", 2026, "ativa", true)).get("id").asLong();
        Map<String, Long> matricula = Map.of("alunoId", alunoId, "turmaId", turmaId);
        assertThat(cadastrar("matriculas", matricula).get("ativa").asBoolean()).isTrue();
        erroCadastro("matriculas", matricula, 409);

        long inativaId = cadastrar("turmas", Map.of(
                "nome", "Turma inativa", "anoLetivo", 2026, "ativa", false)).get("id").asLong();
        erroCadastro("matriculas", Map.of("alunoId", alunoId, "turmaId", inativaId), 409);
        erroCadastro("matriculas", Map.of("alunoId", Long.MAX_VALUE, "turmaId", turmaId), 404);
        erroCadastro("matriculas", Map.of("alunoId", alunoId, "turmaId", Long.MAX_VALUE), 404);
    }

    @Test
    void atribuicaoValidaVinculosEDuplicidade() throws Exception {
        long professorId = cadastrar("professores", Map.of(
                "nome", "Professor", "email", "docente@sanquim.br", "senha", "Teste123!")).get("id").asLong();
        long turmaId = cadastrar("turmas", Map.of(
                "nome", "2º Ano A", "anoLetivo", 2026, "ativa", true)).get("id").asLong();
        long disciplinaId = cadastrar("disciplinas", Map.of("nome", "Matemática")).get("id").asLong();
        Map<String, Long> atribuicao = Map.of(
                "professorId", professorId, "turmaId", turmaId, "disciplinaId", disciplinaId);
        assertThat(cadastrar("atribuicoes", atribuicao).get("professorId").asLong()).isEqualTo(professorId);
        erroCadastro("atribuicoes", atribuicao, 409);
        erroCadastro("atribuicoes", Map.of(
                "professorId", Long.MAX_VALUE, "turmaId", turmaId, "disciplinaId", disciplinaId), 404);
        erroCadastro("atribuicoes", Map.of(
                "professorId", professorId, "turmaId", turmaId, "disciplinaId", Long.MAX_VALUE), 404);
    }

    @Test
    void validaCamposAntesDeGravar() throws Exception {
        long quantidade = usuarios.count();
        mvc.perform(post("/api/admin/alunos").with(user("gestao").roles("GESTAO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"","email":"invalido","senha":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.camposInvalidos.length()").value(3));
        assertThat(usuarios.count()).isEqualTo(quantidade);
        erroCadastro("turmas", Map.of("nome", "1º Ano", "anoLetivo", 2026), 400);
        erroCadastro("matriculas", Map.of("alunoId", -1, "turmaId", 1), 400);
    }

    @Test
    void todasAsRotasExigemGestao() throws Exception {
        for (String recurso : new String[]{
                "gestores", "professores", "alunos", "turmas", "disciplinas", "matriculas", "atribuicoes"
        }) {
            mvc.perform(get("/api/admin/" + recurso)).andExpect(status().isUnauthorized());
            for (String role : new String[]{"PROFESSOR", "ALUNO"}) {
                mvc.perform(get("/api/admin/" + recurso).with(user("usuario").roles(role)))
                        .andExpect(status().isForbidden());
                mvc.perform(post("/api/admin/" + recurso).with(user("usuario").roles(role))
                                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                        .andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void swaggerDocumentaBearerNosCadastros() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/admin/alunos'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/admin/matriculas'].post.responses['201']").exists());
    }

    private JsonNode cadastrar(String recurso, Object request) throws Exception {
        var result = mvc.perform(post("/api/admin/" + recurso).with(user("gestao").roles("GESTAO"))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private void erroCadastro(String recurso, Object request, int esperado) throws Exception {
        mvc.perform(post("/api/admin/" + recurso).with(user("gestao").roles("GESTAO"))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)))
                .andExpect(status().is(esperado))
                .andExpect(jsonPath("$.status").value(esperado));
    }
}
