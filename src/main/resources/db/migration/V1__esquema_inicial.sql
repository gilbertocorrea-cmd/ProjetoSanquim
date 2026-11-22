-- Estrutura conferida no MySQL 8.0.45 gerado pelas entidades atuais.
-- Role usa VARCHAR com @EnumeratedValue. Gestão é persistida como ROLE_GESTOR.
CREATE TABLE usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT UKkfsp0s1tflm1cwlj8idhqsad0 UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE gestores (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cargo VARCHAR(255) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    usuario_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT UKksewnxtn9aut15nu92bxuwv3h UNIQUE (usuario_id),
    CONSTRAINT FK689x3ny3yse1dswgxlfeailx5 FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE=InnoDB;

CREATE TABLE professores (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(255) NOT NULL,
    usuario_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT UKnlbu14lrbefsl1f264xoj37b6 UNIQUE (usuario_id),
    CONSTRAINT FKhlhqi3cmarjjyno77byxjse9m FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE=InnoDB;

CREATE TABLE alunos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(255) NOT NULL,
    usuario_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT UKqswsnqp4vhi83nrrxk8k4nxoh UNIQUE (usuario_id),
    CONSTRAINT FKhav62e6j9s3fr5k7e8q6py0d9 FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE=InnoDB;

CREATE TABLE turmas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ano_letivo INT NOT NULL,
    ativa BIT(1) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE disciplinas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE matriculas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ativa BIT(1) NOT NULL,
    aluno_id BIGINT NOT NULL,
    turma_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_matriculas_vinculo UNIQUE (aluno_id, turma_id),
    CONSTRAINT FK1o9091wci3fkpenss0fj38w3a FOREIGN KEY (turma_id) REFERENCES turmas (id),
    CONSTRAINT FKrwsrt4qf6q7fghkawume7g1ya FOREIGN KEY (aluno_id) REFERENCES alunos (id)
) ENGINE=InnoDB;

CREATE TABLE atribuicoes_docentes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    disciplina_id BIGINT NOT NULL,
    professor_id BIGINT NOT NULL,
    turma_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_atribuicoes_docentes_vinculo UNIQUE (professor_id, turma_id, disciplina_id),
    CONSTRAINT FK7692angy0ceeo0g2fgvrl1ool FOREIGN KEY (disciplina_id) REFERENCES disciplinas (id),
    CONSTRAINT FKgjmp8vomdefu692m03s0jl163 FOREIGN KEY (professor_id) REFERENCES professores (id),
    CONSTRAINT FKobav4echrdxpmfaqd5xtuq1yl FOREIGN KEY (turma_id) REFERENCES turmas (id)
) ENGINE=InnoDB;
