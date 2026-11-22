package com.lab.projetosanquim.controller;

import com.lab.projetosanquim.dto.AulaResponse;
import com.lab.projetosanquim.dto.FrequenciaResponse;
import com.lab.projetosanquim.dto.BoletimResponse;
import com.lab.projetosanquim.security.UsuarioDetails;
import com.lab.projetosanquim.service.AlunoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/aluno")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Aluno", description = "Consultas privadas do aluno autenticado")
public class AlunoController {
    private final AlunoService alunoService;

    @Operation(summary = "Consulta notas e médias parciais do aluno autenticado")
    @GetMapping("/boletim")
    public BoletimResponse boletim(@AuthenticationPrincipal UsuarioDetails usuario) {
        return alunoService.boletim(usuario);
    }

    @Operation(summary = "Consulta frequência e faltas do aluno autenticado")
    @GetMapping("/frequencia")
    public FrequenciaResponse frequencia(@AuthenticationPrincipal UsuarioDetails usuario) {
        return alunoService.frequencia(usuario);
    }

    @Operation(summary = "Consulta os conteúdos das aulas das matrículas ativas do aluno")
    @GetMapping("/aulas")
    public List<AulaResponse> aulas(@AuthenticationPrincipal UsuarioDetails usuario) {
        return alunoService.aulas(usuario);
    }
}
