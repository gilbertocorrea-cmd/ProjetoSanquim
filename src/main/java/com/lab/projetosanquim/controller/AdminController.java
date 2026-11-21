package com.lab.projetosanquim.controller;

import com.lab.projetosanquim.dto.*;
import com.lab.projetosanquim.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Gestão", description = "Cadastros administrativos exclusivos de ROLE_GESTAO")
public class AdminController {
    private final AdminService adminService;

    @Operation(summary = "Cadastra gestores")
    @PostMapping("/gestores")
    @ResponseStatus(HttpStatus.CREATED)
    public GestorResponse cadastrarGestor(@Valid @RequestBody GestorRequest request) {
        return adminService.cadastrarGestor(request);
    }

    @Operation(summary = "Lista gestores")
    @GetMapping("/gestores")
    public List<GestorResponse> listarGestores() {
        return adminService.listarGestores();
    }

    @Operation(summary = "Cadastra professores")
    @PostMapping("/professores")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfessorResponse cadastrarProfessor(@Valid @RequestBody ProfessorRequest request) {
        return adminService.cadastrarProfessor(request);
    }

    @Operation(summary = "Lista professores")
    @GetMapping("/professores")
    public List<ProfessorResponse> listarProfessores() {
        return adminService.listarProfessores();
    }

    @Operation(summary = "Cadastra alunos")
    @PostMapping("/alunos")
    @ResponseStatus(HttpStatus.CREATED)
    public AlunoResponse cadastrarAluno(@Valid @RequestBody AlunoRequest request) {
        return adminService.cadastrarAluno(request);
    }

    @Operation(summary = "Lista alunos")
    @GetMapping("/alunos")
    public List<AlunoResponse> listarAlunos() {
        return adminService.listarAlunos();
    }

    @Operation(summary = "Cadastra turmas")
    @PostMapping("/turmas")
    @ResponseStatus(HttpStatus.CREATED)
    public TurmaResponse cadastrarTurma(@Valid @RequestBody TurmaRequest request) {
        return adminService.cadastrarTurma(request);
    }

    @Operation(summary = "Lista turmas")
    @GetMapping("/turmas")
    public List<TurmaResponse> listarTurmas() {
        return adminService.listarTurmas();
    }

    @Operation(summary = "Cadastra disciplinas")
    @PostMapping("/disciplinas")
    @ResponseStatus(HttpStatus.CREATED)
    public DisciplinaResponse cadastrarDisciplina(@Valid @RequestBody DisciplinaRequest request) {
        return adminService.cadastrarDisciplina(request);
    }

    @Operation(summary = "Lista disciplinas")
    @GetMapping("/disciplinas")
    public List<DisciplinaResponse> listarDisciplinas() {
        return adminService.listarDisciplinas();
    }

    @Operation(summary = "Cadastra uma matrícula")
    @PostMapping("/matriculas")
    @ResponseStatus(HttpStatus.CREATED)
    public MatriculaResponse cadastrarMatricula(@Valid @RequestBody MatriculaRequest request) {
        return adminService.cadastrarMatricula(request);
    }

    @Operation(summary = "Lista matrículas")
    @GetMapping("/matriculas")
    public List<MatriculaResponse> listarMatriculas() {
        return adminService.listarMatriculas();
    }

    @Operation(summary = "Cadastra um vínculo de professor, turma e disciplina")
    @PostMapping("/atribuicoes")
    @ResponseStatus(HttpStatus.CREATED)
    public AtribuicaoDocenteResponse cadastrarAtribuicao(@Valid @RequestBody AtribuicaoDocenteRequest request) {
        return adminService.cadastrarAtribuicao(request);
    }

    @Operation(summary = "Lista atribuições docentes")
    @GetMapping("/atribuicoes")
    public List<AtribuicaoDocenteResponse> listarAtribuicoes() {
        return adminService.listarAtribuicoes();
    }
}
