package com.lab.projetosanquim;

import com.lab.projetosanquim.dto.AlunoRequest;
import com.lab.projetosanquim.dto.GestorRequest;
import com.lab.projetosanquim.dto.ProfessorRequest;
import com.lab.projetosanquim.repository.AlunoRepository;
import com.lab.projetosanquim.repository.GestorRepository;
import com.lab.projetosanquim.repository.ProfessorRepository;
import com.lab.projetosanquim.repository.UsuarioRepository;
import com.lab.projetosanquim.service.AlunoService;
import com.lab.projetosanquim.service.GestorService;
import com.lab.projetosanquim.service.ProfessorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:transacoes;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "sanquim.demo.seed=false",
        "SANQUIM_JWT_SECRET=" + ProjetoSanquimApplicationTests.CHAVE_TESTE
})
class CadastroTransactionTests {
    @Autowired private AlunoService alunos;
    @Autowired private ProfessorService professores;
    @Autowired private GestorService gestores;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private AlunoRepository alunoRepository;
    @Autowired private ProfessorRepository professorRepository;
    @Autowired private GestorRepository gestorRepository;

    @Test
    void falhaAoPersistirPerfilDesfazUsuarioNaMesmaTransacao() {
        // Invocação interna força a falha no banco após o INSERT do usuário.
        String nomeInvalido = "a".repeat(256);
        assertThatThrownBy(() -> alunos.cadastrarAluno(
                new AlunoRequest(nomeInvalido, "rollback-aluno@sanquim.br", "Fixture123!")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> professores.cadastrarProfessor(
                new ProfessorRequest(nomeInvalido, "rollback-professor@sanquim.br", "Fixture123!")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> gestores.cadastrarGestor(
                new GestorRequest(nomeInvalido, "Secretaria", "rollback-gestor@sanquim.br", "Fixture123!")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(usuarios.count()).isZero();
        assertThat(alunoRepository.count()).isZero();
        assertThat(professorRepository.count()).isZero();
        assertThat(gestorRepository.count()).isZero();
    }
}
