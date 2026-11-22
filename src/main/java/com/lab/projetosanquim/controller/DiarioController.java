package com.lab.projetosanquim.controller;

import com.lab.projetosanquim.dto.*;
import com.lab.projetosanquim.security.UsuarioDetails;
import com.lab.projetosanquim.service.DiarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diario")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Diário de Classe", description = "Lançamentos de Professor e Gestão")
public class DiarioController {
    private final DiarioService diarioService;

    @Operation(summary = "Lista turmas e disciplinas do professor autenticado")
    @GetMapping("/minhas-turmas")
    public List<MinhasTurmasResponse> minhasTurmas(@AuthenticationPrincipal UsuarioDetails usuario) {
        return diarioService.minhasTurmas(usuario);
    }

    @Operation(summary = "Registra presença e ausência em lote")
    @PostMapping("/chamada")
    @ResponseStatus(HttpStatus.CREATED)
    public ChamadaResponse registrarChamada(@Valid @RequestBody ChamadaRequest request,
                                            @AuthenticationPrincipal UsuarioDetails usuario) {
        return diarioService.registrarChamada(request, usuario);
    }

    @Operation(summary = "Cadastra uma avaliação")
    @PostMapping("/avaliacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public AvaliacaoResponse cadastrarAvaliacao(@Valid @RequestBody AvaliacaoRequest request,
                                               @AuthenticationPrincipal UsuarioDetails usuario) {
        return diarioService.cadastrarAvaliacao(request, usuario);
    }

    @Operation(summary = "Lança ou atualiza a nota de um aluno")
    @PostMapping("/avaliacoes/{avaliacaoId}/notas")
    public NotaResponse lancarNota(@PathVariable Long avaliacaoId, @Valid @RequestBody NotaRequest request,
                                   @AuthenticationPrincipal UsuarioDetails usuario) {
        return diarioService.lancarNota(avaliacaoId, request, usuario);
    }

    @Operation(summary = "Registra os tópicos de uma aula")
    @PostMapping("/planos-aula")
    @ResponseStatus(HttpStatus.CREATED)
    public AulaResponse registrarPlanoAula(@Valid @RequestBody PlanoAulaRequest request,
                                          @AuthenticationPrincipal UsuarioDetails usuario) {
        return diarioService.registrarPlanoAula(request, usuario);
    }
}
