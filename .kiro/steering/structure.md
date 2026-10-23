---
inclusion: always
---
# Repository Structure and Documentation

## Independent project policy
The root repository is **not** a Maven reactor. No root aggregator `pom.xml`. Each numbered chapter is an independent Spring Boot project with its own `pom.xml`, `src/main`, `src/test`, `README.md`, `docker-compose.yml`, and `VERIFICATION.md` after execution. One chapter should run without building any other chapter. Keep per-chapter dependencies and configs explicit.

```text
spring-webflux-elasticsearch-learning/
├── .kiro/
│   ├── prompts/initialize-learning-repository.md
│   └── steering/{product,tech,structure,chapter-workflow,verification-standards}.md
├── README.md
├── .gitignore
├── 01-basic-connection/
│   ├── README.md
│   ├── VERIFICATION.md
│   ├── pom.xml
│   ├── docker-compose.yml
│   └── src/{main,test}/...
└── [future chapters only when approved]
```

Do not create empty dirs for Chapter 02–12 initially. Avoid hardcoded absolute local paths. Do not set fixed Docker `container_name` unless necessary; use distinct Compose project names or project-scoped invocation for isolation. Document host port conflicts; expose local no-auth Elasticsearch only on `127.0.0.1`.

## Chapter README template (Thai narrative; English technical terminology)
1. Overview — what and why.
2. Learning Objectives — outcomes.
3. Concepts — define ideas and reactive/blocking distinction if relevant.
4. Project Structure — tree and class responsibilities.
5. How It Works — request → service → client → ES, Mermaid if useful.
6. Configuration — versions, properties, env vars, Docker/TLS/security details.
7. Getting Started — prerequisites, up, readiness, run, stop/cleanup commands.
8. API Examples — copy-paste cURL, HTTP status, expected fields/responses, assertion commands.
9. Testing — unit/integration commands and verification boundaries.
10. Key Takeaways — lessons.
11. Common Pitfalls — errors and production considerations.

Keep commands and response examples synchronized with executed results. Explain variable fields rather than falsely pinning dynamically changing values. Root README lists all 12 chapter topics with clear state (planned/implemented/verified), links only to folders that exist.

## Ownership and safety
Inspect current workspace and git status before changes. If a previous repo exists, adapt without destructive moves unless user authorizes. Never overwrite a user's uncommitted work; never commit or push without explicit instruction.
