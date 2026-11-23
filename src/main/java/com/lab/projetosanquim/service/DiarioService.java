package com.lab.projetosanquim.service;

import com.lab.projetosanquim.dto.*;
import com.lab.projetosanquim.entity.*;
import com.lab.projetosanquim.exception.RecursoNaoEncontradoException;
import com.lab.projetosanquim.exception.RegraNegocioException;
import com.lab.projetosanquim.repository.*;
import com.lab.projetosanquim.security.UsuarioDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiarioService {
    private final ProfessorRepository professores;
    private final AtribuicaoDocenteRepository atribuicoes;
    private final TurmaService turmas;
    private final DisciplinaRepository disciplinas;
    private final AlunoRepository alunos;
    private final MatriculaRepository matriculas;
    private final ChamadaRepository chamadas;
    private final FrequenciaRepository frequencias;
    private final AvaliacaoRepository avaliacoes;
    private final NotaRepository notas;
    private final PlanoAulaRepository planos;

    public List<MinhasTurmasResponse> minhasTurmas(UsuarioDetails usuario) {
        return professores.findByUsuarioId(usuario.getId())
                .map(professor -> atribuicoes.findByProfessorOrderByTurmaIdAscDisciplinaIdAsc(professor).stream()
                        .map(MinhasTurmasResponse::from).toList())
                .orElseGet(List::of);
    }

    @Transactional
    public ChamadaResponse registrarChamada(ChamadaRequest request, UsuarioDetails usuario) {
        Vinculo vinculo = validarAcesso(request.turmaId(), request.disciplinaId(), usuario);
        if (chamadas.existsByTurmaAndDisciplinaAndDataAula(
                vinculo.turma(), vinculo.disciplina(), request.dataAula())) {
            throw new RegraNegocioException("Já existe chamada para esta turma, disciplina e data");
        }

        Set<Long> ids = new HashSet<>();
        for (RegistroPresenca registro : request.registros()) {
            if (!ids.add(registro.alunoId())) {
                throw new RegraNegocioException("O mesmo aluno foi informado mais de uma vez na chamada");
            }
        }
        Map<Long, Aluno> alunosDaChamada = alunos.findAllById(ids).stream()
                .collect(Collectors.toMap(Aluno::getId, Function.identity()));
        if (alunosDaChamada.size() != ids.size()) {
            throw new RecursoNaoEncontradoException("Aluno não encontrado");
        }
        Set<Long> matriculados = matriculas.findByTurmaIdAndAtivaTrueAndAlunoIdIn(request.turmaId(), ids)
                .stream().map(matricula -> matricula.getAluno().getId()).collect(Collectors.toSet());
        if (!matriculados.containsAll(ids)) {
            throw new RegraNegocioException("Todos os alunos da chamada devem ter matrícula ativa na turma");
        }
        Set<Long> todosMatriculados = matriculas.findByTurmaIdAndAtivaTrue(request.turmaId())
                .stream().map(matricula -> matricula.getAluno().getId()).collect(Collectors.toSet());
        if (!ids.containsAll(todosMatriculados)) {
            throw new RegraNegocioException("A chamada deve incluir todos os alunos com matrícula ativa na turma");
        }

        Chamada chamada = chamadas.saveAndFlush(Chamada.builder()
                .turma(vinculo.turma()).disciplina(vinculo.disciplina()).dataAula(request.dataAula()).build());
        List<Frequencia> registros = request.registros().stream()
                .map(registro -> Frequencia.builder()
                        .chamada(chamada).aluno(alunosDaChamada.get(registro.alunoId()))
                        .presente(registro.presente()).build())
                .toList();
        frequencias.saveAllAndFlush(registros);
        return ChamadaResponse.from(chamada, request.registros());
    }

    @Transactional
    public AvaliacaoResponse cadastrarAvaliacao(AvaliacaoRequest request, UsuarioDetails usuario) {
        Vinculo vinculo = validarAcesso(request.turmaId(), request.disciplinaId(), usuario);
        Avaliacao avaliacao = avaliacoes.saveAndFlush(Avaliacao.builder()
                .turma(vinculo.turma()).disciplina(vinculo.disciplina())
                .titulo(request.titulo().strip()).tipo(request.tipo().strip())
                .notaMaxima(request.notaMaxima()).peso(request.peso()).dataEntrega(request.dataEntrega()).build());
        return AvaliacaoResponse.from(avaliacao);
    }

    @Transactional
    public NotaResponse lancarNota(Long avaliacaoId, NotaRequest request, UsuarioDetails usuario) {
        Avaliacao consulta = avaliacoes.findById(avaliacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação não encontrada"));
        validarAcesso(consulta.getTurma().getId(), consulta.getDisciplina().getId(), usuario);
        // O lock da avaliação serializa a criação/atualização da mesma nota.
        Avaliacao avaliacao = avaliacoes.buscarParaAtualizacao(avaliacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação não encontrada"));
        Aluno aluno = alunos.findById(request.alunoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado"));
        if (!matriculas.existsByAlunoIdAndTurmaIdAndAtivaTrue(aluno.getId(), avaliacao.getTurma().getId())) {
            throw new RegraNegocioException("O aluno deve ter matrícula ativa na turma da avaliação");
        }
        if (request.valor().compareTo(avaliacao.getNotaMaxima()) > 0) {
            throw new RegraNegocioException("A nota não pode ser maior que a nota máxima da avaliação");
        }

        Nota nota = notas.buscarParaAtualizacao(avaliacao, aluno)
                .orElseGet(() -> Nota.builder().avaliacao(avaliacao).aluno(aluno).build());
        nota.setValor(request.valor());
        return NotaResponse.from(notas.saveAndFlush(nota));
    }

    @Transactional
    public AulaResponse registrarPlanoAula(PlanoAulaRequest request, UsuarioDetails usuario) {
        Vinculo vinculo = validarAcesso(request.turmaId(), request.disciplinaId(), usuario);
        PlanoAula plano = planos.saveAndFlush(PlanoAula.builder()
                .turma(vinculo.turma()).disciplina(vinculo.disciplina())
                .dataAula(request.dataAula()).topicos(request.topicos().strip()).build());
        return AulaResponse.from(plano);
    }

    private Vinculo validarAcesso(Long turmaId, Long disciplinaId, UsuarioDetails usuario) {
        if (usuario.getRole() != Role.ROLE_GESTAO) {
            Professor professor = professores.findByUsuarioId(usuario.getId())
                    .orElseThrow(() -> new AccessDeniedException("Professor sem vínculo"));
            if (!atribuicoes.existsByProfessorIdAndTurmaIdAndDisciplinaId(professor.getId(), turmaId, disciplinaId)) {
                throw new AccessDeniedException("Turma e disciplina não atribuídas ao professor");
            }
        }
        Turma turma = turmas.buscarAtiva(turmaId);
        Disciplina disciplina = disciplinas.findById(disciplinaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Disciplina não encontrada"));
        return new Vinculo(turma, disciplina);
    }

    private record Vinculo(Turma turma, Disciplina disciplina) {
    }
}
