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

Keep commands and response examples synchronized with executed results. Explain variable fields rather than falsely pinning dynamically changing values. Root README lists all 13 chapter topics with clear state (planned/implemented/verified), links only to folders that exist.

## Ownership and safety
Inspect current workspace and git status before changes. If a previous repo exists, adapt without destructive moves unless user authorizes. Never overwrite a user's uncommitted work; never commit or push without explicit instruction.

## Spring Boot Project Structure & Coding Best Practices
These standards apply to **all** chapters (current and future). They define the default internal organization of each independent chapter project. They refine — and never weaken — the verification, approval, and ownership rules in the other steering files.

### Package Organization
- Prefer a **layered package structure** for simple learning chapters, under the chapter's base package (e.g. `com.example.learning.<chapter>`).
- Default packages:
  - `controller/` — REST API endpoints (WebFlux handlers)
  - `service/` — application logic and Elasticsearch integration
  - `dto/` — request and response models (API contracts)
  - `exception/` — centralized exception handling
  - `config/` — explicit configuration, created **only when** auto-configuration is insufficient
- Keep the Spring Boot application class (`@SpringBootApplication`) in the **root** package of the chapter.
- Use **package-by-feature** only when a chapter's complexity genuinely justifies it; otherwise stay layered.
- Do not create packages (or empty directories) without a genuine responsibility.

### Separation of Concerns
- Controllers must remain **thin**: map HTTP routes to service calls, return reactive types, and avoid integration logic.
- Services hold application/integration logic and must not depend on HTTP-specific details (request/response objects, servlet/web types) unnecessarily.
- DTOs are plain data carriers (prefer Java `record`s) and must not contain business logic. Prefer stable DTOs over exposing raw vendor payloads so API contracts stay stable.
- Client-specific implementations must be **clearly identifiable** (e.g. `SpringData*` vs `JavaClient*` class names and distinct endpoint path prefixes). Keep the Spring Data Elasticsearch approach and the official Elasticsearch Java API Client approach clearly separated.
- Avoid unnecessary coupling between layers. Do not create interfaces without a meaningful purpose (no single-implementation interfaces "just in case").

### Spring Boot Standards
- Use **constructor injection** (no field injection).
- Favor Spring Boot **auto-configuration**; add custom beans/config only when necessary and place them in `config/`.
- Use meaningful class and method names; follow standard Java naming conventions.
- Keep public API contracts explicit (documented endpoint paths, HTTP status codes, response fields).
- Handle errors consistently and centrally (e.g. `@RestControllerAdvice`), and never leak credentials or connection details in error responses.
- Follow Java 21 and Spring Boot 4.x compatible practices (e.g. Boot 4 modularized test slices, `Rest5Client` infrastructure).
- Avoid unnecessary new dependencies, factories, or design patterns.

### Reactive Programming
- Never block reactor-netty **event-loop threads**. Do not call `.block()` within reactive request handling.
- Prefer genuinely non-blocking (reactive) APIs when available (e.g. Spring Data `ReactiveElasticsearchClient`).
- For the **synchronous** official `ElasticsearchClient`, isolate blocking calls with `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`. Do not describe a blocking call wrapped in `Mono` as inherently non-blocking — document that the work is blocking but offloaded.
- Use Project Reactor idiomatically; return `Mono`/`Flux` from services and controllers.

### Testing Standards
- **Mirror production packages** in tests where practical (`controller/`, `service/`, `integration/`).
- Naming: unit/slice tests end with `*Test` (run by Surefire); real-server integration tests end with `*IT` (run by Failsafe in the `verify` phase).
- Write **meaningful** unit tests: controller slice tests (`@WebFluxTest` with mocked services) and service tests (mocked client + `StepVerifier` for reactive streams, including error propagation).
- Use **integration tests** (Testcontainers) to prove real Elasticsearch behavior; assert deterministic values exactly and dynamic metadata tolerantly.
- Keep tests deterministic and independent. Do not create placeholder tests, disable tests, or weaken assertions merely to pass verification.
- Ensure Maven Surefire and Failsafe are configured so the intended suites actually execute during `mvn clean verify`.

### Repository Consistency
- Every chapter remains an **independent** Maven project (no cross-chapter source dependencies, no root aggregator POM).
- Every chapter contains its own `README.md` (Thai narrative, English technical terms), `docker-compose.yml`, and `pom.xml`.
- Every **completed** chapter contains its own `VERIFICATION.md` with real recorded evidence.
- Apply the same naming and organization principles across all future chapters.
- Do not modify previously completed chapters automatically; changes to an existing chapter require explicit user approval.

## README Project Structure Standard
Applies to the "โครงสร้างโปรเจกต์" (Project Structure) section of **every** chapter README (refines item 4 of the Chapter README template above).

### Presentation
- Every chapter README must contain a "โครงสร้างโปรเจกต์" section with an **expanded ASCII directory tree** in a fenced ```text block.
- List **every actual source file and test file individually**. Include the relevant root-level files that exist in the chapter: `.env.example` (when present), `.gitignore`, `docker-compose.yml`, `pom.xml`, `README.md`, `VERIFICATION.md`, and `src/`.
- Expand `src/` down through `main/` and `test/`, the full Java package path, and every package directory and file.
- **Never** use compressed class notation such as `{ClassA,ClassB}.java`, and never use ellipsis placeholders such as `controller/…`, `service/…`, or `Test*.java`.
- Do not invent files or directories that do not exist; do not omit files that do exist.
- Ordering within each directory: **directories grouped before files**, each group in alphabetical order (the `@SpringBootApplication` class therefore appears after its sibling sub-packages).
- Use consistent indentation and tree connectors (`├──`, `└──`, `│`).
- Avoid lengthy inline descriptions inside the tree; keep per-line comments out of the tree and put responsibilities in prose elsewhere in the README if needed.

### Accuracy and maintenance
- The tree must match the real repository filesystem (excluding build output such as `target/`).
- After adding, removing, moving, or renaming any source/test/config file, regenerate or re-verify the affected chapter's tree before considering the chapter done.
- Verify the tree against the actual files (e.g. `git ls-files` within the chapter, excluding `target/`).

### Consistency across chapters
- Apply the same tree presentation style to all current and future chapters.
- Respect each chapter's own independent Spring Boot layout; reflect the implementation as it actually is.
- Documentation describes the implementation — it must **not** dictate new packages, classes, or architectural layers, and source code must not be changed merely to match a documentation example.

## Root README Presentation Standards
Applies to the repository **root** `README.md` (chapter READMEs follow the Chapter README template and the README Project Structure Standard above).
- Use a clear project title and a concise Thai introduction (keep technical terms, identifiers, and code in English).
- Prefer clean technical documentation over decorative design; no banners or animated images.
- Use icons sparingly and consistently — mainly for section navigation and Roadmap statuses.
- Use technology badges only when useful and **factually correct** (verified versions only); keep the set small.
- Roadmap status indicators are fixed and consistent: `✅ Verified`, `🛠️ Implemented`, `⏳ Planned`.
- Keep all chapter links and statuses **synchronized with the repository** (link only to folders that exist; a chapter is `✅ Verified` only when its `VERIFICATION.md` records real passing evidence).
- Use concise, consistent topic descriptions in a readable Markdown table; avoid overly wide tables (readable on desktop and mobile GitHub).
- Do not duplicate chapter descriptions across the Roadmap and Getting Started; the Roadmap is the single source for status.
- Keep Getting Started practical and beginner-friendly; link to chapter READMEs for full setup rather than duplicating each chapter's instructions.
- Preserve the Thai narrative with English technical terminology; keep whitespace and Markdown formatting consistent; ensure all relative links work.
- Do not change chapter README formatting standards as part of root README edits.

## Chapter README Presentation Standards
Applies to each chapter's `README.md` (complements the Chapter README template and the README Project Structure Standard above; does not replace them). General Java coding standards stay in `code-standards.md`.
- Use a clear learning-oriented structure and a concise introduction stating what the reader learns, which clients/features are used, and what they can run after finishing.
- Add a compact **Table of Contents** (valid GitHub Markdown anchors) for longer chapters so sections are easy to navigate.
- Keep project trees readable and **accurate to the real filesystem** (every source/test file listed; no compressed notation or ellipses; no noisy inline comments — put responsibilities in a short section below the tree).
- Provide a practical, **step-by-step Quick Start** in small fenced Bash blocks (enter dir → start ES → verify readiness → test → run → call API → clean up). Never imply `mvn clean verify` passes without Docker when integration tests need Testcontainers.
- Group **API examples by client**, each with HTTP method + endpoint, copy-paste cURL, a concise expected response, and a short explanation; clearly mark dynamic fields (e.g. cluster UUID) as variable.
- Use section icons **sparingly** and consistently for navigation; keep Markdown GitHub-compatible and readable on mobile; no banners/screenshots/GIFs/badges inside chapter READMEs.
- Avoid duplicate explanations across sections; preserve technical accuracy and keep documented commands/responses synchronized with real verification evidence.
- Preserve the Thai narrative with English technical terminology.
