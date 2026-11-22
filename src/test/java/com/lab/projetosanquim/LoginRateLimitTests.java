package com.lab.projetosanquim;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:limiter;DB_CLOSE_DELAY=-1;MODE=MySQL",
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
class LoginRateLimitTests {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void sextaTentativaPorEmailRecebe429ComRetryAfter() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("professor1@sanquim.br", "errada", "192.0.2.1", 401);
        }
        login("PROFESSOR1@sanquim.br", ProjetoSanquimApplicationTests.SENHA_TESTE, "192.0.2.2", 429);
    }

    @Test
    void emailInexistenteTambemConta() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("inexistente@sanquim.br", "errada", "192.0.2.3", 401);
        }
        login("inexistente@sanquim.br", "errada", "192.0.2.4", 429);
    }

    @Test
    void ipBloqueiaAposVinteFalhasEIgnoraCabecalhosForjados() throws Exception {
        for (int i = 0; i < 20; i++) {
            login("ip" + i + "@sanquim.br", "errada", "192.0.2.5", 401);
        }
        login("ip-novo@sanquim.br", "errada", "192.0.2.5", 429);
        login("ip-novo@sanquim.br", "errada", "192.0.2.6", 401);
    }

    @Test
    void sucessoZeraEmailSemZerarIp() throws Exception {
        for (int i = 0; i < 4; i++) {
            login("admin@sanquim.br", "errada", "192.0.2.7", 401);
        }
        login("admin@sanquim.br", ProjetoSanquimApplicationTests.SENHA_TESTE, "192.0.2.7", 200);
        for (int i = 0; i < 5; i++) {
            login("admin@sanquim.br", "errada", "192.0.2.7", 401);
        }
        login("admin@sanquim.br", "errada", "192.0.2.8", 429);
        for (int i = 0; i < 11; i++) {
            login("apos-sucesso" + i + "@sanquim.br", "errada", "192.0.2.7", 401);
        }
        login("apos-sucesso-novo@sanquim.br", "errada", "192.0.2.7", 429);
    }

    @Test
    void validacao400NaoConta() throws Exception {
        for (int i = 0; i < 20; i++) {
            login("email-invalido", "errada", "192.0.2.9", 400);
        }
        for (int i = 0; i < 5; i++) {
            login("validacao@sanquim.br", "errada", "192.0.2.9", 401);
        }
        login("validacao@sanquim.br", "errada", "192.0.2.9", 429);
    }

    private void login(String email, String senha, String ip, int esperado) throws Exception {
        var result = mvc.perform(post("/api/auth/login")
                        .with(request -> { request.setRemoteAddr(ip); return request; })
                        .header("X-Forwarded-For", email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email, "senha", senha))))
                .andExpect(status().is(esperado));
        if (esperado == 429) {
            result.andExpect(header().string("Retry-After", org.hamcrest.Matchers.matchesPattern("[1-9][0-9]*")))
                    .andExpect(jsonPath("$.status").value(429))
                    .andExpect(jsonPath("$.camposInvalidos").isEmpty());
        }
    }
}
