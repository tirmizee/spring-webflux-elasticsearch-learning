# Chapter 03 Verification Report

- **Timestamp (with timezone):** 2026-10-23 21:49:21 +07 (ICT, Asia/Bangkok)
- **Source revision verified (historical):**
  - Branch: `main`
  - Repository HEAD at verification time: `d855622` (`docs(02-api-key-authentication): remove stale uncommitted-status note`)
  - At that time Chapter 03 (`03-document-crud/`) was still **untracked/new** and was verified from the **working tree** (not yet committed). This section records the source state that was actually executed; the timestamp, commands, and results below all refer to that run.
  - **Current state (informational):** Chapter 03 has since been committed in `ce3b4d8` (`feat(03-document-crud): add document CRUD chapter`). The verification evidence in this report was produced against the working-tree source described above, which matches what was committed in `ce3b4d8`; it was not re-run against that later commit.
- **Versions:**
  - Java: OpenJDK Temurin **21.0.2** (arm64); Maven 3.9.9 (via `JAVA_HOME` → 21)
  - Spring Boot **4.1.2**; Spring Framework 7.0.10; Spring Data Elasticsearch **6.1.2**
  - Elasticsearch Java API Client **9.4.8**; Elasticsearch server image **9.4.8**
  - Testcontainers **2.0.5**; Docker 25.0.2; Compose v2.24.3-desktop.1
- **Environment:** macOS (arm64). Default `java`/`mvn` are Java 17; JDK 21 selected explicitly via `JAVA_HOME`. Docker Desktop running.

## Scope recap
Create/Get/Update/Delete of `Product` documents in a single index `learning-products`, through two clients (Spring Data reactive + Java API Client) on the same schema, on a security-enabled cluster authenticated with an API key. Independent project; Chapters 01/02 untouched.

## Key design decisions
- **No silent upsert.** Spring Data `save` is an upsert, so create gates on `exists` (409 if present) and update gates on `exists` (404 if absent). The Java client uses `op_type=create` (ES returns 409, translated to `DuplicateProductException`) and an `exists` pre-check for update/delete. Observable behavior is identical across both clients.
- **Explicit ids** via URL path for reproducibility; `_id` vs `_source` explained in the README.
- **Immediate refresh on writes** (`RefreshPolicy.IMMEDIATE` / `refresh=true`) for deterministic read-after-write in a learning context; README notes this is not a production default.
- **Idempotent index init** at startup via `ReactiveIndexOperations.createWithMapping()`; no templates/aliases/rollover.
- **Error mapping** via a single `@RestControllerAdvice`: 404/409/400 plus the inherited 401/403/502/503/504/500; generic `ProblemDetail` bodies, no credential/host/stack-trace leakage.
- No custom repositories/interfaces/factories; blocking Java client isolated on `boundedElastic`.

## Commands executed
- `mvn compile` (main) — success
- `mvn clean verify` — success (surefire + failsafe)
- `docker compose -p ch03 up -d` + authenticated readiness probe
- Create API key via `POST /_security/api_key` (least-privilege, index-level CRUD)
- `java -jar target/document-crud.jar` with `ELASTICSEARCH_API_KEY`
- Real cURL for the full CRUD flow + error scenarios
- `docker compose -p ch03 down` + temp key file removed

## Gate results

| Gate | Expected | Actual | Status |
|---|---|---|---|
| Maven compile | success | success | PASS |
| Unit tests (surefire) | pass | 26 tests, 0 failures | PASS |
| Integration tests (failsafe, real secured ES) | pass | 7 tests, 0 failures | PASS |
| Full `mvn clean verify` | BUILD SUCCESS | BUILD SUCCESS (23.1s) | PASS |
| Docker startup + security | authenticated 200, anon 401 | observed 200 / 401 | PASS |
| API key (index privileges) created | `encoded` returned | created (len 60) | PASS |
| App startup + index init | listening, index created | Netty 8082; "Created index 'learning-products'" | PASS |
| cURL CRUD + errors | per table below | all matched | PASS |
| No credential leakage | none in logs/responses | none observed | PASS |
| Chapter 01 / 02 unchanged | untouched | untouched | PASS |
| Chapter 03 independent | builds/runs standalone | yes | PASS |

## Actual test counts (this run)
Surefire (unit) — total **26**, 0 failures:
- `controller.JavaClientProductControllerTest` — 5
- `controller.SpringDataProductControllerTest` — 8
- `service.JavaClientProductServiceTest` — 6
- `service.SpringDataProductServiceTest` — 7

Failsafe (integration) — `integration.ProductCrudIT` — **7**, 0 failures (ordered cross-client CRUD flow + duplicate 409 + missing 404, on real secured ES with a real API key). Final line: `BUILD SUCCESS`.

> Note: one integration assertion initially failed due to a test-only bug (`BigDecimal.equals` is scale-sensitive: `159.0` vs `159.00`). Fixed the assertion to use `compareTo`; the application behavior was correct throughout. Re-ran `mvn clean verify` → all pass. No application code changed for this.

## Verified cURL results (real app + real secured ES 9.4.8, both clients)
| Step | Request | Status | Notes |
|---|---|---|---|
| 1 | POST spring-data/products/p100 | **201** | `{"client":"spring-data","id":"p100","name":"Keyboard",...}` |
| 2 | GET java-client/products/p100 | **200** | cross-client read sees the spring-data write (same data) |
| 3 | POST java-client/products/p100 (dup) | **409** | `Product already exists` |
| 4 | PUT java-client/products/p100 | **200** | full replace → `Keyboard Pro`, qty 7 |
| 5 | GET spring-data/products/p100 | **200** | sees java-client update |
| 6 | DELETE spring-data/products/p100 | **204** | |
| 7 | GET java-client/products/p100 | **404** | `Product not found` after delete |
| 8 | PUT spring-data/products/ghost | **404** | update missing (no upsert) |
| 9 | DELETE java-client/products/ghost | **404** | delete missing |
| 10 | POST spring-data/products/bad (invalid) | **400** | `Invalid request` (validation) |

## No-leak verification
- Application logs contained only startup/index-init info and sanitized warnings — no API key, no `Authorization` header, no host/URI.
- All error bodies are generic `ProblemDetail`s (no key, host, stack trace, or raw ES message). The 400 body includes only the offending field name + validation message (safe, no infra detail).
- No real credentials in source or committed docs; `.env` gitignored, `.env.example` holds placeholders.

## Auth vs authz (401 vs 403)
- 401/403 mapping is inherited from the Chapter-02 pattern and exercised via the ElasticsearchException-status branch. In this chapter the end-to-end run used a correctly-privileged key (success path). The dedicated 401/403 end-to-end behavior was verified in Chapter 02; here it is covered by the handler logic and not separately re-run end-to-end.

## Known limitations
- Immediate refresh on writes is a learning-time choice, not a production default (documented).
- 401/403 not re-exercised end-to-end in this chapter (see above); other error paths (404/409/400) are verified end-to-end.
- HTTP-layer TLS intentionally disabled for local learning (documented; production requires HTTPS).

## Overall status
**COMPLETED** — `mvn clean verify` passes with **26 unit + 7 integration** tests against a real security-enabled Elasticsearch 9.4.8 using a least-privilege API key; real cURL confirms every required status code (201/200/204/404/409/400) across both clients with cross-client consistency; no credential leakage observed. Chapters 01 and 02 are untouched; Chapter 03 runs independently.
