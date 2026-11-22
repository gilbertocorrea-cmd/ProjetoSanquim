CREATE TABLE chamadas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    turma_id BIGINT NOT NULL,
    disciplina_id BIGINT NOT NULL,
    data_aula DATE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_chamada_turma_disciplina_data UNIQUE (turma_id, disciplina_id, data_aula),
    CONSTRAINT fk_chamada_turma FOREIGN KEY (turma_id) REFERENCES turmas(id),
    CONSTRAINT fk_chamada_disciplina FOREIGN KEY (disciplina_id) REFERENCES disciplinas(id)
) ENGINE=InnoDB;

CREATE TABLE frequencias (
    id BIGINT NOT NULL AUTO_INCREMENT,
    chamada_id BIGINT NOT NULL,
    aluno_id BIGINT NOT NULL,
    presente BIT(1) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_frequencia_chamada_aluno UNIQUE (chamada_id, aluno_id),
    CONSTRAINT fk_frequencia_chamada FOREIGN KEY (chamada_id) REFERENCES chamadas(id),
    CONSTRAINT fk_frequencia_aluno FOREIGN KEY (aluno_id) REFERENCES alunos(id)
) ENGINE=InnoDB;

CREATE TABLE avaliacoes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    turma_id BIGINT NOT NULL,
    disciplina_id BIGINT NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    tipo VARCHAR(255) NOT NULL,
    nota_maxima DECIMAL(19,4) NOT NULL,
    peso DECIMAL(19,4) NOT NULL,
    data_entrega DATE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_avaliacao_turma FOREIGN KEY (turma_id) REFERENCES turmas(id),
    CONSTRAINT fk_avaliacao_disciplina FOREIGN KEY (disciplina_id) REFERENCES disciplinas(id)
) ENGINE=InnoDB;

CREATE TABLE notas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    avaliacao_id BIGINT NOT NULL,
    aluno_id BIGINT NOT NULL,
    valor DECIMAL(19,4) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_nota_avaliacao_aluno UNIQUE (avaliacao_id, aluno_id),
    CONSTRAINT fk_nota_avaliacao FOREIGN KEY (avaliacao_id) REFERENCES avaliacoes(id),
    CONSTRAINT fk_nota_aluno FOREIGN KEY (aluno_id) REFERENCES alunos(id)
) ENGINE=InnoDB;

CREATE TABLE planos_aula (
    id BIGINT NOT NULL AUTO_INCREMENT,
    turma_id BIGINT NOT NULL,
    disciplina_id BIGINT NOT NULL,
    data_aula DATE NOT NULL,
    topicos LONGTEXT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_plano_turma FOREIGN KEY (turma_id) REFERENCES turmas(id),
    CONSTRAINT fk_plano_disciplina FOREIGN KEY (disciplina_id) REFERENCES disciplinas(id)
) ENGINE=InnoDB;
