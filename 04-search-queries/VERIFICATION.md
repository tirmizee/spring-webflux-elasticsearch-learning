# Chapter 04 Verification Report

- **Timestamp (with timezone):** 2026-10-24 00:44:13 +07 (ICT, Asia/Bangkok)
- **Source revision verified:**
  - Branch: `main`
  - Repository HEAD at verification time: `7e318d3` (`refactor(03-document-crud): use targeted Lombok on Product entity`).
  - Chapter 04 (`04-search-queries/`) was **untracked/new** and was verified from the **uncommitted working tree** on top of `7e318d3`. The commit that will contain this chapter is a later revision; this report documents the source state that was actually executed, not that later commit.
- **Versions:**
  - Java: OpenJDK Temurin **21.0.2** (arm64); Maven 3.9.9 (via `JAVA_HOME` → 21)
  - Spring Boot **4.1.2**; Spring Framework 7.0.10; Spring Data Elasticsearch **6.1.2**
  - Elasticsearch Java API Client **9.4.8**; Elasticsearch server image **9.4.8**
  - Testcontainers **2.0.5**; Lombok 1.18.48 (BOM-managed); Docker 25.0.2; Compose v2.24.3-desktop.1
- **Environment:** macOS (arm64). Default `java`/`mvn` are Java 17; JDK 21 selected explicitly via `JAVA_HOME`. Docker Desktop running.

## Scope recap
Elasticsearch `term`, `match`, and `bool` (`must`/`should`/`must_not`/`minimum_should_match`) queries with relevance `_score`, over a deterministic 10-product catalog in index `search-products`, implemented with two clients that run the same `co.elastic.clients` query DSL (shared `ProductQueries`). Security-enabled ES + API key (Chapters 02/03 conventions). Independent project; Chapters 01–03 untouched.

## Key design decisions
- **Shared query builder** (`query/ProductQueries`) consumed by both clients, so Spring Data (`NativeQuery`) and the Java API Client run the identical query and differ only in execution. Not an abstraction layer — a stateless helper, no interface.
- **term on keyword, match on text**: `category`/`brand` are `keyword` (exact `term`); `name`/`description` are `text` (full-text `match`). Demonstrates why exact matching uses keyword fields.
- **Bool semantics**: `must`/`should`/`must_not` map directly; `minimum_should_match` applied only when provided; `should` is optional when a `must` is present.
- **Empty result → HTTP 200 with empty list** (never 404).
- **Non-destructive seeding**: `DataInitializer` creates index+mapping and seeds the sample catalog only when the index does not exist; existing data is left untouched. Seeding uses immediate refresh so data is searchable at startup (documented as a learning choice, not a production default).
- **Blocking Java client isolated** on `boundedElastic`; Spring Data path fully reactive.
- Lombok used only on the `Product` entity (targeted annotations); DTOs are records.

## Commands executed
- `mvn compile` (main) — success
- `mvn clean verify` — success (surefire + failsafe)
- `docker compose -p ch04 up -d` + authenticated readiness probe
- Create API key via `POST /_security/api_key` (least-privilege, index-level)
- `java -jar target/search-queries.jar` with `ELASTICSEARCH_API_KEY`
- Real cURL for term/match/bool across both clients
- `docker compose -p ch04 down` + temp key file removed

## Gate results

| Gate | Expected | Actual | Status |
|---|---|---|---|
| Maven compile | success | success | PASS |
| Unit tests (surefire) | pass | 17 tests, 0 failures | PASS |
| Integration tests (failsafe, real secured ES) | pass | 11 tests, 0 failures | PASS |
| Full `mvn clean verify` | BUILD SUCCESS | BUILD SUCCESS (~30s) | PASS |
| Docker startup + security | authenticated 200 | ES ready, authenticated health 200 | PASS |
| API key (index privileges) created | `encoded` returned | created (len 60) | PASS |
| App startup + seeding | listening, index seeded | Netty 8083; "Created index 'search-products' and seeded 10 sample products" | PASS |
| cURL term/match/bool | per tables below | all matched | PASS |
| No credential leakage | none in logs/responses | none observed | PASS |
| Chapters 01–03 unchanged | untouched | untouched | PASS |
| Chapter 04 independent | builds/runs standalone | yes | PASS |

## Actual test counts (this run)
Surefire (unit) — total **17**, 0 failures:
- `controller.JavaClientSearchControllerTest` — 4
- `controller.SpringDataSearchControllerTest` — 5
- `query.ProductQueriesTest` — 5
- `service.JavaClientSearchServiceTest` — 3

Failsafe (integration) — `integration.SearchQueriesIT` — **11**, 0 failures (term exact hit/miss, match full-text, bool must/should/must_not, minimum_should_match, should-as-OR, empty, cross-client consistency). Final line: `BUILD SUCCESS`.

## Verified cURL results (real app + real secured ES 9.4.8)
| Scenario | Request | Result |
|---|---|---|
| term exact (keyword) | `GET term?field=category&value=laptop` (spring-data) | 200, ids `{p01,p02,p03,p04,p10}` |
| term cross-client | same (java-client) | 200, same ids → consistent |
| term exact-miss | `term?field=category&value=Laptop` (wrong case) | 200, `total=0`, empty hits (not 404) |
| match full-text | `GET match?field=name&text=macbook` (both clients) | 200, ids `{p01,p02}` |
| bool must | must category=laptop AND match name 'gaming' | ids `{p10}` |
| bool must_not | must category=laptop, mustNot brand=Apple | ids `{p03,p04,p10}` |
| bool should optional | must category=laptop, should name 'pro' (no msm) | ids `{p01,p02,p03,p04,p10}` (should only boosts) |
| minimum_should_match=1 | must category=laptop, should name 'pro'/'air', msm=1 (java-client) | ids `{p01,p02}` |
| invalid clause type | bool with type `wildcard` | 400 `Invalid request` (`must[0].type: type must be 'term' or 'match'`) |

## No-leak verification
- App logs contained only startup/seed info and validation warnings — no API key, no `Authorization` header, no host/URI.
- Error bodies are generic `ProblemDetail`s. No credentials in source or committed docs; `.env` gitignored, `.env.example` has placeholders only.

## Known limitations
- `_score` values are not asserted exactly (depend on Lucene scoring); tests assert membership/ordering instead — by design.
- 401/403 auth-failure mapping is inherited from Chapters 02/03 (same handler pattern) and not separately re-exercised end-to-end here; the success path uses a correctly-privileged key.
- Immediate refresh on seed is a learning/demo choice, not a production default (documented).
- HTTP-layer TLS intentionally disabled for local learning (documented; production requires HTTPS).

## Overall status
**COMPLETED** — `mvn clean verify` passes with **17 unit + 11 integration** tests against a real security-enabled Elasticsearch 9.4.8 using a least-privilege API key; real cURL confirms term (exact hit/miss), match (full-text), and bool (must/should/must_not/minimum_should_match) behavior across both clients with cross-client consistency and empty-result-as-200. No credential leakage. Chapters 01–03 untouched; Chapter 04 runs independently.
