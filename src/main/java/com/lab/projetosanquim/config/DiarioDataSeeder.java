package com.lab.projetosanquim.config;

import com.lab.projetosanquim.dto.*;
import com.lab.projetosanquim.entity.*;
import com.lab.projetosanquim.repository.*;
import com.lab.projetosanquim.security.UsuarioDetails;
import com.lab.projetosanquim.service.DiarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(1)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sanquim.demo.seed", havingValue = "true")
public class DiarioDataSeeder implements CommandLineRunner {
    private final ProfessorRepository professores;
    private final AlunoRepository alunos;
    private final TurmaRepository turmas;
    private final DisciplinaRepository disciplinas;
    private final MatriculaRepository matriculas;
    private final AtribuicaoDocenteRepository atribuicoes;
    private final ChamadaRepository chamadas;
    private final AvaliacaoRepository avaliacoes;
    private final NotaRepository notas;
    private final DiarioService diario;
    private final Environment environment;

    @PostConstruct
    void validarPerfil() {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("A carga de demonstração não pode ser executada no perfil prod");
        }
    }

    @Override
    @Transactional
    public void run(String... args) {
        Professor primeiro = professores.findByUsuarioEmail("professor1@sanquim.br")
                .orElseThrow(() -> new IllegalStateException("Perfil de professor1 ausente na carga de demonstração"));
        Professor segundo = professores.findByUsuarioEmail("professor2@sanquim.br")
                .orElseThrow(() -> new IllegalStateException("Perfil de professor2 ausente na carga de demonstração"));
        Turma turma = turmas.findFirstByNomeAndAnoLetivoOrderByIdAsc("Demonstração Sanquim", 2026)
                .orElseGet(() -> turmas.saveAndFlush(Turma.builder()
                        .nome("Demonstração Sanquim").anoLetivo(2026).ativa(true).build()));
        Disciplina matematica = disciplina("Matemática");
        Disciplina historia = disciplina("História");
        atribuir(primeiro, turma, matematica);
        atribuir(segundo, turma, historia);
        List<Aluno> estudantes = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            String email = "aluno" + i + "@sanquim.br";
            Aluno aluno = alunos.findByUsuarioEmail(email)
                    .orElseThrow(() -> new IllegalStateException("Perfil de aluno ausente na carga de demonstração"));
            estudantes.add(aluno);
            if (!matriculas.existsByAlunoIdAndTurmaId(aluno.getId(), turma.getId())) {
                matriculas.saveAndFlush(Matricula.builder().aluno(aluno).turma(turma).ativa(true).build());
            }
        }

        UsuarioDetails principal = new UsuarioDetails(primeiro.getUsuario());
        LocalDate data = LocalDate.of(2026, 11, 10);
        if (!chamadas.existsByTurmaAndDisciplinaAndDataAula(turma, matematica, data)) {
            List<RegistroPresenca> registros = estudantes.stream()
                    .map(aluno -> new RegistroPresenca(aluno.getId(), true)).toList();
            diario.registrarChamada(new ChamadaRequest(turma.getId(), matematica.getId(), data, registros), principal);
        }
        String titulo = "Avaliação de demonstração";
        Avaliacao avaliacao = avaliacoes.findFirstByTurmaIdAndDisciplinaIdAndTituloOrderByIdAsc(
                        turma.getId(), matematica.getId(), titulo)
                .orElseGet(() -> {
                    AvaliacaoResponse response = diario.cadastrarAvaliacao(new AvaliacaoRequest(
                            turma.getId(), matematica.getId(), titulo, "PROVA",
                            BigDecimal.TEN, BigDecimal.ONE, data), principal);
                    return avaliacoes.findById(response.id()).orElseThrow();
                });
        for (Aluno aluno : estudantes) {
            if (notas.findByAvaliacaoAndAluno(avaliacao, aluno).isEmpty()) {
                diario.lancarNota(avaliacao.getId(), new NotaRequest(aluno.getId(), BigDecimal.valueOf(8)), principal);
            }
        }
    }

    private Disciplina disciplina(String nome) {
        return disciplinas.findFirstByNomeOrderByIdAsc(nome)
                .orElseGet(() -> disciplinas.saveAndFlush(Disciplina.builder().nome(nome).build()));
    }

    private void atribuir(Professor professor, Turma turma, Disciplina disciplina) {
        if (!atribuicoes.existsByProfessorIdAndTurmaIdAndDisciplinaId(
                professor.getId(), turma.getId(), disciplina.getId())) {
            atribuicoes.saveAndFlush(AtribuicaoDocente.builder()
                    .professor(professor).turma(turma).disciplina(disciplina).build());
        }
    }
}
