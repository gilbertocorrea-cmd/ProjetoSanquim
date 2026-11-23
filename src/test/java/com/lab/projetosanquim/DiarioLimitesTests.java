package com.lab.projetosanquim;

import com.lab.projetosanquim.dto.*;
import com.lab.projetosanquim.entity.Role;
import com.lab.projetosanquim.entity.Usuario;
import com.lab.projetosanquim.security.UsuarioDetails;
import com.lab.projetosanquim.service.DiarioService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:diario-limites;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false", "sanquim.demo.seed=false",
        "SANQUIM_JWT_SECRET=" + ProjetoSanquimApplicationTests.CHAVE_TESTE
})
@AutoConfigureMockMvc
class DiarioLimitesTests {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private Validator validator;
    @MockitoBean private DiarioService diario;

    @Test
    void registros501Retornam400ComCampoSemChamarServico() throws Exception {
        rejeitar("/chamada", chamada(501), "registros");
    }

    @Test
    void topicos10001Retornam400ComCampoSemChamarServico() throws Exception {
        rejeitar("/planos-aula", new PlanoAulaRequest(1L, 1L, LocalDate.now(), "a".repeat(10001)), "topicos");
    }

    @Test
    void validatorAceita500Registros() {
        assertThat(validator.validate(chamada(500))).isEmpty();
    }

    @Test
    void validatorAceita10000Caracteres() {
        assertThat(validator.validate(new PlanoAulaRequest(1L, 1L, LocalDate.now(), "a".repeat(10000)))).isEmpty();
    }

    private ChamadaRequest chamada(int quantidade) {
        return new ChamadaRequest(1L, 1L, LocalDate.now().minusDays(1), LongStream.rangeClosed(1, quantidade)
                .mapToObj(id -> new RegistroPresenca(id, true)).toList());
    }

    private void rejeitar(String path, Object corpo, String campo) throws Exception {
        UsuarioDetails professor = new UsuarioDetails(Usuario.builder().id(1L).email("professor@limites.test")
                .role(Role.ROLE_PROFESSOR).senhaHash("fixture-sem-login").build());
        mvc.perform(post("/api/diario" + path).with(user(professor)).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(corpo)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.camposInvalidos[*].campo", hasItem(campo)));
        verifyNoInteractions(diario);
    }
}
