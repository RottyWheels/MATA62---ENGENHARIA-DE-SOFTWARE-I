# Veridt: contexto para desenvolvedores

Plataforma de captura de evidências digitais: o usuário compra créditos, registra telas ou vídeos de navegação de um site e obtém relatório e ZIP verificáveis.

Fontes oficiais: ADRs e requisitos em `MATA62-Veridt/docs`. Este arquivo é só um resumo; em caso de divergência, valem as ADRs.

## Decisões arquiteturais

- **ADR-0001:** monólito modular.
- **ADR-0002:** características prioritárias são segurança, auditabilidade, confiabilidade e rastreabilidade.
- **ADR-0003:** particionamento por domínio.

## Stack (`pom.xml`)

- Java 25, Spring Boot 4.1.2, Spring Modulith 2.1.2
- PostgreSQL 16, JPA, Flyway
- Spring Security, Spring Session JDBC, Validation
- Web MVC, WebSocket, Mail, Actuator
- Testcontainers, Docker Compose

**A adicionar:** `bcprov-jdk18on` (Argon2), Playwright, SDK do Mercado Pago e gerador de PDF.

## Módulos

| Módulo | Responsabilidade | Requisitos |
|---|---|---|
| `identity` | Cadastro, login, logout, recuperação de senha | REQ 01-04 |
| `credits` | Pacotes, saldo e pagamento (Mercado Pago, Pix) | REQ 05-07 |
| `capture` | Captura de tela, gravação, salvamento incremental | REQ 08-11 |
| `records` | Listagem, detalhes, relatório, ZIP | REQ 12-15 |
| `notification` | E-mails assíncronos | REQ 02, 07, 10, 11 |
| `storage` | Arquivos e integridade | Suporte a `capture` e `records` |

Dependências permitidas:

- `credits` → `notification`
- `capture` → `credits`, `records`, `notification`, `storage`
- `records` → `storage`
- `identity` → `notification`

## Regras de implementação

- Senhas com Argon2id; nunca em texto puro.
- Usuário acessa apenas os próprios registros.
- Webhook de pagamento: validar autenticidade e processar de forma idempotente.
- Créditos só após confirmação do pagamento, em transação.
- SHA-256 em cada arquivo capturado; identificador único por captura.
- Eventos relevantes em log de auditoria: quem, quando, operação e resultado.
- E-mail nunca pode derrubar a operação principal: usar eventos assíncronos.
- Captura salva em partes e mantém o estado (concluída ou interrompida).
- Schema só via Flyway (`ddl-auto=validate`).
- Fronteiras validadas por teste com `ApplicationModules.verify()`.

## Ambiente

- JDK 25 com `JAVA_HOME` configurado.
- Docker em execução.
- Conta de teste do Mercado Pago e túnel (ngrok ou cloudflared) para o webhook.
- Postgres exposto no host em `5433`; aplicação em `8080`.

## Regra da disciplina

ADRs só podem ser criadas ou alteradas em sala, nas sessões supervisionadas. Fora dela, apenas anote o ponto pendente.

**Pendências para a próxima sessão:**

- Frontend: SPA ou Thymeleaf.
- Tecnologia de captura (proposta: Playwright com screencast CDP em partes).
- Armazenamento: disco ou MinIO.
- Quantos créditos cada registro consome.

## Roadmap

1. Fundação
2. `identity` + `notification`
3. Spike de captura (maior risco técnico)
4. `storage` + auditoria
5. `credits` + pagamento
6. `capture`
7. `records`
8. Backup, testes E2E e instruções de execução
