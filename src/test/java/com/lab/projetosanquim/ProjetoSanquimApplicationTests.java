package com.lab.projetosanquim;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.lab.projetosanquim.config.DataSeeder;
import com.lab.projetosanquim.dto.LoginResponse;
import com.lab.projetosanquim.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sanquim;DB_CLOSE_DELAY=-1;MODE=MySQL",
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
class ProjetoSanquimApplicationTests {
    static final String SENHA_TESTE = "TesteLocal123!";
    static final String CHAVE_TESTE = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private DataSeeder seeder;

    @Autowired
    private UsuarioRepository usuarios;

    @ParameterizedTest
    @CsvSource({
            "admin@sanquim.br, ROLE_GESTAO, 200, 200, 403",
            "professor1@sanquim.br, ROLE_PROFESSOR, 403, 200, 403",
            "aluno1@sanquim.br, ROLE_ALUNO, 403, 403, 200"
    })
    void autenticaCadaPerfilEAplicaPermissoes(
            String email, String role, int adminStatus, int diarioStatus, int alunoStatus
    ) throws Exception {
        String json = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "senha": "%s"}
                                """.formatted(email, SENHA_TESTE)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.role").value(role))
                .andReturn().getResponse().getContentAsString();

        LoginResponse login = mapper.readValue(json, LoginResponse.class);
        assertThat(login.expiraEm()).isAfter(Instant.now());
        assertThat(JWT.decode(login.token()).getExpiresAtAsInstant()).isEqualTo(login.expiraEm());

        // Cada perfil acessa somente os módulos autorizados.
        verificaAcesso("/api/admin/alunos", login.token(), adminStatus);
        verificaAcesso("/api/diario/minhas-turmas", login.token(), diarioStatus);
        verificaAcesso("/api/aluno/boletim", login.token(), alunoStatus);

        if ("ROLE_ALUNO".equals(role)) {
            mvc.perform(post("/api/diario/chamada")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + login.token()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }
    }

    @Test
    void padronizaEntradaECredenciaisInvalidas() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "email-invalido", "senha": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.camposInvalidos.length()").value(1));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@sanquim.br", "senha": "senha-incorreta"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
    }

    @Test
    void exigeTokenValidoENaoExpirado() throws Exception {
        mvc.perform(get("/api/aluno/boletim"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401));

        verificaAcesso("/api/aluno/boletim", "invalido", 401);

        var usuario = usuarios.findByEmail("aluno1@sanquim.br").orElseThrow();
        String expirado = JWT.create()
                .withIssuer("projetoSanquim")
                .withSubject(usuario.getId().toString())
                .withClaim("role", "ROLE_ALUNO")
                .withExpiresAt(Instant.now().minusSeconds(60))
                .sign(Algorithm.HMAC256(Base64.getDecoder().decode(CHAVE_TESTE)));
        verificaAcesso("/api/aluno/boletim", expirado, 401);
    }

    @Test
    void seederPreservaContasExistentes() {
        assertThat(usuarios.count()).isEqualTo(8);
        var admin = usuarios.findByEmail("admin@sanquim.br").orElseThrow();
        assertThat(admin.getSenhaHash()).startsWith("$2");

        seeder.run();

        assertThat(usuarios.count()).isEqualTo(8);
        assertThat(usuarios.findById(admin.getId()).orElseThrow().getSenhaHash())
                .isEqualTo(admin.getSenhaHash());
    }

    @Test
    void swaggerPublicaLoginEEsquemaBearer() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    void qualquerSenhaInvalidaNoLoginRetorna401() throws Exception {
        var senhas = java.util.Arrays.asList(null, "", " ", "a".repeat(73), "é".repeat(37), "😀".repeat(19));
        for (int i = 0; i < senhas.size(); i++) {
            var request = new com.lab.projetosanquim.dto.LoginRequest("senha-invalida" + i + "@sanquim.br", senhas.get(i));
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
        }
    }

    private void verificaAcesso(String rota, String token, int esperado) throws Exception {
        var resultado = mvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().is(esperado));
        if (esperado != 200) {
            resultado.andExpect(jsonPath("$.status").value(esperado));
        }
    }
}
