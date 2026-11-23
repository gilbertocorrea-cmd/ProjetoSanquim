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
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:chamada-completa;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false", "sanquim.demo.seed=false",
        "SANQUIM_JWT_SECRET=" + ProjetoSanquimApplicationTests.CHAVE_TESTE
})
@AutoConfigureMockMvc
@Transactional
class ChamadaCompletaTests {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private ProfessorRepository professores;
    @Autowired private AlunoRepository alunos;
    @Autowired private TurmaRepository turmas;
    @Autowired private DisciplinaRepository disciplinas;
    @Autowired private MatriculaRepository matriculas;
    @Autowired private AtribuicaoDocenteRepository atribuicoes;
    @Autowired private ChamadaRepository chamadas;
    @Autowired private FrequenciaRepository frequencias;
    private UsuarioDetails professor;
    private Turma turma;
    private Disciplina disciplina;
    private Aluno primeiro;
    private Aluno segundo;
    private Aluno inativo;

    @BeforeEach
    void preparar() {
        Professor perfil = professores.saveAndFlush(Professor.builder().nome("Professor")
                .usuario(usuario("professor@completa.test", Role.ROLE_PROFESSOR)).build());
        professor = new UsuarioDetails(perfil.getUsuario());
        turma = turmas.saveAndFlush(Turma.builder().nome("Completa").anoLetivo(2026).ativa(true).build());
        disciplina = disciplinas.saveAndFlush(Disciplina.builder().nome("Matemática").build());
        atribuicoes.saveAndFlush(AtribuicaoDocente.builder().professor(perfil).turma(turma).disciplina(disciplina).build());
        primeiro = aluno("primeiro", true);
        segundo = aluno("segundo", true);
        inativo = aluno("inativo", false);
    }

    @Test
    void loteIncompletoRetorna409SemGravar() throws Exception {
        enviar(List.of(registro(primeiro.getId())), 409,
                "A chamada deve incluir todos os alunos com matrícula ativa na turma");
        assertThat(chamadas.count()).isZero();
        assertThat(frequencias.count()).isZero();
    }

    @Test
    void loteCompletoIncluiSomenteMatriculasAtivas() throws Exception {
        enviar(List.of(registro(primeiro.getId()), registro(segundo.getId())), 201, null);
        assertThat(chamadas.count()).isEqualTo(1);
        assertThat(frequencias.count()).isEqualTo(2);
    }

    @Test
    void validacoesAnterioresPrecedemACompletude() throws Exception {
        enviar(List.of(registro(primeiro.getId()), registro(primeiro.getId())), 409,
                "O mesmo aluno foi informado mais de uma vez na chamada");
        enviar(List.of(registro(Long.MAX_VALUE)), 404, "Aluno não encontrado");
        enviar(List.of(registro(inativo.getId())), 409,
                "Todos os alunos da chamada devem ter matrícula ativa na turma");
        assertThat(chamadas.count()).isZero();
        assertThat(frequencias.count()).isZero();
    }

    private void enviar(List<Map<String, Object>> registros, int status, String mensagem) throws Exception {
        var result = mvc.perform(post("/api/diario/chamada").with(user(professor))
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of(
                        "turmaId", turma.getId(), "disciplinaId", disciplina.getId(),
                        "dataAula", LocalDate.now().minusDays(1).toString(), "registros", registros))))
                .andExpect(status().is(status));
        if (mensagem != null) result.andExpect(jsonPath("$.mensagem").value(mensagem));
    }

    private Map<String, Object> registro(Long id) { return Map.of("alunoId", id, "presente", true); }
    private Usuario usuario(String email, Role role) {
        return Usuario.builder().email(email).role(role).senhaHash("fixture-sem-login").build();
    }
    private Aluno aluno(String nome, boolean ativa) {
        Aluno aluno = alunos.saveAndFlush(Aluno.builder().nome(nome)
                .usuario(usuario(nome + "@completa.test", Role.ROLE_ALUNO)).build());
        matriculas.saveAndFlush(Matricula.builder().aluno(aluno).turma(turma).ativa(ativa).build());
        return aluno;
    }
}
