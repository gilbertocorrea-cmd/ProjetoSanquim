package com.lab.projetosanquim.dto;

import java.util.List;

public record BoletimResponse(Long alunoId, List<DisciplinaBoletimResponse> disciplinas) {
}
