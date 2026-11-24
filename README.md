# Projeto Rede Social

Projeto acadêmico desenvolvido para a disciplina de **Banco de Dados e Programação Orientada a Objetos**. A aplicação de rede social é desenvolvida em Java e utiliza PostgreSQL como sua persistência.

O sistema permite gerenciar usuários, publicações, comentários, mensagens, hashtags e relações entre essas entidades por meio de DAOs JDBC e uma interface Java Swing.

## Versões do projeto e bancos isolados

As implementações SQL e NoSQL ficam em branches separadas. A aplicação SQL desta branch usa somente PostgreSQL; a versão MongoDB tem sua própria branch, configuração e script. Os bancos têm nomes diferentes e não compartilham dados:

| Versão | Branch | Banco e script |
| --- | --- | --- |
| SQL — Fase 1 | [`fase1-postgresql`](https://github.com/tataFaucz/ProjetoRedeSocial-Omeletty/tree/fase1-postgresql) | PostgreSQL, banco `rede_social`; script `RedeSocial_PostgreSQL.sql` nesta branch |
| NoSQL — Fase 2 | [`fase2-mongodb`](https://github.com/tataFaucz/ProjetoRedeSocial-Omeletty/tree/fase2-mongodb) | MongoDB, banco `rede_social_nosql`; script `MongoDB_Init.js` na branch MongoDB |

Use uma branch por vez. Esta versão aceita `POSTGRES_*` e `DB_*`; a versão MongoDB usa `MONGODB_URI` e `MONGODB_DATABASE`. Uma versão não lê nem altera as configurações ou o banco da outra.

## Funcionalidades

- Cadastro, login e manutenção de usuários;
- Publicação e consulta de fotos, comentários e mensagens;
- Seguir usuários, curtir e compartilhar publicações;
- Associar hashtags e consultar publicações por hashtag;
- Consultas de gestão: publicações por usuário, comentários por publicação e publicações por hashtag.

## Tecnologias

- Java 11 ou superior e Java Swing;
- PostgreSQL;
- PostgreSQL JDBC Driver 42.7.7 (incluído em `lib/`);
- PowerShell ou outro terminal para compilar e executar.

## Estrutura

```text
projeto rede social/
├── dados/        # Entidades
├── dao/          # Persistência JDBC
├── negocios/     # Regras de negócio e inicialização
├── testes/       # Testes unitários e de integração
└── ui/           # Interface Swing
lib/              # Driver JDBC
RedeSocial_PostgreSQL.sql
validacao_schema_postgresql.sql
```

## Configuração do PostgreSQL

Crie o banco `rede_social` em uma instância PostgreSQL local e execute `RedeSocial_PostgreSQL.sql` nesse banco (por exemplo, pelo pgAdmin ou `psql`). O script cria as tabelas, índices, chaves estrangeiras e regras de exclusão em cascata. `validacao_schema_postgresql.sql` contém consultas para verificar o schema. A aplicação não cria nem altera o schema automaticamente.

Configure a conexão por variáveis de ambiente ou propriedades Java. São aceitos os prefixos `POSTGRES_*` e `DB_*`; quando ambos são definidos, `POSTGRES_*` tem precedência:

| Variável | Alternativa | Padrão |
| --- | --- | --- |
| `POSTGRES_URL` | `DB_URL` | URL formada abaixo |
| `POSTGRES_HOST` | `DB_HOST` | `localhost` |
| `POSTGRES_PORT` | `DB_PORT` | `5432` |
| `POSTGRES_DATABASE` | `DB_NAME` | `rede_social` |
| `POSTGRES_USER` | `DB_USER` | driver decide (não definido pela aplicação) |
| `POSTGRES_PASSWORD` | `DB_PASSWORD` | não definida |

Sem uma URL explícita, a conexão usa `jdbc:postgresql://<host>:<port>/<database>`. Sem credenciais explícitas, o driver usa seu comportamento padrão local. Configure usuário e senha apropriados para a sua instância; a aplicação não presume credenciais e não inclua segredos no repositório.

Exemplo no PowerShell (substitua usuário e senha pelos valores locais):

```powershell
$env:POSTGRES_URL = "jdbc:postgresql://localhost:5432/rede_social"
$env:POSTGRES_USER = "seu_usuario"
$env:POSTGRES_PASSWORD = "sua_senha"
```

Também é possível usar `DB_URL`, `DB_USER` e `DB_PASSWORD` no lugar das variáveis `POSTGRES_*`.

## Compilação e execução

Na raiz do repositório, no PowerShell:

```powershell
$env:JAVA_TOOL_OPTIONS = "-Xms16m -Xmx128m -XX:MarkStackSize=1024"
$sources = Get-ChildItem ".\projeto rede social" -Recurse -Filter "*.java" | ForEach-Object FullName
javac -encoding UTF-8 -cp ".\lib\postgresql-42.7.7.jar" -d ".\out" $sources
java -cp ".\out;.\lib\postgresql-42.7.7.jar" negocios.Main
```

```powershell
$env:POSTGRES_URL = "jdbc:postgresql://localhost:5433/omellety"
>> $env:POSTGRES_USER = "postgres"    
>> $securePassword = Read-Host "Senha do PostgreSQL" -AsSecureString
>> $env:POSTGRES_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
>> 
>> java -cp ".\out;.\lib\postgresql-42.7.7.jar" negocios.Main
```

No VS Code, use **Executar Rede Social** ou a tarefa **Executar projeto Java**. O driver JDBC já está incluído; não é necessário Maven.

## Testes

Os testes unitários não precisam de um banco de dados:

```powershell
$sources = Get-ChildItem ".\projeto rede social" -Recurse -Filter "*.java" | ForEach-Object FullName
javac -encoding UTF-8 -cp ".\lib\postgresql-42.7.7.jar" -d ".\out" $sources
java -cp ".\out;.\lib\postgresql-42.7.7.jar" testes.ContaEtapa1Test --unit
```

O teste de integração de cadastro/login/edição/exclusão exige um PostgreSQL configurado e conectado ao schema. Ele cria e remove registros de teste no banco informado:

```powershell
java -cp ".\out;.\lib\postgresql-42.7.7.jar" testes.ContaEtapa1Test
```

## Repositório e autoria

Código-fonte: https://github.com/tataFaucz/Projeto-Rede-Social

**Thais Faucz Jasse** — projeto acadêmico do curso de Análise e Desenvolvimento de Sistemas, UDESC CCT.
