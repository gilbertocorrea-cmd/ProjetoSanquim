package com.lab.projetosanquim.controller;

import com.lab.projetosanquim.dto.*;
import com.lab.projetosanquim.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Gestão", description = "Cadastros administrativos exclusivos de ROLE_GESTAO")
public class AdminController {
    private final GestorService gestores;
    private final ProfessorService professores;
    private final AlunoService alunos;
    private final TurmaService turmas;
    private final DisciplinaService disciplinas;
    private final MatriculaService matriculas;
    private final AtribuicaoDocenteService atribuicoes;

    @Operation(summary = "Cadastra gestores")
    @PostMapping("/gestores")
    @ResponseStatus(HttpStatus.CREATED)
    public GestorResponse cadastrarGestor(@Valid @RequestBody GestorRequest request) {
        return gestores.cadastrarGestor(request);
    }

    @Operation(summary = "Lista gestores")
    @GetMapping("/gestores")
    public PaginaResponse<GestorResponse> listarGestores(@ParameterObject @ModelAttribute PaginacaoRequest paginacao) {
        return gestores.listarGestores(paginacao.toPageable());
    }

    @Operation(summary = "Cadastra professores")
    @PostMapping("/professores")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfessorResponse cadastrarProfessor(@Valid @RequestBody ProfessorRequest request) {
        return professores.cadastrarProfessor(request);
    }

    @Operation(summary = "Lista professores")
    @GetMapping("/professores")
    public PaginaResponse<ProfessorResponse> listarProfessores(@ParameterObject @ModelAttribute PaginacaoRequest paginacao) {
        return professores.listarProfessores(paginacao.toPageable());
    }

    @Operation(summary = "Cadastra alunos")
    @PostMapping("/alunos")
    @ResponseStatus(HttpStatus.CREATED)
    public AlunoResponse cadastrarAluno(@Valid @RequestBody AlunoRequest request) {
        return alunos.cadastrarAluno(request);
    }

    @Operation(summary = "Lista alunos")
    @GetMapping("/alunos")
    public PaginaResponse<AlunoResponse> listarAlunos(@ParameterObject @ModelAttribute PaginacaoRequest paginacao) {
        return alunos.listarAlunos(paginacao.toPageable());
    }

    @Operation(summary = "Cadastra turmas")
    @PostMapping("/turmas")
    @ResponseStatus(HttpStatus.CREATED)
    public TurmaResponse cadastrarTurma(@Valid @RequestBody TurmaRequest request) {
        return turmas.cadastrarTurma(request);
    }

    @Operation(summary = "Lista turmas")
    @GetMapping("/turmas")
    public PaginaResponse<TurmaResponse> listarTurmas(@ParameterObject @ModelAttribute PaginacaoRequest paginacao) {
        return turmas.listarTurmas(paginacao.toPageable());
    }

    @Operation(summary = "Cadastra disciplinas")
    @PostMapping("/disciplinas")
    @ResponseStatus(HttpStatus.CREATED)
    public DisciplinaResponse cadastrarDisciplina(@Valid @RequestBody DisciplinaRequest request) {
        return disciplinas.cadastrarDisciplina(request);
    }

    @Operation(summary = "Lista disciplinas")
    @GetMapping("/disciplinas")
    public PaginaResponse<DisciplinaResponse> listarDisciplinas(@ParameterObject @ModelAttribute PaginacaoRequest paginacao) {
        return disciplinas.listarDisciplinas(paginacao.toPageable());
    }

    @Operation(summary = "Cadastra uma matrícula")
    @PostMapping("/matriculas")
    @ResponseStatus(HttpStatus.CREATED)
    public MatriculaResponse cadastrarMatricula(@Valid @RequestBody MatriculaRequest request) {
        return matriculas.cadastrarMatricula(request);
    }

    @Operation(summary = "Lista matrículas")
    @GetMapping("/matriculas")
    public PaginaResponse<MatriculaResponse> listarMatriculas(@ParameterObject @ModelAttribute PaginacaoRequest paginacao) {
        return matriculas.listarMatriculas(paginacao.toPageable());
    }

    @Operation(summary = "Cadastra um vínculo de professor, turma e disciplina")
    @PostMapping("/atribuicoes")
    @ResponseStatus(HttpStatus.CREATED)
    public AtribuicaoDocenteResponse cadastrarAtribuicao(@Valid @RequestBody AtribuicaoDocenteRequest request) {
        return atribuicoes.cadastrarAtribuicao(request);
    }

    @Operation(summary = "Lista atribuições docentes")
    @GetMapping("/atribuicoes")
    public PaginaResponse<AtribuicaoDocenteResponse> listarAtribuicoes(@ParameterObject @ModelAttribute PaginacaoRequest paginacao) {
        return atribuicoes.listarAtribuicoes(paginacao.toPageable());
    }
}
