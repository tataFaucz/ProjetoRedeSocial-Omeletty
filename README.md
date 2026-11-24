# Projeto Rede Social

Projeto acadêmico da disciplina de Banco de Dados e Programação Orientada a Objetos. A aplicação é Java Swing e nesta branch utiliza MongoDB.

## Bancos e branches — versões isoladas

As implementações SQL e NoSQL ficam em branches diferentes e usam bancos independentes. Não compartilham esquema, configuração ou arquivos de inicialização:

| Versão | Branch | Banco e script |
| --- | --- | --- |
| SQL — Fase 1 | [`fase1-postgresql`](https://github.com/tataFaucz/ProjetoRedeSocial-Omeletty/tree/fase1-postgresql) | PostgreSQL, banco `rede_social`; script `RedeSocial_PostgreSQL.sql` nessa branch |
| NoSQL — Fase 2 | [`fase2-mongodb`](https://github.com/tataFaucz/ProjetoRedeSocial-Omeletty/tree/fase2-mongodb) | MongoDB, banco `rede_social_nosql`; script `MongoDB_Init.js` nesta branch |

Use uma branch por vez. A versão MongoDB não lê as variáveis `POSTGRES_*` ou `DB_*`; a versão PostgreSQL não lê `MONGODB_*`. Os bancos também têm nomes diferentes, portanto executar uma versão não altera os dados da outra.

## Funcionalidades

- Cadastro, login e manutenção de usuários;
- Publicações com fotos, comentários e hashtags;
- Seguir usuários, curtir e compartilhar publicações;
- Feed de publicações de usuários seguidos;
- Mensagens entre usuários;
- Consultas de gestão e relatórios.

## Tecnologias e requisitos

- Java 11 ou superior;
- Maven 3.8 ou superior;
- MongoDB 5.0 ou superior, local ou remoto;
- `mongosh` para executar o script de criação do banco.

O driver MongoDB para Java é obtido pelo Maven a partir da dependência declarada em `pom.xml`.

## Estrutura

```text
projeto rede social/
├── dados/        # Entidades
├── dao/          # Acesso às coleções MongoDB
├── negocios/     # Regras de negócio e inicialização
├── testes/       # Testes de conta
└── ui/           # Interface Swing
MongoDB_Init.js   # Criação idempotente das coleções e índices
pom.xml           # Compilação e driver MongoDB
```

## Banco MongoDB

O banco padrão é `rede_social_nosql`, no MongoDB em `localhost:27017`. A aplicação cria os índices necessários na inicialização; o script abaixo também pode preparar previamente o banco e pode ser executado novamente sem apagar dados.

No PowerShell:

```powershell
$env:MONGODB_URI = "mongodb://localhost:27017"
$env:MONGODB_DATABASE = "rede_social_nosql"
mongosh $env:MONGODB_URI .\MongoDB_Init.js
```

Para uma instância com autenticação ou hospedada, configure `MONGODB_URI` com a URI fornecida pelo administrador e mantenha `MONGODB_DATABASE` como `rede_social_nosql` (ou escolha outro nome exclusivo). Não coloque senhas no repositório.

Coleções: `usuarios`, `fotos`, `comentarios`, `mensagens`, `curtidas`, `compartilhamentos`, `seguidores`, `hashtags` e `counters`. As relações usam IDs numéricos e índices únicos; hashtags são associadas às publicações pelo array `hashtagIds`. O gerador de IDs usa a coleção `counters`.

## Compilação e execução

Na raiz do repositório, com o serviço MongoDB ativo:

```powershell
mvn clean package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/dependency
$env:MONGODB_URI = "mongodb://localhost:27017"
$env:MONGODB_DATABASE = "rede_social_nosql"
java -cp "target/classes;target/dependency/*" negocios.Main
```

As variáveis podem ser definidas como propriedades Java (`-DMONGODB_URI=...` e `-DMONGODB_DATABASE=...`) no lugar de variáveis de ambiente.

## Testes

Os testes unitários de validação de conta e senha não precisam do banco:

```powershell
java -cp "target/classes;target/dependency/*" testes.ContaEtapa1Test --unit
```

O fluxo completo da conta exige MongoDB ativo e usa a mesma URI e o mesmo banco configurados para a aplicação:

```powershell
java -cp "target/classes;target/dependency/*" testes.ContaEtapa1Test
java -cp "target/classes;target/dependency/*" testes.MongoDBEtapa2Test
```

`MongoDBEtapa2Test` verifica persistência de publicações, seguidores, hashtags, comentários, curtidas, compartilhamentos, mensagens, relatórios e exclusão em cascata; os registros temporários são removidos ao final.

## Entrega acadêmica

Os arquivos de código, o script de criação do banco e estas instruções estão disponíveis sem compactação nesta branch. Para a demonstração, inicie o MongoDB e execute a aplicação seguindo as etapas acima. A branch SQL permanece disponível no link indicado no início deste README.

Repositório: https://github.com/tataFaucz/ProjetoRedeSocial-Omeletty

**Thais Faucz Jasse** — curso de Análise e Desenvolvimento de Sistemas, UDESC CCT.
