# API de Chamados do IFRN

API REST para centralizar os pedidos de suporte do IFRN, substituindo os avisos por WhatsApp, e-mail ou conversa no corredor. Um **solicitante** abre um chamado informando o problema, o local e a categoria. O chamado vai para o **setor** responsável (COTIC para TI, Manutenção para infraestrutura), onde um **técnico** o assume, registra o andamento e o encerra. O solicitante acompanha tudo pelo histórico de interações do chamado.

Projeto individual da disciplina de Desenvolvimento de Sistemas Corporativos (TADS · IFRN Campus Pau dos Ferros · 2026.2).

## Tecnologias

- Java 21 e Spring Boot 4 (Maven)
- Spring Web MVC, Spring Data JPA, Bean Validation
- PostgreSQL 17 com migrations Flyway
- Lombok
- Docker (Dockerfile multi-stage) e Docker Compose
- JUnit 5 e Mockito nos testes das regras de negócio

## Como executar

O único pré-requisito é ter o Docker instalado.

```bash
git clone <url-do-repositorio>
cd <pasta-do-repositorio>
docker compose up
```

O Compose sobe o PostgreSQL, espera o banco ficar saudável, compila a API dentro do container e a inicia. O Flyway cria as tabelas e insere os dados iniciais. A API fica disponível em **http://localhost:8080**.

- Os dados ficam no volume `chamados-data` e continuam lá entre reinicializações.
- Para recomeçar com o banco vazio: `docker compose down -v && docker compose up`.
- As credenciais do banco vêm de variáveis de ambiente (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`). O `application.properties` não tem senha fixa. O `docker-compose.yml` define valores padrão para desenvolvimento, que podem ser trocados criando um `.env` a partir do `.env.example`.

### Rodar fora do Docker (opcional)

Com um PostgreSQL local e Java 21:

```bash
DB_URL=jdbc:postgresql://localhost:5432/chamados DB_USERNAME=chamados DB_PASSWORD=chamados ./mvnw spring-boot:run
```

Testes: `./mvnw test`

## Arquitetura

```
src/main/java/br/edu/ifrn/chamados
├── controller   recebe a requisição, chama o Service e devolve a resposta
├── service      regras de negócio e transações
├── repository   interfaces JpaRepository com filtros em @Query / métodos derivados
├── model        entidades JPA e enums
├── dto          records de entrada (com validação) e de saída
└── exception    exceções próprias e o @RestControllerAdvice
src/main/resources/db/migration   migrations Flyway (V1, V2, ...)
```

- Os Controllers não têm regra de negócio.
- A injeção de dependência é feita por construtor (`final` + `@RequiredArgsConstructor`), sem `@Autowired` em atributo.
- As entidades nunca são expostas diretamente: os Services devolvem DTOs.
- Relacionamentos: `Setor 1—N Categoria`, `Setor 1—N Usuario (técnicos)`, `Chamado N—1 Usuario (solicitante e técnico)`, `Chamado N—1 Categoria` e `Chamado 1—N Interacao N—1 Usuario (autor)`. Os enums são gravados com `@Enumerated(EnumType.STRING)`.

## Dados iniciais (migration V2)

| Tipo | Registros |
|---|---|
| Setores | 1 COTIC, 2 MANUT |
| Categorias | 1 Rede e Wi-Fi, 2 Computadores e Software, 3 Projetores e Multimídia (COTIC) · 4 Elétrica, 5 Climatização (MANUT) |
| Usuários | 1 Ana Souza (SOLICITANTE), 2 Bruno Lima (SOLICITANTE), 3 Carla Dias (TECNICO · COTIC), 4 Diego Alves (TECNICO · MANUT) |

## Endpoints

### Cadastros básicos

| Método | Rota | Descrição |
|---|---|---|
| POST | `/setores` | cria setor (sigla única, gravada em maiúsculas) |
| GET | `/setores` | lista paginada · filtro `nome` |
| GET | `/setores/{id}` | busca por id |
| GET | `/setores/{id}/categorias` | categorias do setor (paginado) |
| PUT | `/setores/{id}` | atualiza |
| DELETE | `/setores/{id}` | exclui (regra 8) |
| POST | `/categorias` | cria categoria |
| GET | `/categorias` | lista paginada · filtros `nome`, `setorId` |
| GET | `/categorias/{id}` | busca por id |
| PUT | `/categorias/{id}` | atualiza |
| DELETE | `/categorias/{id}` | exclui (regra 8) |
| POST | `/usuarios` | cria usuário (email duplicado → 409) |
| GET | `/usuarios` | lista paginada · filtros `nome`, `tipo`, `setorId` |
| GET | `/usuarios/{id}` | busca por id |
| PUT | `/usuarios/{id}` | atualiza |

### Chamados

| Método | Rota | Descrição | Sucesso |
|---|---|---|---|
| POST | `/chamados` | abre um chamado | 201 |
| GET | `/chamados` | lista paginada com filtros | 200 |
| GET | `/chamados/{id}` | detalha o chamado com suas interações | 200 |
| GET | `/usuarios/{id}/chamados` | chamados abertos pelo solicitante ou atribuídos ao técnico · filtro `status` | 200 |
| PATCH | `/chamados/{id}/assumir` | técnico assume (`tecnicoId`) | 200 |
| PATCH | `/chamados/{id}/resolver` | técnico resolve (`tecnicoId`, `solucao`) | 200 |
| PATCH | `/chamados/{id}/cancelar` | solicitante cancela (`solicitanteId`) | 200 |
| POST | `/chamados/{id}/interacoes` | registra interação (`autorId`, `texto`) | 201 |

Como a API não tem autenticação, quem executa a ação é informado no corpo da requisição (`solicitanteId`, `tecnicoId`, `autorId`).

### Paginação e filtros

Todas as listagens usam `Pageable`: `page`, `size` (padrão 10, máximo 50; valores maiores são reduzidos para 50) e `sort` (ex.: `sort=dataAbertura,desc`). Os filtros são opcionais e podem ser combinados. Filtro não informado é ignorado.

Filtros de `GET /chamados`: `status`, `prioridade`, `setorId`, `categoriaId`, `solicitanteId`, `tecnicoId`, `dataInicio` e `dataFim` (formato `yyyy-MM-dd`, período inclusivo nas duas pontas).

Formato da resposta:

```json
{
  "conteudo": [ ... ],
  "pagina": 0,
  "tamanho": 10,
  "totalElementos": 2,
  "totalPaginas": 1
}
```

### Regras de negócio (todas na camada Service)

1. Todo chamado nasce `ABERTO`, com `dataAbertura` preenchida pelo sistema e prioridade `MEDIA` quando não informada.
2. Só usuários `SOLICITANTE` abrem chamados.
3. Um técnico só assume chamado `ABERTO` cuja categoria seja do seu setor. O status passa a `EM_ATENDIMENTO`.
4. Só o técnico atribuído resolve, e a solução é obrigatória. O status passa a `RESOLVIDO` e `dataFechamento` é preenchida.
5. Só o próprio solicitante cancela, e apenas enquanto o chamado estiver `ABERTO`.
6. Chamados `RESOLVIDO` ou `CANCELADO` não aceitam alterações nem novas interações.
7. Só o solicitante e o técnico atribuído registram interações.
8. Não é possível excluir setor ou categoria que tenha chamados vinculados.

Ciclo de vida permitido: `ABERTO → EM_ATENDIMENTO → RESOLVIDO` e `ABERTO → CANCELADO`. Qualquer outra transição retorna 409.

Decisões adicionais:

- Um usuário `TECNICO` precisa de `setorId`. Para `SOLICITANTE`, o setor é ignorado.
- Um setor que ainda tem técnicos vinculados também não pode ser excluído (409). Se o setor não tem chamados nem técnicos, as categorias dele são excluídas junto.
- O tipo de um usuário que já participa de chamados não pode ser alterado, para manter o histórico consistente.
- O cancelamento também preenche `dataFechamento`, registrando quando o chamado foi encerrado.

### Respostas de erro

| Código | Quando |
|---|---|
| 400 | dados inválidos: campo obrigatório vazio, email mal formatado, enum ou data inválida, ordenação por campo inexistente |
| 404 | recurso inexistente |
| 409 | violação de regra de negócio: transição de status inválida, técnico de outro setor, email ou sigla duplicados, exclusão bloqueada |

Todos os erros são gerados pelo `GlobalExceptionHandler` (`@RestControllerAdvice`) e seguem o mesmo formato:

```json
{
  "timestamp": "2026-10-27T12:46:56.504148",
  "status": 409,
  "mensagem": "O técnico pertence ao setor MANUT, mas a categoria do chamado é do setor COTIC",
  "caminho": "/chamados/1/assumir"
}
```

## Exemplos de requisição

A coleção completa, com todas as rotas e os casos de erro de cada regra, está em [`requisicoes/chamados-api.http`](requisicoes/chamados-api.http). Ela funciona com a extensão REST Client do VS Code ou com o cliente HTTP do IntelliJ. Exemplos com `curl`:

```bash
# Abrir um chamado (prioridade padrão MEDIA)
curl -X POST http://localhost:8080/chamados -H "Content-Type: application/json" -d '{
  "titulo": "Wi-Fi do laboratório caiu",
  "descricao": "Nenhum computador do Lab 3 conecta na rede sem fio.",
  "local": "Laboratório 3 - Bloco B",
  "categoriaId": 1,
  "solicitanteId": 1
}'

# Técnica da COTIC assume o chamado
curl -X PATCH http://localhost:8080/chamados/1/assumir -H "Content-Type: application/json" -d '{"tecnicoId": 3}'

# Solicitante registra uma interação
curl -X POST http://localhost:8080/chamados/1/interacoes -H "Content-Type: application/json" \
  -d '{"autorId": 1, "texto": "O problema continua desde ontem."}'

# Técnica resolve informando a solução
curl -X PATCH http://localhost:8080/chamados/1/resolver -H "Content-Type: application/json" \
  -d '{"tecnicoId": 3, "solucao": "Access point substituído."}'

# Detalhe do chamado com o histórico de interações
curl http://localhost:8080/chamados/1

# Listagem filtrada e paginada
curl "http://localhost:8080/chamados?status=RESOLVIDO&setorId=1&prioridade=MEDIA&page=0&size=10&sort=dataAbertura,desc"
```

## Migrations com Flyway

O esquema do banco é versionado com Flyway em `src/main/resources/db/migration`. Cada arquivo segue o padrão `V<versão>__<descricao>.sql` (`V1__criar_tabelas.sql`, `V2__inserir_dados_iniciais.sql`). O Hibernate não cria nem altera tabelas: com `spring.jpa.hibernate.ddl-auto=validate`, ele só confere na inicialização se as entidades batem com o esquema criado pelas migrations. Se não baterem, a aplicação não sobe.

**Como o Flyway registra as versões.** Na primeira execução, o Flyway cria a tabela `flyway_schema_history` no banco. Cada migration aplicada vira uma linha com a versão, a descrição, o nome do script, o **checksum** (um CRC32 do conteúdo do arquivo), o usuário, a data, o tempo de execução e se teve sucesso. Nas inicializações seguintes, o Flyway compara os arquivos do projeto com essa tabela e executa, em ordem, só as versões que ainda não estão registradas. Assim, cada script roda uma única vez em cada banco. Consulta feita neste projeto:

```
 installed_rank | version |      description       |  checksum   | success
----------------+---------+------------------------+-------------+---------
              1 | 1       | criar tabelas          |   966233627 | t
              2 | 2       | inserir dados iniciais | -2141119940 | t
```

**O que acontece quando o checksum muda.** Antes de migrar, o Flyway recalcula o checksum de cada arquivo já aplicado e compara com o valor salvo na tabela. Se alguém editar uma migration já executada, nem que seja um espaço, os valores deixam de bater. A validação falha com `Migration checksum mismatch for migration version X` e a aplicação não inicia. A trava existe porque o banco já foi alterado pela versão antiga do script: a versão nova nunca rodaria ali, e os ambientes ficariam diferentes sem ninguém perceber. Por isso, migration aplicada nunca é editada. Toda correção entra como uma nova versão (`V3__...`). O comando `flyway repair` atualiza os checksums da tabela, mas só deve ser usado quando se sabe exatamente por que o arquivo mudou.

**Por que não usar `ddl-auto=update` em produção.** Com `update`, o Hibernate tenta deduzir as mudanças comparando as entidades com o banco a cada inicialização, e isso traz vários problemas:

- não fica histórico nem versão do que foi alterado, e não há como revisar o SQL antes de ele rodar;
- ele só acrescenta: não remove nem renomeia colunas, e um campo renomeado vira uma coluna nova, com os dados presos na antiga;
- não migra dados e não lida bem com mudança de tipo ou de restrições de colunas já existentes;
- cada ambiente pode acabar com um esquema diferente, conforme a ordem em que as versões da aplicação foram implantadas;
- um erro na entidade pode alterar o banco de produção direto, sem revisão e sem rollback.

Com migrations, toda mudança de esquema é um script SQL explícito, revisado e versionado no Git, e é aplicado da mesma forma em todos os bancos.
