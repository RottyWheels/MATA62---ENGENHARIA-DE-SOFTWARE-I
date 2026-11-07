# Veridt

Back-end do Veridt, uma plataforma web para capturar e gravar conteúdo de páginas da internet, produzindo evidências digitais rastreáveis e verificáveis.

Projeto do componente curricular Engenharia de Software I (MATA62).

- **Instituição:** Universidade Federal da Bahia (UFBA), Instituto de Computação
- **Semestre:** 2026.2
- **Professor:** Eduardo Santana de Almeida
- **Discentes:** Igor Reis Santos, Edwu Morais Braga, Ana Beatriz Figueiredo de Aquino e Beatriz Mendes Rios

## Sobre o sistema

Advogados e usuários comuns compram pacotes de créditos (básico, médio e premium) pelo Mercado Pago ou via Pix. Os créditos são usados para registrar capturas de tela ou vídeos de navegação de um site. Depois, o usuário pode consultar os registros, gerar relatórios e exportar os arquivos em ZIP. Compras e registros concluídos são confirmados por e-mail.

## Status

Em desenvolvimento. O repositório contém a estrutura inicial da aplicação e a configuração de execução com Docker. Os módulos abaixo ainda serão implementados.

## Stack

| Área | Tecnologias |
|---|---|
| Linguagem e framework | Java 25, Spring Boot 4.1.2 |
| Arquitetura | Spring Modulith 2.1.2 |
| Banco de dados | PostgreSQL 16, Spring Data JPA, Flyway |
| Segurança | Spring Security, Spring Session JDBC |
| Comunicação | Spring Web MVC, WebSocket, Spring Mail |
| Testes | JUnit, Testcontainers |
| Infraestrutura | Docker, Docker Compose, Maven Wrapper |

## Arquitetura

O sistema é um monólito modular, particionado por domínio.

| Módulo | Responsabilidade | Requisitos |
|---|---|---|
| `identity` | Cadastro, login, logout e recuperação de senha | REQ 01-04 |
| `credits` | Pacotes, saldo e pagamentos | REQ 05-07 |
| `capture` | Captura de tela e gravação de navegação | REQ 08-11 |
| `records` | Listagem, detalhes, relatórios e exportação em ZIP | REQ 12-15 |
| `notification` | Envio assíncrono de e-mails | REQ 02, 07, 10 e 11 |
| `storage` | Armazenamento e integridade dos arquivos | Suporte a `capture` e `records` |

As características prioritárias são segurança, auditabilidade, confiabilidade e rastreabilidade.

## Como executar

**Pré-requisito:** Docker com Docker Compose.

1. Copie o `.env.example` para `.env` na raiz do projeto e defina uma senha. O `.env` não deve ser commitado.

   ```bash
   cp .env.example .env
   ```

2. Suba o banco de dados, o Mailpit e a aplicação:

   ```bash
   docker compose up --build
   ```

| Serviço | Endereço |
|---|---|
| Aplicação | `http://localhost:8080` |
| PostgreSQL | `localhost:5433` |
| Mailpit (painel de e-mails) | `http://localhost:8025` |
| Mailpit (SMTP) | `localhost:1025` |

O Mailpit captura os e-mails enviados em desenvolvimento, sem entregá-los de verdade.

Para encerrar:

```bash
docker compose down
```

## Testes

Os testes exigem JDK 25, com `JAVA_HOME` configurado, e Docker em execução.

No Windows:

```bash
.\mvnw test
```

No Linux ou macOS:

```bash
./mvnw test
```

## Estrutura do repositório

```
.
├── .mvn/                 # Maven Wrapper
├── src/
│   ├── main/
│   │   ├── java/br/ufba/veridt/
│   │   └── resources/application.properties
│   └── test/java/br/ufba/veridt/
├── CONTEXTO.md           # Resumo técnico para desenvolvedores
├── compose.yaml
├── Dockerfile
├── mvnw
├── mvnw.cmd
└── pom.xml
```

## Documentação

Requisitos, ADRs, diagramas e táticas arquiteturais estão no repositório [MATA62-Veridt](https://github.com/iuryyxd/MATA62-Veridt). As telas estão no [Figma do Veridt](https://www.figma.com/design/D4vDhuk1DBAJY9knexl7Hs/Veridt?node-id=0-1&t=YgLGf4RsxAjWx53e-1).

O contexto de desenvolvimento e o roadmap estão em [CONTEXTO.md](CONTEXTO.md).
