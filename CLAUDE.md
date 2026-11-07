# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Veridt is the back-end of a platform for capturing web page content as traceable, verifiable digital evidence. Users buy credit packages, spend credits on screenshots or navigation recordings, then consult records and export reports and ZIPs. It is a university project (UFBA, MATA62).

The repository is at the foundation stage: only `VeridtApplication` exists, and none of the domain modules below are implemented yet. Docs and code comments are in Brazilian Portuguese.

`CONTEXTO.md` holds the developer summary and roadmap. The authoritative requirements and ADRs live in a separate repository, [MATA62-Veridt](https://github.com/iuryyxd/MATA62-Veridt) (`docs/`). If `CONTEXTO.md` and an ADR disagree, the ADR wins.

## Commands

Requires JDK 25 and a running Docker daemon (tests use Testcontainers). There is no linter or formatter configured.

```bash
./mvnw test                                           # all tests (.\mvnw test on Windows)
./mvnw test -Dtest=VeridtApplicationTests             # one test class
./mvnw test -Dtest=VeridtApplicationTests#contextLoads  # one test method
./mvnw test-compile                                   # compile only, no Docker needed
./mvnw clean package -DskipTests                      # build the jar
./mvnw spring-boot:run                                # run the app on the host; auto-starts only the postgres Compose service
./mvnw spring-boot:test-run                           # run the app via TestVeridtApplication (Postgres from Testcontainers)
docker compose up --build                             # run Postgres + app
docker compose down
```

`docker compose` needs a `.env` at the repo root (gitignored) defining `POSTGRES_DB`, `POSTGRES_USER` and `POSTGRES_PASSWORD`. `application.properties` also imports this file when the app runs on the host. Postgres only applies these values to an empty volume, so changing them after the first `up` fails with `role "..." does not exist` until you run `docker compose down -v`, which deletes the database data.

Ports: app on `8080`, Postgres on host port `5433` (not 5432).

On this machine `JAVA_HOME` may point to an older JDK, which fails with `class file has wrong version`. A JDK 25 is at `~/.jdks/openjdk-25`; set `JAVA_HOME` to it for the Maven command.

## Architecture

Modular monolith on Spring Boot 4.1 and Spring Modulith 2.1, partitioned by domain (ADR-0001, ADR-0003). Priority characteristics are security, auditability, reliability and traceability (ADR-0002).

Each module is a direct subpackage of `br.ufba.veridt`:

| Module | Responsibility | Requirements |
|---|---|---|
| `identity` | Sign-up, login, logout, password recovery | REQ 01-04 |
| `credits` | Packages, balance, payments (Mercado Pago, Pix) | REQ 05-07 |
| `capture` | Screenshots, recording, incremental saving | REQ 08-11 |
| `records` | Listing, details, reports, ZIP export | REQ 12-15 |
| `notification` | Async e-mail | REQ 02, 07, 10, 11 |
| `storage` | Files and integrity | Supports `capture` and `records` |

Allowed module dependencies. Anything else is a boundary violation:

- `identity` → `notification`
- `credits` → `notification`
- `records` → `storage`
- `capture` → `credits`, `records`, `notification`, `storage`

Boundaries are meant to be enforced by a test calling `ApplicationModules.verify()`. That test does not exist yet; add it when the first module lands.

## Implementation rules

- Schema changes go only through Flyway migrations in `src/main/resources/db/migration` (`ddl-auto=validate`). `V1` holds the tables Spring Modulith and Spring Session need, since neither creates them here.
- Passwords use Argon2id. This needs `bcprov-jdk18on`, which is not in `pom.xml` yet.
- Users can only access their own records.
- Payment webhooks must validate authenticity and be idempotent. Credits are granted only after payment confirmation, inside a transaction.
- Every captured file gets a SHA-256 hash, and every capture gets a unique identifier.
- Captures are saved in parts and keep their state (completed or interrupted).
- Relevant events go to an audit log: who, when, operation, result.
- E-mail must never bring down the main operation: send it through async events.

## Course rule: ADRs

ADRs may only be created or changed in class, in supervised sessions. Do not write or edit an ADR, and do not settle an open decision on your own. Note the point as pending instead.

Currently pending: frontend (SPA or Thymeleaf), capture technology (proposal: Playwright with CDP screencast in parts), storage (disk or MinIO), and credits consumed per record.
