# API REST do Colégio Sanquim

## Obter e configurar o projeto

No PowerShell, clone o repositório e entre na pasta criada.

```powershell
git clone https://github.com/victormourahs/ProjetoSanquim.git
cd ProjetoSanquim
```

No IntelliJ, abra a pasta como projeto Maven e selecione Java 17 ou 21. O Maven Wrapper está incluído no repositório.

Crie o banco local `sanquim` no MySQL, caso ainda não exista. Use uma conta de administração do seu MySQL para conceder ao usuário da aplicação acesso a esse banco. Configure `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD` na execução do IntelliJ conforme sua instalação. Os padrões de URL e usuário estão em `application.properties`. No desenvolvimento, Hibernate cria e atualiza as tabelas ao iniciar.

```sql
CREATE DATABASE IF NOT EXISTS sanquim CHARACTER SET utf8mb4;
```

Depois siga a configuração das contas de demonstração abaixo. Para uma instalação com perfil `prod`, consulte a seção de migrações antes de executar.

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

Use a senha de demonstração configurada na primeira criação das contas. A carga é opcional e preserva usuários existentes, incluindo suas senhas. Com SANQUIM_DEMO_SEED=true, a carga também cria a turma Demonstração Sanquim, atribui Matemática ao professor1 e História ao professor2, enturma os cinco alunos e inclui uma chamada e uma avaliação com notas de demonstração. Reiniciar preserva os registros e notas já existentes.

Com o perfil `prod` ativo, `SANQUIM_DEMO_SEED=true` impede a inicialização. Os dois seeders verificam o perfil antes de executar a carga e falham com `A carga de demonstração não pode ser executada no perfil prod`. Em produção mantenha a carga desativada. Fora de `prod`, a configuração e o comportamento da demonstração permanecem os mesmos.

## Testar a API

Abra o [Swagger UI local](http://localhost:8080/swagger-ui.html) e execute `POST /api/auth/login` com `email` e `senha`. A resposta de sucesso contém `token`, `expiraEm` (data e hora UTC) e `role`. O token vale uma hora.

Para as demais rotas, envie `Authorization: Bearer <token>`. O filtro aplica as permissões abaixo. O Diário de Classe e as consultas privadas do aluno já estão disponíveis.

| Rotas | Perfis autorizados |
| --- | --- |
| `/api/admin/**` | Gestão |
| `/api/diario/**` | Professor e Gestão |
| `/api/aluno/**` | Aluno |

Importe `Sanquim-Login.postman_collection.json` no Postman, preencha a variável `senhaDemo` e execute a coleção na ordem indicada. Ela valida os três logins, o `403` do aluno ao tentar registrar presença e o `401` de uma requisição sem token.

Execute `.\mvnw.cmd test` para rodar os testes de integração com banco H2 em memória.

## Validar o fluxo completo do MVP

O teste `FluxoEscolarTests` acompanha uma operação escolar completa, usando logins reais e o JWT retornado pela API. Ele usa um banco H2 separado, cria somente o gestor inicial diretamente e realiza os demais cadastros e lançamentos pelas rotas HTTP. Cada requisição conclui sua própria transação.

| Etapa | Verificação automatizada |
| --- | --- |
| Gestão | Login, cadastro de professor, alunos, turma e disciplina, matrícula e atribuição docente |
| Professor | Login, consulta da atribuição, chamada com falta, rejeição da chamada duplicada e plano de aula |
| Avaliações | Duas notas com máximas e pesos diferentes, uma avaliação pendente e atualização de nota |
| Aluno | Login, boletim atualizado, falta acumulada e conteúdo da aula |
| Acesso | Outro aluno sem acesso aos resultados, aluno bloqueado na Gestão, professor bloqueado no boletim e requisição sem token rejeitada |

Para executar apenas esse fluxo no Windows, use `.\mvnw.cmd -Dtest=FluxoEscolarTests test`. A suíte completa continua disponível com `.\mvnw.cmd test`. Os testes de produção com MySQL exigem a configuração descrita na seção de migrações.

Para repetir no Swagger com seus dados de demonstração, siga os roteiros de Gestão, Diário de Classe e Consultas do Aluno deste README, nesta ordem. Use uma data de chamada ainda não registrada. Cadastre uma avaliação com nota máxima 20 e peso 2 e outra com nota máxima 10 e peso 1. Lance, para o mesmo aluno, as notas 12 e 10. O boletim deve apresentar média `7.3333`. Atualize a primeira nota para 16 e consulte novamente, esperando `8.6667`. Uma terceira avaliação sem nota lançada deve aparecer como `NAO_LANCADA` e preservar a média.

Confira também uma falta registrada na chamada e os tópicos do plano de aula, com data até hoje. Com o token do aluno, tente listar alunos na Gestão e confira `403`. Este roteiro altera os dados locais que você cadastrar e lançar.

## Cadastros de Gestão

O controller delega aos serviços de Gestor, Professor, Aluno, Turma, Disciplina, Matrícula e Atribuição Docente. A criação de usuário e a busca de turma ativa são compartilhadas. Cada cadastro preserva sua transação, inclusive o rollback do usuário quando a gravação do perfil falha. Os DTOs continuam convertidos pelos métodos estáticos `from(...)`.

As rotas abaixo aceitam `POST` para cadastrar e `GET` para listar. O cadastro retorna `201`, e a listagem retorna `200`. Todas exigem token de `ROLE_GESTAO`.

Os sete GETs aceitam `page` (a partir de 0, padrão 0) e `size` (de 1 a 100, padrão 20). A ordenação é sempre por ID crescente e ignora `sort`. Parâmetros vazios, não inteiros, negativos ou fora do intervalo suportado retornam 400 em ErroResponse.

A resposta tem `conteudo`, `pagina`, `tamanho`, `totalElementos` e `totalPaginas`. Os registros ficam em `conteudo`. Exemplo de consulta, `GET /api/admin/alunos?page=0&size=20`. Uma página além dos dados retorna conteúdo vazio e preserva os totais.

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

Nos cadastros de gestores, professores e alunos, a senha é validada por `@MaxBytes` e aceita até 72 bytes em UTF-8. Por exemplo, 36 letras é ou 18 emojis 😀 ocupam 72 bytes. Acima disso, a resposta é 400 e `camposInvalidos` contém o campo `senha`. No login, senhas ausentes, vazias ou acima do limite retornam 401 com a mensagem `E-mail ou senha inválidos`, sujeito ao limite de tentativas. A regra de bytes é compartilhada com a autenticação e com a carga de demonstração.

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

## Diário de Classe

As rotas abaixo usam token de Professor ou Gestão. O professor só pode lançar dados para as combinações de turma e disciplina que lhe foram atribuídas. Gestão pode realizar os lançamentos em turmas ativas.

| Método e rota | Resultado |
| --- | --- |
| `GET /api/diario/minhas-turmas` | 200, turmas e disciplinas do professor autenticado |
| `POST /api/diario/chamada` | 201, chamada com registros de presença |
| `POST /api/diario/avaliacoes` | 201, avaliação criada |
| `POST /api/diario/avaliacoes/{avaliacaoId}/notas` | 200, nota criada ou atualizada |
| `POST /api/diario/planos-aula` | 201, plano de aula criado |

`minhas-turmas` consulta o vínculo de Professor do usuário logado. Uma conta de Gestão sem esse vínculo recebe lista vazia. Para consultar os cadastros gerais, Gestão usa as rotas de administração.

A chamada exige todos os alunos com matrícula ativa na turma, exatamente uma vez cada. O lote inteiro é validado antes da gravação. Se faltar algum aluno ativo, retorna 409 com a mensagem `A chamada deve incluir todos os alunos com matrícula ativa na turma`, sem gravar chamada ou frequência. As validações de aluno repetido (409), inexistente (404) e sem matrícula ativa na turma (409) continuam ocorrendo antes da verificação de completude. Uma chamada já existente para a mesma turma, disciplina e data retorna 409. A restrição única do banco também impede duplicidade em requisições simultâneas.

O corpo da chamada aceita no máximo 500 registros. Os tópicos do plano de aula aceitam até 10.000 caracteres e continuam obrigatórios. Exceder esses limites retorna 400 com `registros` ou `topicos` em `camposInvalidos`. Uma turma com mais de 500 alunos ativos não pode registrar chamada por este contrato; não divida a chamada em lotes parciais.

No lançamento de notas, a avaliação é consultada e o acesso é validado antes de obter o lock pessimista. Avaliação inexistente continua retornando 404 e professor sem atribuição, 403. Para os pedidos autorizados, o lock da avaliação continua serializando os lançamentos. A consulta da nota usa uma leitura bloqueante após esse lock para obter o estado atual, inclusive com `REPEATABLE READ` do MySQL. Esse isolamento mantém a visão da primeira leitura comum, enquanto leituras bloqueantes usam o estado mais recente, conforme a [documentação do MySQL](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-isolation-levels.html).

Na chamada, `dataAula` deve ser hoje ou uma data passada, considerando `America/Sao_Paulo`. Uma data futura retorna 400 com `dataAula` em `camposInvalidos`. O bean `Clock` é compartilhado pela validação e pelo corte de datas da consulta de aulas do aluno, e pode ser substituído por um relógio fixo nos testes. Planos de aula e datas de entrega de avaliações continuam aceitando datas futuras.

Notas aceitam zero e devem ser menores ou iguais à nota máxima. Aluno com matrícula inativa ou em outra turma é rejeitado. Repetir o lançamento para a mesma avaliação e aluno atualiza o registro existente. Notas, pesos e notas máximas usam decimal exato com até quatro casas decimais, com validação para evitar arredondamento silencioso. Peso e nota máxima devem ser positivos.

### Testar o diário no Swagger

1. Pare e inicie `ProjetoSanquimApplication` no IntelliJ e atualize o Swagger.
2. Com o token de Gestão, cadastre ou consulte um Professor, um Aluno, uma Turma ativa e uma Disciplina. Anote os IDs de cada recurso, conforme o roteiro de Gestão.
3. Em `POST /api/admin/matriculas`, associe o Aluno à Turma. Em `POST /api/admin/atribuicoes`, associe o Professor à mesma Turma e Disciplina.
4. Faça login com o e-mail e a senha desse Professor. Em **Authorize**, remova o token anterior e cole o token do Professor. Execute `GET /api/diario/minhas-turmas` e confira os IDs.

Para um teste rápido com a carga de demonstração, você também pode entrar como `professor1@sanquim.br`, usar a senha de demonstração que já funciona e consultar `minhas-turmas`. A chamada de demonstração usa 2026-11-10. Use outra data para testar um lançamento novo.
5. Em `POST /api/diario/chamada`, use o corpo abaixo. Substitua os números pelos IDs reais que você anotou e inclua todos os alunos ativos da turma em `registros`. O exemplo com um aluno só serve para uma turma que tenha exatamente um aluno ativo. Na turma de demonstração inicial são cinco alunos. Para testar o erro, em uma data ainda sem chamada, omita um aluno ativo e confira 409; depois envie o lote completo na mesma data e confira 201.

```json
{
  "turmaId": 1,
  "disciplinaId": 1,
  "dataAula": "2026-11-10",
  "registros": [
    { "alunoId": 1, "presente": true }
  ]
}
```

Use uma data passada ou o dia atual de São Paulo, ainda sem chamada nessa turma e disciplina. Confira 201 em **Server response**. Execute o mesmo corpo outra vez e confira 409. Troque somente a data para amanhã e confira 400 com `dataAula` em `camposInvalidos`.

6. Em `POST /api/diario/avaliacoes`, use os mesmos IDs de turma e disciplina e anote o ID da avaliação retornada.

```json
{
  "turmaId": 1,
  "disciplinaId": 1,
  "titulo": "Prova 1",
  "tipo": "PROVA",
  "notaMaxima": 10,
  "peso": 1,
  "dataEntrega": "2026-11-10"
}
```

7. Em `POST /api/diario/avaliacoes/{avaliacaoId}/notas`, preencha o ID da avaliação e use o ID real do aluno.

```json
{
  "alunoId": 1,
  "valor": 8
}
```

Confira 200. Troque o valor para 9 e execute novamente. O ID da nota deve permanecer igual. Com valor 11 e nota máxima 10, o resultado esperado é 409.

8. Em `POST /api/diario/planos-aula`, use os mesmos IDs.

```json
{
  "turmaId": 1,
  "disciplinaId": 1,
  "dataAula": "2026-11-10",
  "topicos": "Equações do primeiro grau"
}
```

Confira 201. Com outro Professor sem atribuição para essa turma e disciplina, os lançamentos devem retornar 403. Com token de Aluno, todas as rotas do diário devem retornar 403.

A pasta **Diário de Classe** do Postman usa os IDs criados pela pasta **Gestão**, autentica o Professor criado e executa essa sequência. Execute Gestão antes dela.

Em desenvolvimento, Hibernate cria as tabelas novas com ddl-auto=update. Em prod, Flyway aplica `V2__diario_classe.sql` e Hibernate valida o esquema. Para adoção inicial de um banco existente, baseline 1 pressupõe o esquema V1. Se as tabelas do diário já foram criadas pelo Hibernate, a adoção deve considerar o esquema V2 existente e seu histórico, evitando reaplicar a criação das tabelas.

## Consultas do Aluno

As três consultas são exclusivas de ROLE_ALUNO e usam o aluno associado ao usuário do token. Elas não recebem ID de aluno. Um parâmetro adicional alunoId é ignorado e não muda o proprietário dos dados. Professor e Gestão recebem 403 nessas rotas.

| Método e rota | Resultado |
| --- | --- |
| `GET /api/aluno/boletim` | Notas por turma e disciplina, situação de cada avaliação e média parcial |
| `GET /api/aluno/frequencia` | Presenças, faltas, percentual global e detalhamento por turma e disciplina |
| `GET /api/aluno/aulas` | Conteúdos das aulas das matrículas ativas, com data até hoje |

O boletim usa a regra aprovada de normalização para 0 a 10 e ponderação pelos pesos. A fórmula é `sum((valor / notaMaxima) * 10 * peso) / sum(pesos das notas lançadas)`. O cálculo usa BigDecimal, com precisão intermediária DECIMAL128. As notas normalizadas e a média são apresentadas com quatro casas decimais e arredondamento HALF_UP. O arredondamento de apresentação de cada nota não é usado no cálculo da média.

Nota ainda não lançada tem situação NAO_LANCADA, valor e notaNormalizada nulos e fica fora da média. Nota zero lançada tem situação LANCADA, valor zero e seu peso entra na média. Uma disciplina sem notas lançadas tem mediaParcial nula.

Avaliações das matrículas ativas aparecem no boletim. Notas próprias de turmas anteriores também são preservadas no histórico, separadas por turma e ano letivo. Avaliações sem nota de uma matrícula inativa ficam fora dessa consulta.

A frequência considera somente os registros individuais do aluno. Uma chamada em que ele não tem registro não é convertida automaticamente em falta. O percentual é `presencas * 100 / aulasRegistradas`, apresentado com quatro casas decimais e HALF_UP. O histórico inclui registros próprios de matrículas anteriores. Sem registros, os totais são zero, as listas são vazias e percentualGlobal é nulo.

As aulas usam matrículas ativas e dataAula menor ou igual à data atual em `America/Sao_Paulo`, obtida pelo mesmo bean `Clock` da validação da chamada. A ordem é dataAula decrescente e, em caso de empate, ID decrescente. A consulta preserva o formato AulaResponse usado no diário.

### Testar as consultas no Swagger

1. Pare e inicie a aplicação no IntelliJ e atualize o Swagger.
2. Faça login como `aluno1@sanquim.br` com a senha de demonstração que já funciona.
3. Copie somente o token retornado. Em **Authorize**, remova a autorização anterior e cole o novo token.
4. No grupo **Aluno**, execute os três GETs sem corpo. Todos devem retornar 200.
5. Em boletim, confira valor, notaMaxima, peso, notaNormalizada e mediaParcial. Uma avaliação recém-criada sem lançamento de nota deve aparecer como NAO_LANCADA e não reduzir a média.
6. Em frequência, confira as presenças e faltas que você lançou no diário. Em aulas, confira os tópicos registrados para a turma do aluno.
7. Faça login como `aluno2@sanquim.br`, substitua o token e consulte novamente para conferir o isolamento dos dados.
8. Com token de Professor ou Gestão, os três GETs devem retornar 403. Sem token, devem retornar 401.

Os dados de demonstração já incluem matrícula, avaliação, notas e frequência. Para a consulta de aulas ter conteúdo, registre um plano de aula no Diário de Classe para a mesma turma, com data até hoje.

A pasta **Aluno** da coleção Postman usa as contas de demonstração, valida os três retornos e inclui testes de bloqueio por perfil. A senha continua na variável senhaDemo.

## Chave JWT

Sem configuração adicional, a aplicação gera uma chave aleatória ao iniciar. Reiniciar invalida os tokens emitidos com essa chave. Para manter os tokens válidos entre reinicializações ou compartilhar uma chave entre instâncias, configure `SANQUIM_JWT_SECRET` com pelo menos 32 bytes aleatórios codificados em Base64.

## Perfil prod e migrações

Ative o perfil Spring `prod` e forneça `SANQUIM_JWT_SECRET` em Base64 com ao menos 32 bytes aleatórios. A inicialização falha com segredo ausente, vazio, inválido ou curto. Nesse perfil, Swagger fica desativado, show-sql=false e ddl-auto=validate. No perfil padrão, permanecem Swagger público, show-sql=true, ddl-auto=update e a chave JWT aleatória quando não configurada.

O projeto usa `org.springframework.boot:spring-boot-starter-flyway` e `org.flywaydb:flyway-mysql`, com as versões gerenciadas pelo BOM do Spring Boot 4.1.2 (Flyway 12.4.0). Os artefatos foram compilados e executados com Java 17. Consulte a [documentação oficial](https://docs.spring.io/spring-boot/4.1/how-to/data-initialization.html).

Flyway está desligado no perfil padrão e nos testes H2 com create-drop. Somente prod aplica as migrações de `db/migration` antes da validação JPA. A V1 corresponde às oito tabelas administrativas iniciais, incluindo `usuarios.role VARCHAR(255)`, o valor persistido `ROLE_GESTOR`, chaves estrangeiras e restrições únicas. A V2 acrescenta as cinco tabelas do Diário de Classe.

Para banco vazio, Flyway executa V1 e V2. Para banco existente sem histórico Flyway, baseline-on-migrate=true e baseline-version=1 registram o baseline, pulam a V1 e executam a V2. Esse banco existente deve corresponder ao esquema V1 antes da adoção. Compare a estrutura e o histórico antes de ativar prod no ambiente desejado. O baseline não corrige divergências de esquema.

`ProdMySqlTests` é habilitado apenas quando `SANQUIM_MYSQL_TEST_URL` aponta para uma instância MySQL descartável, sem nome de banco. Informe também `SANQUIM_MYSQL_TEST_USER` e, se necessário, `SANQUIM_MYSQL_TEST_PASSWORD`. O teste cria bancos de nomes aleatórios, executa criação/baseline, inicia prod com validação das entidades, verifica a leitura de ROLE_GESTOR e remove somente os bancos que criou. Os testes H2 continuam usando create-drop e Flyway desativado.

`MySqlDiarioTests` usa as mesmas variáveis e exige a mesma instância descartável. Cada caso cria seu próprio banco, aplica V1/V2 em `prod` com `ddl-auto=validate` e desativa a demonstração. Ele verifica o 403 de um professor sem atribuição enquanto outra transação mantém o lock da avaliação e a atualização concorrente da mesma nota com `REPEATABLE READ`. No segundo caso, uma transação estabelece sua visão antes do commit da outra; os dois lançamentos devem retornar o mesmo ID e deixar uma única nota com o valor atualizado. Os bancos são removidos depois dos testes.

Para executar somente as verificações MySQL, configure as três variáveis na sessão que executará o Maven e use `.\mvnw.cmd '-Dtest=ProdMySqlTests,MySqlDiarioTests' test`. A URL deve identificar somente o servidor, por exemplo `jdbc:mysql://127.0.0.1:PORTA`, substituindo `PORTA` pela porta da instância descartável. Não use a instância que mantém seus dados de desenvolvimento ou produção. Sem `SANQUIM_MYSQL_TEST_URL`, os quatro casos MySQL são ignorados e a suíte H2 continua disponível. Nenhuma dessas variáveis altera a conexão da aplicação executada separadamente no IntelliJ.

## Limite de tentativas de login

Somente falhas de credenciais contam. Após 5 falhas pelo mesmo e-mail ou 20 pelo mesmo IP, a próxima tentativa retorna 429 com o corpo ErroResponse e Retry-After em segundos. Cada janela dura 15 minutos a partir da primeira falha. O e-mail é normalizado com strip e minúsculas, incluindo contas inexistentes. Um login válido zera as falhas daquele e-mail e preserva as falhas do IP. Erros 400 não contam.

Configure sanquim.auth.login-rate-limit.email-limit (5), ip-limit (20) e window (15m) sob o prefixo sanquim.auth.login-rate-limit. O IP vem de request.getRemoteAddr(). A aplicação recebe conexões diretas. Os contadores ficam na memória da instância, expiram e são limpos periodicamente. As seções críticas curtas verificam os limites, reservam uma tentativa por e-mail e IP e ajustam os contadores. A autenticação é executada fora do lock. Sucesso limpa o e-mail e devolve a reserva do IP. Falha de credencial mantém a reserva. Outras exceções devolvem as duas reservas. Conclusões antigas só ajustam entradas com o mesmo expiraEm, preservando janelas novas. Reservas em andamento ocupam temporariamente a capacidade dos respectivos contadores. Reiniciar a aplicação apaga os contadores. A interface LoginAttemptLimiter permite substituir o armazenamento futuramente.

A pasta Limite de login da coleção Postman executa seis tentativas com uma conta inexistente e verifica 401 nas cinco primeiras e 429 com Retry-After na sexta. Execute-a uma vez por janela para um e-mail e IP disponíveis.
