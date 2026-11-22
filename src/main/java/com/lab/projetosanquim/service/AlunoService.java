package com.lab.projetosanquim.service;

import com.lab.projetosanquim.dto.*;
import com.lab.projetosanquim.entity.*;
import com.lab.projetosanquim.repository.*;
import com.lab.projetosanquim.exception.RecursoNaoEncontradoException;
import com.lab.projetosanquim.security.UsuarioDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlunoService {
    private final AlunoRepository alunos;
    private final UsuarioService usuarios;
    private final FrequenciaRepository frequencias;
    private final PlanoAulaRepository planos;
    private final NotaRepository notas;
    private final AvaliacaoRepository avaliacoes;

    @Transactional
    public AlunoResponse cadastrarAluno(AlunoRequest request) {
        Usuario usuario = usuarios.criarUsuario(request.email(), request.senha(), Role.ROLE_ALUNO);
        Aluno aluno = alunos.saveAndFlush(Aluno.builder()
                .nome(request.nome().strip())
                .usuario(usuario)
                .build());
        return AlunoResponse.from(aluno);
    }

    public PaginaResponse<AlunoResponse> listarAlunos(Pageable paginacao) {
        return PaginaResponse.from(alunos.findAll(paginacao).map(AlunoResponse::from));
    }

    public BoletimResponse boletim(UsuarioDetails usuario) {
        Aluno aluno = buscarAlunoLogado(usuario);
        Map<Long, Nota> notasPorAvaliacao = notas.findByAlunoId(aluno.getId()).stream()
                .collect(Collectors.toMap(nota -> nota.getAvaliacao().getId(), nota -> nota));
        Map<ChaveDisciplina, List<Avaliacao>> grupos = avaliacoes.consultarDoAluno(aluno.getId()).stream()
                .collect(Collectors.groupingBy(
                        avaliacao -> new ChaveDisciplina(avaliacao.getTurma().getId(), avaliacao.getDisciplina().getId()),
                        LinkedHashMap::new, Collectors.toList()));
        List<DisciplinaBoletimResponse> disciplinas = grupos.values().stream()
                .map(grupo -> boletimDisciplina(grupo, notasPorAvaliacao)).toList();
        return new BoletimResponse(aluno.getId(), disciplinas);
    }

    private DisciplinaBoletimResponse boletimDisciplina(List<Avaliacao> grupo, Map<Long, Nota> notasPorAvaliacao) {
        List<NotaBoletimResponse> registros = new java.util.ArrayList<>();
        BigDecimal somaPonderada = BigDecimal.ZERO;
        BigDecimal somaPesos = BigDecimal.ZERO;
        for (Avaliacao avaliacao : grupo) {
            Nota nota = notasPorAvaliacao.get(avaliacao.getId());
            BigDecimal normalizada = null;
            if (nota != null) {
                normalizada = nota.getValor().multiply(BigDecimal.TEN)
                        .divide(avaliacao.getNotaMaxima(), MathContext.DECIMAL128);
                somaPonderada = somaPonderada.add(normalizada.multiply(avaliacao.getPeso()));
                somaPesos = somaPesos.add(avaliacao.getPeso());
            }
            registros.add(NotaBoletimResponse.from(avaliacao, nota,
                    normalizada == null ? null : normalizada.setScale(4, RoundingMode.HALF_UP)));
        }
        BigDecimal media = somaPesos.signum() == 0 ? null
                : somaPonderada.divide(somaPesos, 4, RoundingMode.HALF_UP);
        return DisciplinaBoletimResponse.from(grupo.get(0), registros, media);
    }

    public FrequenciaResponse frequencia(UsuarioDetails usuario) {
        Aluno aluno = buscarAlunoLogado(usuario);
        List<Frequencia> registros = frequencias.consultarDoAluno(aluno.getId());
        Map<ChaveDisciplina, List<Frequencia>> grupos = registros.stream().collect(Collectors.groupingBy(
                registro -> new ChaveDisciplina(registro.getChamada().getTurma().getId(),
                        registro.getChamada().getDisciplina().getId()),
                LinkedHashMap::new, Collectors.toList()));
        List<FrequenciaDisciplinaResponse> disciplinas = grupos.values().stream()
                .map(grupo -> {
                    long presencas = grupo.stream().filter(Frequencia::isPresente).count();
                    return FrequenciaDisciplinaResponse.from(grupo.get(0).getChamada(), grupo.size(),
                            presencas, percentual(presencas, grupo.size()));
                }).toList();
        long presencas = registros.stream().filter(Frequencia::isPresente).count();
        return new FrequenciaResponse(aluno.getId(), registros.size(), presencas,
                registros.size() - presencas, percentual(presencas, registros.size()), disciplinas);
    }

    public List<AulaResponse> aulas(UsuarioDetails usuario) {
        Aluno aluno = buscarAlunoLogado(usuario);
        return planos.consultarDoAluno(aluno.getId(), LocalDate.now()).stream().map(AulaResponse::from).toList();
    }

    private Aluno buscarAlunoLogado(UsuarioDetails usuario) {
        return alunos.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado"));
    }

    private BigDecimal percentual(long presencas, long total) {
        if (total == 0) {
            return null;
        }
        return BigDecimal.valueOf(presencas).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }

    private record ChaveDisciplina(Long turmaId, Long disciplinaId) {
    }
}
