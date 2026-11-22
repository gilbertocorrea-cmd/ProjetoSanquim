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
        "sanquim.demo.seed=false",
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
                .andExpect(jsonPath("$.conteudo[?(@.email == 'novo@sanquim.br')]").isNotEmpty())
                .andExpect(jsonPath("$.conteudo[*].senhaHash").isEmpty());

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

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "gestores", "professores", "alunos", "turmas", "disciplinas", "matriculas", "atribuicoes"
    })
    void todasAsListagensUsamRespostaPaginada(String recurso) throws Exception {
        mvc.perform(get("/api/admin/" + recurso).with(user("gestao").roles("GESTAO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isArray())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.totalElementos").isNumber())
                .andExpect(jsonPath("$.totalPaginas").isNumber())
                .andExpect(jsonPath("$.pageable").doesNotExist());
    }

    @Test
    void paginacaoOrdenaPorIdIgnoraSortEInformaTotais() throws Exception {
        long primeiro = cadastrar("turmas", Map.of(
                "nome", "Z", "anoLetivo", 2026, "ativa", true)).get("id").asLong();
        long segundo = cadastrar("turmas", Map.of(
                "nome", "A", "anoLetivo", 2026, "ativa", true)).get("id").asLong();
        cadastrar("turmas", Map.of("nome", "B", "anoLetivo", 2026, "ativa", true));
        mvc.perform(get("/api/admin/turmas").with(user("gestao").roles("GESTAO"))
                        .param("page", "0").param("size", "2").param("sort", "nome,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].id").value(primeiro))
                .andExpect(jsonPath("$.conteudo[1].id").value(segundo))
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2));
        mvc.perform(get("/api/admin/turmas").with(user("gestao").roles("GESTAO"))
                        .param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.conteudo.length()").value(1));
        mvc.perform(get("/api/admin/turmas").with(user("gestao").roles("GESTAO"))
                        .param("page", "2").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isEmpty());
        mvc.perform(get("/api/admin/turmas").with(user("gestao").roles("GESTAO")).param("size", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tamanho").value(100));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "page|-1", "page|abc", "page|2147483648", "page|2147483647",
            "page|", "size|0", "size|-1", "size|101", "size|abc", "size|"
    }, delimiter = '|', emptyValue = "")
    void parametrosInvalidosRetornam400(String campo, String valor) throws Exception {
        mvc.perform(get("/api/admin/alunos").with(user("gestao").roles("GESTAO"))
                        .param(campo, valor == null ? "" : valor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.camposInvalidos[0].campo").value(campo));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"gestores", "professores", "alunos"})
    void senhasSaoValidadasPorBytesUtf8(String recurso) throws Exception {
        String[] validas = {"a".repeat(72), "é".repeat(36), "😀".repeat(18)};
        String[] invalidas = {"a".repeat(73), "é".repeat(37), "😀".repeat(19)};
        for (int i = 0; i < validas.length; i++) {
            String email = "bytes" + i + recurso + "@sanquim.br";
            cadastrar(recurso, Map.of(
                    "nome", "Teste UTF-8", "cargo", "Secretaria", "email", email, "senha", validas[i]));
            mvc.perform(post("/api/admin/" + recurso).with(user("gestao").roles("GESTAO"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(Map.of(
                                    "nome", "Teste UTF-8", "cargo", "Secretaria",
                                    "email", "invalida" + email, "senha", invalidas[i]))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.camposInvalidos[0].campo").value("senha"));
        }
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
