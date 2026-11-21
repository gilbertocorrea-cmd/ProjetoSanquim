# API REST do Colégio Sanquim

## Executar o login localmente

Use Java 17 ou 21 e um banco MySQL local chamado `sanquim`. O arquivo `src/main/resources/application.properties` contém a configuração da conexão. Defina `SPRING_DATASOURCE_PASSWORD` com a senha do usuário do banco, como na configuração de execução do IntelliJ.

Para criar as contas de demonstração em um banco local de desenvolvimento, defina mais duas variáveis na configuração do IntelliJ e execute `ProjetoSanquimApplication`.

| Variável | Valor |
| --- | --- |
| `SANQUIM_DEMO_SEED` | `true` |
| `SANQUIM_DEMO_PASSWORD` | Uma senha escolhida para as contas de teste, com até 72 bytes em UTF-8 |

No PowerShell, com as mesmas variáveis definidas na sessão, execute `.\mvnw.cmd spring-boot:run` na raiz do projeto.

| Perfil | E-mail |
| --- | --- |
| `ROLE_GESTAO` | `admin@sanquim.br` |
| `ROLE_PROFESSOR` | `professor1@sanquim.br` e `professor2@sanquim.br` |
| `ROLE_ALUNO` | `aluno1@sanquim.br` a `aluno5@sanquim.br` |

Use a senha de demonstração configurada na primeira criação das contas. A carga é opcional e preserva usuários existentes, incluindo suas senhas. Esta etapa cria as contas e os vínculos com seus perfis. A carga de turmas, matrículas, chamadas e avaliações acompanha os módulos acadêmicos.

## Testar a API

Abra o [Swagger UI local](http://localhost:8080/swagger-ui.html) e execute `POST /api/auth/login` com `email` e `senha`. A resposta de sucesso contém `token`, `expiraEm` (data e hora UTC) e `role`. O token vale uma hora.

Para as demais rotas, envie `Authorization: Bearer <token>`. O filtro aplica as permissões abaixo. O diário e as consultas do aluno ainda serão implementados nas próximas etapas.

| Rotas | Perfis autorizados |
| --- | --- |
| `/api/admin/**` | Gestão |
| `/api/diario/**` | Professor e Gestão |
| `/api/aluno/**` | Aluno |

Importe `Sanquim-Login.postman_collection.json` no Postman, preencha a variável `senhaDemo` e execute a coleção na ordem indicada. Ela valida os três logins, o `403` do aluno ao tentar registrar presença e o `401` de uma requisição sem token.

Execute `.\mvnw.cmd test` para rodar os testes de integração com banco H2 em memória.

## Cadastros de Gestão

As rotas abaixo aceitam `POST` para cadastrar e `GET` para listar. O cadastro retorna `201`, e a listagem retorna `200`. Todas exigem token de `ROLE_GESTAO`.

| Rota | Campos do cadastro |
| --- | --- |
| `/api/admin/professores` | `nome`, `email`, `senha` |
| `/api/admin/alunos` | `nome`, `email`, `senha` |
| `/api/admin/gestores` | `nome`, `cargo`, `email`, `senha` |
| `/api/admin/turmas` | `nome`, `anoLetivo`, `ativa` |
| `/api/admin/disciplinas` | `nome` |
| `/api/admin/matriculas` | `alunoId`, `turmaId` |
| `/api/admin/atribuicoes` | `professorId`, `turmaId`, `disciplinaId` |

O servidor define o perfil a partir do cadastro escolhido. Cada nova conta pode receber sua própria senha, independente da senha das contas de demonstração. As respostas incluem o ID do perfil e o `usuarioId`. Use o ID do perfil nos vínculos de matrícula e atribuição docente.

E-mails duplicados, matrículas repetidas, atribuições repetidas e vínculos com turmas inativas retornam `409`. Referências a IDs inexistentes retornam `404`. Campos obrigatórios ausentes ou inválidos retornam `400` com `camposInvalidos`.

### Roteiro para testar no Swagger

1. Pare e inicie `ProjetoSanquimApplication` no IntelliJ. Atualize o [Swagger UI](http://localhost:8080/swagger-ui.html).
2. Execute `POST /api/auth/login` com `admin@sanquim.br` e a senha que já funcionou. Copie somente o valor de `token`.
3. Clique em **Authorize**, cole o token no campo de `bearerAuth`, clique em **Authorize** e depois em **Close**. O Swagger acrescenta o prefixo Bearer automaticamente.
4. Abra o grupo **Gestão**, use **Try it out** e execute os cadastros abaixo, um de cada vez. Confira o código real em **Server response** e anote o `id` de cada resposta.

Professor, em `POST /api/admin/professores`

```json
{
  "nome": "Professor de teste",
  "email": "professor.novo@sanquim.br",
  "senha": "SenhaTeste123!"
}
```

Aluno, em `POST /api/admin/alunos`

```json
{
  "nome": "Aluno de teste",
  "email": "aluno.novo@sanquim.br",
  "senha": "SenhaAluno123!"
}
```

Turma, em `POST /api/admin/turmas`

```json
{
  "nome": "1º Ano EM - Turma B",
  "anoLetivo": 2026,
  "ativa": true
}
```

Disciplina, em `POST /api/admin/disciplinas`

```json
{
  "nome": "Matemática"
}
```

5. Execute `POST /api/admin/matriculas` com os campos `alunoId` e `turmaId`, usando os IDs que você anotou. A matrícula criada terá `ativa: true`.
6. Execute `POST /api/admin/atribuicoes` com os campos `professorId`, `turmaId` e `disciplinaId`, usando os IDs anotados.
7. Execute os `GET` de professores, alunos, turmas, disciplinas, matrículas e atribuições. Confira os cadastros nas listas.
8. Repita o cadastro do mesmo aluno e a mesma matrícula. Ambos devem retornar `409`.
9. Faça login com `aluno.novo@sanquim.br` e `SenhaAluno123!`. Em **Authorize**, use **Logout** para remover o token de Gestão e autorize com o novo token. `GET /api/admin/alunos` deve retornar `403`. Repita com a conta de professor para conferir a mesma restrição.
10. Use **Logout** em **Authorize** e execute `GET /api/admin/alunos` novamente. O resultado esperado é `401`.

Os e-mails dos exemplos devem estar disponíveis na primeira execução. Se já os cadastrou, use outro e-mail para criar uma conta adicional ou consulte os registros pelos endpoints de listagem.

A coleção do Postman também contém a pasta **Gestão**, com login, cadastros encadeados, captura automática dos IDs e verificações das respostas.

## Chave JWT

Sem configuração adicional, a aplicação gera uma chave aleatória ao iniciar. Reiniciar invalida os tokens emitidos com essa chave. Para manter os tokens válidos entre reinicializações ou compartilhar uma chave entre instâncias, configure `SANQUIM_JWT_SECRET` com pelo menos 32 bytes aleatórios codificados em Base64.
