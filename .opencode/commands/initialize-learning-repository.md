---
description: Initialize Chapter 01 learning repository only
---
# Initialize Spring WebFlux + Elasticsearch Learning Repository

You are a Senior Java 21 / Spring WebFlux / Spring Data Elasticsearch / Elasticsearch Java Client engineer and a technical instructor. Act as an implementing agent, not just a code generator. Your deliverables are working code, tests, local Elasticsearch runtime verification and instructional documentation.

## Primary objective
Initialize or adapt the workspace into `spring-webflux-elasticsearch-learning`, a monorepo containing **independent** Maven Spring Boot applications. **Implement Chapter 01 only.** Leave Chapters 02–12 as roadmap entries in the root README until the user explicitly requests them. Avoid an aggregator POM and avoid shared source dependencies.

## Read these project rules FIRST
- `.opencode/rules/product.md`
- `.opencode/rules/tech.md`
- `.opencode/rules/structure.md`
- `.opencode/rules/chapter-workflow.md`
- `.opencode/rules/verification-standards.md`

These steering rules are mandatory; if any conflict with this prompt, prefer the more restrictive non-destructive/testing/approval constraint and surface the discrepancy. Use Thai for narrative README content, with English technical terminology, identifiers and code comments.

## Explicit stack
- Java **21**; Spring Boot **4.x**; Spring WebFlux; Maven.
- Elasticsearch server version compatible with chosen Spring Data Elasticsearch and Java API Client.
- Both integration approaches in Chapter 01: Spring Data reactive client/operations and **official** Elasticsearch Java API Client, in clearly separate implementations.
- JUnit Jupiter, Mockito, Reactor Test, and realistic integration testing (Testcontainers where beneficial).
- Every chapter owns `pom.xml`, `docker-compose.yml`, README, app sources/tests and verification report.

## Chapter roadmap — write to root README
01 `basic-connection`; 02 `api-key-authentication`; 03 `document-crud`; 04 `search-queries`; 05 `elasticsearch-filtering`; 06 `custom-analyzers`; 07 `sorting-pagination`; 08 `fuzzy-search`; 09 `index-management`; 10 `bulk-operations`; 11 `reactive-error-handling`; 12 `index-recreation`; 13 `integration-testing`.

## Step A: Inspect and plan
1. Inspect the workspace, `git status --short`, existing user project, Java/Maven/Docker tools, config and dependency constraints. Do not delete or overwrite existing code or uncommitted work. If the repository is the existing single-project `spring-web-flux-elastic-search`, propose safe adaptation and avoid destructive moves.
2. Verify **concrete compatible** Spring Boot 4.x, Spring Data Elasticsearch, official Elasticsearch Java API Client and Elasticsearch Docker versions against documentation/metadata. Do not make up exact versions. Prefer BOM-managed versions, document actual choices.
3. Present a short implementation plan including endpoints, response contracts, configs, architecture and test/evidence plan. Then implement only Chapter 01 within approved scope.

## Step B: Implement `01-basic-connection`
Create an independent Spring Boot WebFlux app and dedicated `docker-compose.yml`:
- Elasticsearch single-node, official pinned compatible server image.
- No authentication **only for local Chapter 01**; bind ES exposed port to `127.0.0.1`, document that insecure mode must never be deployed publicly.
- Readiness/health validation and safe Docker startup/stop instructions.
- Configuration via `application.yml` and environment variables; no credentials hardcoded.
- Distinct client-specific service/controller paths, e.g. `GET /api/spring-data/elasticsearch/health` and `GET /api/java-client/elasticsearch/health`. Both MUST actually contact ES, not return constant UP.
- Cluster information endpoints for each approach if supported by current APIs, with clear documented response contracts. Prefer a stable DTO rather than exposing changing raw vendor payloads.
- For the official blocking Java API Client, ensure blocking work is NOT done on Netty event loop (e.g., use `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`). Explicitly explain blocking vs reactive behavior and why this scheduler is used. If using async APIs instead, document and properly adapt asynchronous semantics.
- Use simple layers (controller/service/config), constructor injection, meaningful error handling, no unnecessary patterns.
- Write unit/controller tests and meaningful real-server integration tests; make sure required tests execute during `mvn clean verify` or clearly document extra integration phase and include it in mandatory verification.
- Root README with architecture, roadmap and chapter states; Chapter 01 README with Thai sections Overview, Learning Objectives, Concepts, Project Structure, How It Works (Mermaid if useful), Configuration, Getting Started, API Examples, Testing, Key Takeaways, Common Pitfalls.

## Step C: Mandatory actual verification
Execute, don't just describe:
1. Record Java, Maven, Docker versions; verify compatibility.
2. From `01-basic-connection`, run `mvn clean verify` (without disabling tests).
3. Start its Elasticsearch container with `docker compose up -d`; inspect readiness (not merely container Running).
4. Start the Spring Boot app and verify it becomes ready.
5. Run real cURL requests against every Chapter 01 endpoint for both client approaches, verify expected HTTP statuses **and assert response fields**. Confirm requests actually contact live ES; include representative failure behavior where feasible.
6. Run real-server integration tests. If dependencies require Docker, rerun all relevant verification with Docker started. Retest after fixes.
7. Execute documentation commands and stop app/containers responsibly.
8. Write `01-basic-connection/VERIFICATION.md` using required evidence template with date/time, versions, exact commands, observed vs expected response values, status for every check, remaining issues and overall status.

**Never claim `COMPLETED` if any mandatory check is FAIL/BLOCKED/NOT VERIFIED. Never fabricate output, claim curl worked without running it, skip tests, or change prior chapters without authorization.**

## Step D: Stop and handoff
Report file tree, endpoints, versions, actual test/cURL outcomes, verification report path, completion status, and remaining issues. **Stop after Chapter 01.** Do not implement Chapter 02 unless the user explicitly authorizes it in a later request.
