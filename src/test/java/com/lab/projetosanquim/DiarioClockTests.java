package com.lab.projetosanquim;

import com.lab.projetosanquim.dto.*;
import com.lab.projetosanquim.entity.*;
import com.lab.projetosanquim.repository.AlunoRepository;
import com.lab.projetosanquim.repository.PlanoAulaRepository;
import com.lab.projetosanquim.security.UsuarioDetails;
import com.lab.projetosanquim.service.DiarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:diario-clock;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false", "sanquim.demo.seed=false",
        "SANQUIM_JWT_SECRET=" + ProjetoSanquimApplicationTests.CHAVE_TESTE
})
@AutoConfigureMockMvc
@Import(DiarioClockTests.RelogioFixo.class)
class DiarioClockTests {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @MockitoBean private DiarioService diario;
    @MockitoBean private AlunoRepository alunos;
    @MockitoBean private PlanoAulaRepository planos;

    @Test
    void amanhaEmSaoPauloRetorna400AntesDoServico() throws Exception {
        mvc.perform(post("/api/diario/chamada").with(user(principal(Role.ROLE_PROFESSOR)))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(chamada("2026-11-22"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.camposInvalidos[*].campo", hasItem("dataAula")));
        verifyNoInteractions(diario);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-11-21", "2026-11-20"})
    void hojeEPassadoSaoAceitos(String data) throws Exception {
        ChamadaRequest request = chamada(data);
        mvc.perform(post("/api/diario/chamada").with(user(principal(Role.ROLE_PROFESSOR)))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        verify(diario).registrarChamada(eq(request), any());
    }

    @Test
    void aulasUsamMesmoClockComDataAnteriorADataUtc() throws Exception {
        when(alunos.findByUsuarioId(7L)).thenReturn(Optional.of(Aluno.builder().id(11L).nome("Aluno").build()));
        when(planos.consultarDoAluno(11L, LocalDate.of(2026, 11, 21))).thenReturn(List.of());
        mvc.perform(get("/api/aluno/aulas").with(user(principal(Role.ROLE_ALUNO))))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        verify(planos).consultarDoAluno(11L, LocalDate.of(2026, 11, 21));
    }

    @Test
    void planoEAvaliacaoContinuamAceitandoDatasFuturas() throws Exception {
        LocalDate futura = LocalDate.of(2099, 1, 1);
        PlanoAulaRequest plano = new PlanoAulaRequest(1L, 1L, futura, "Tópicos futuros");
        AvaliacaoRequest avaliacao = new AvaliacaoRequest(1L, 1L, "Prova", "PROVA", BigDecimal.TEN, BigDecimal.ONE, futura);
        for (var entrada : List.of(new Object[]{"/planos-aula", plano}, new Object[]{"/avaliacoes", avaliacao})) {
            mvc.perform(post("/api/diario" + entrada[0]).with(user(principal(Role.ROLE_PROFESSOR)))
                            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(entrada[1])))
                    .andExpect(status().isCreated());
        }
        verify(diario).registrarPlanoAula(eq(plano), any());
        verify(diario).cadastrarAvaliacao(eq(avaliacao), any());
    }

    private ChamadaRequest chamada(String data) {
        return new ChamadaRequest(1L, 1L, LocalDate.parse(data), List.of(new RegistroPresenca(11L, true)));
    }

    private UsuarioDetails principal(Role role) {
        return new UsuarioDetails(Usuario.builder().id(7L).email("perfil@clock.test")
                .senhaHash("fixture-sem-login").role(role).build());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RelogioFixo {
        @Bean
        @Primary
        Clock clockTeste() {
            // Em UTC já é dia 22; em São Paulo ainda é dia 21.
            return Clock.fixed(Instant.parse("2026-11-22T01:30:00Z"), ZoneId.of("America/Sao_Paulo"));
        }
    }
}
