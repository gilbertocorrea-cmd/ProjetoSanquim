package com.lab.projetosanquim.service;

import com.lab.projetosanquim.dto.*;
import com.lab.projetosanquim.entity.*;
import com.lab.projetosanquim.exception.RecursoNaoEncontradoException;
import com.lab.projetosanquim.exception.RegraNegocioException;
import com.lab.projetosanquim.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {
    private final UsuarioRepository usuarios;
    private final GestorRepository gestores;
    private final ProfessorRepository professores;
    private final AlunoRepository alunos;
    private final TurmaRepository turmas;
    private final DisciplinaRepository disciplinas;
    private final MatriculaRepository matriculas;
    private final AtribuicaoDocenteRepository atribuicoes;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public GestorResponse cadastrarGestor(GestorRequest request) {
        Usuario usuario = criarUsuario(request.email(), request.senha(), Role.ROLE_GESTAO);
        Gestor gestor = gestores.saveAndFlush(Gestor.builder()
                .nome(request.nome().strip())
                .cargo(request.cargo().strip())
                .usuario(usuario)
                .build());
        return GestorResponse.from(gestor);
    }

    public List<GestorResponse> listarGestores() {
        return gestores.findAll(Sort.by("id")).stream().map(GestorResponse::from).toList();
    }

    @Transactional
    public ProfessorResponse cadastrarProfessor(ProfessorRequest request) {
        Usuario usuario = criarUsuario(request.email(), request.senha(), Role.ROLE_PROFESSOR);
        Professor professor = professores.saveAndFlush(Professor.builder()
                .nome(request.nome().strip())
                .usuario(usuario)
                .build());
        return ProfessorResponse.from(professor);
    }

    public List<ProfessorResponse> listarProfessores() {
        return professores.findAll(Sort.by("id")).stream().map(ProfessorResponse::from).toList();
    }

    @Transactional
    public AlunoResponse cadastrarAluno(AlunoRequest request) {
        Usuario usuario = criarUsuario(request.email(), request.senha(), Role.ROLE_ALUNO);
        Aluno aluno = alunos.saveAndFlush(Aluno.builder()
                .nome(request.nome().strip())
                .usuario(usuario)
                .build());
        return AlunoResponse.from(aluno);
    }

    public List<AlunoResponse> listarAlunos() {
        return alunos.findAll(Sort.by("id")).stream().map(AlunoResponse::from).toList();
    }

    @Transactional
    public TurmaResponse cadastrarTurma(TurmaRequest request) {
        Turma turma = turmas.saveAndFlush(Turma.builder()
                .nome(request.nome().strip())
                .anoLetivo(request.anoLetivo())
                .ativa(request.ativa())
                .build());
        return TurmaResponse.from(turma);
    }

    public List<TurmaResponse> listarTurmas() {
        return turmas.findAll(Sort.by("id")).stream().map(TurmaResponse::from).toList();
    }

    @Transactional
    public DisciplinaResponse cadastrarDisciplina(DisciplinaRequest request) {
        Disciplina disciplina = disciplinas.saveAndFlush(
                Disciplina.builder().nome(request.nome().strip()).build());
        return DisciplinaResponse.from(disciplina);
    }

    public List<DisciplinaResponse> listarDisciplinas() {
        return disciplinas.findAll(Sort.by("id")).stream().map(DisciplinaResponse::from).toList();
    }

    @Transactional
    public MatriculaResponse cadastrarMatricula(MatriculaRequest request) {
        Aluno aluno = alunos.findById(request.alunoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado"));
        Turma turma = buscarTurmaAtiva(request.turmaId());
        if (matriculas.existsByAlunoIdAndTurmaId(aluno.getId(), turma.getId())) {
            throw new RegraNegocioException("O aluno já possui matrícula nesta turma");
        }

        Matricula matricula = matriculas.saveAndFlush(Matricula.builder()
                .aluno(aluno).turma(turma).ativa(true).build());
        return MatriculaResponse.from(matricula);
    }

    public List<MatriculaResponse> listarMatriculas() {
        return matriculas.findAll(Sort.by("id")).stream().map(MatriculaResponse::from).toList();
    }

    @Transactional
    public AtribuicaoDocenteResponse cadastrarAtribuicao(AtribuicaoDocenteRequest request) {
        Professor professor = professores.findById(request.professorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor não encontrado"));
        Turma turma = buscarTurmaAtiva(request.turmaId());
        Disciplina disciplina = disciplinas.findById(request.disciplinaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Disciplina não encontrada"));
        if (atribuicoes.existsByProfessorIdAndTurmaIdAndDisciplinaId(
                professor.getId(), turma.getId(), disciplina.getId())) {
            throw new RegraNegocioException("O professor já está atribuído a esta turma e disciplina");
        }

        AtribuicaoDocente atribuicao = atribuicoes.saveAndFlush(AtribuicaoDocente.builder()
                .professor(professor).turma(turma).disciplina(disciplina).build());
        return AtribuicaoDocenteResponse.from(atribuicao);
    }

    public List<AtribuicaoDocenteResponse> listarAtribuicoes() {
        return atribuicoes.findAll(Sort.by("id")).stream().map(AtribuicaoDocenteResponse::from).toList();
    }

    private Usuario criarUsuario(String email, String senha, Role role) {
        String emailNormalizado = email.strip().toLowerCase(Locale.ROOT);
        if (usuarios.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new RegraNegocioException("Já existe um usuário com este e-mail");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RegraNegocioException("A senha deve ter até 72 bytes em UTF-8");
        }

        return Usuario.builder()
                .email(emailNormalizado)
                .senhaHash(passwordEncoder.encode(senha))
                .role(role)
                .build();
    }

    private Turma buscarTurmaAtiva(Long id) {
        Turma turma = turmas.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma não encontrada"));
        if (!turma.isAtiva()) {
            throw new RegraNegocioException("A turma está inativa");
        }
        return turma;
    }
}
