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

Para as demais rotas, envie `Authorization: Bearer <token>`. O filtro aplica as permissões abaixo. Nesta etapa, uma rota acadêmica autorizada retorna `404` até o respectivo controller ser implementado.

| Rotas | Perfis autorizados |
| --- | --- |
| `/api/admin/**` | Gestão |
| `/api/diario/**` | Professor e Gestão |
| `/api/aluno/**` | Aluno |

Importe `Sanquim-Login.postman_collection.json` no Postman, preencha a variável `senhaDemo` e execute a coleção na ordem indicada. Ela valida os três logins, o `403` do aluno ao tentar registrar presença e o `401` de uma requisição sem token.

Execute `.\mvnw.cmd test` para rodar os testes de integração com banco H2 em memória.

## Chave JWT

Sem configuração adicional, a aplicação gera uma chave aleatória ao iniciar. Reiniciar invalida os tokens emitidos com essa chave. Para manter os tokens válidos entre reinicializações ou compartilhar uma chave entre instâncias, configure `SANQUIM_JWT_SECRET` com pelo menos 32 bytes aleatórios codificados em Base64.
