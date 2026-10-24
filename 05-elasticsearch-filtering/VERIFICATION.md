# Chapter 05 Verification Report

- **Timestamp (with timezone):** 2026-10-25 00:25:58 +07 (ICT, Asia/Bangkok)
- **Source revision verified:**
  - Branch: `main`
  - Repository HEAD at verification time: `034c489` (`docs: add Chapter 14 (field-collapse) to roadmap as planned`).
  - Chapter 05 (`05-elasticsearch-filtering/`) was **untracked/new** and was verified from the **uncommitted working tree** on top of `034c489`. The commit that will contain this chapter is a later revision; this report documents the source state that was actually executed, not that later commit.
- **Versions:**
  - Java: OpenJDK Temurin **21.0.2** (arm64); Maven 3.9.9 (via `JAVA_HOME` → JDK 21)
  - Spring Boot **4.1.2**; Spring Data Elasticsearch **6.1.2** (BOM-managed)
  - Elasticsearch Java API Client **9.4.8** (`co.elastic.clients`, BOM-managed); Elasticsearch server image **9.4.8**
  - Testcontainers **2.0.5** (`${testcontainers.version}` from the Spring Boot BOM); Lombok 1.18.x (BOM-managed, compile-time only)
  - Docker 25.0.2; Docker Compose v2.24.3-desktop.1
- **Environment:** macOS (arm64). Default `java`/`mvn` are Java 17; JDK 21 selected explicitly via `JAVA_HOME`. Docker Desktop running.

## Scope recap
Elasticsearch filtering: `term`/`terms` (keyword + boolean), `range` (numeric inclusive/exclusive and date), `exists`, and composed `bool` filters, with a focus on **filter context vs query context** and the effect on `_score`. A deterministic 10-product catalog in index `filter-products`, implemented with two clients that run the same `co.elastic.clients` query DSL (shared `ProductFilters`): Spring Data reactive (`NativeQuery`) and the official Java API Client (synchronous, offloaded to `boundedElastic`). Security-enabled ES + least-privilege API key (Chapters 02/03/04 conventions). Independent project; Chapters 01–04 untouched.

## Version-selection gate (performed before coding)
The `co.elastic.clients` 9.4.8 query DSL for `range` differs from older examples: `RangeQuery` is a tagged union with `number`/`date`/`untyped`/`longNumber`/`term` variants, and bound setters (`gte`/`lte`/`gt`/`lt`) live on `RangeQueryBase$AbstractBuilder<T,…>` (`T` = `Double` for number, `String` for date). `terms` uses `TermsQueryField.value(List<FieldValue>)`. These signatures were confirmed by inspecting the resolved jar (`javap`) before writing `ProductFilters`, and then proven by a passing `ProductFiltersTest` and real integration tests — no guessed API.

## Key design decisions
- **Shared filter builder** (`query/ProductFilters`) consumed by both clients, so Spring Data and the Java API Client run the identical query and differ only in execution. Stateless helper, no interface.
- **Filter context demonstrated structurally:** leaf filters are placed in `bool.filter`, so pure-filter responses carry a constant `_score` of `0.0`; a full-text `match` (in `bool.must`) runs in query context and produces a positive `_score`. Both paths normalize the "no score" case to `0.0` for deterministic responses.
- **New field set vs Chapter 04:** `releaseDate` (date), `inStock` (boolean), `discountPercent` (nullable integer) make date-range, boolean-term, and `exists` observable without duplicating Chapter 04.
- **Deterministic boundaries:** `price = 1000.00` exactly (`p06`) proves inclusive (`gte`) vs exclusive (`gt`); `rating = 4.0` exactly (`p03`) proves exclusive `gt`/`lt` boundaries.
- **Empty result → HTTP 200 with empty list** (never 404); validation and invalid-filter semantics → HTTP 400 before any ES call.
- **Blocking Java client isolated** on `boundedElastic`; Spring Data path fully reactive.
- Lombok used only on the `Product` entity (targeted annotations); DTOs are records.

## Commands executed
- `mvn clean verify` (from `05-elasticsearch-filtering`) — BUILD SUCCESS (surefire + failsafe)
- `docker compose -p ch05 up -d` + authenticated readiness probe (`/_cluster/health?wait_for_status=yellow`)
- Create API key via `POST /_security/api_key` (least-privilege, index-level on `filter-products`)
- `mvn -DskipTests package` then `java -jar target/elasticsearch-filtering.jar` with `ELASTICSEARCH_API_KEY`
- Real cURL for term/terms/range/exists/compose across both clients (success, empty, and 400 paths)
- `docker compose -p ch05 down` + temp key file removed

## Gate results

| Gate | Command / evidence | Expected | Actual | Status |
|---|---|---|---|---|
| Maven build + unit tests | `mvn clean verify` (surefire) | BUILD SUCCESS | 36 unit tests, 0 failures | PASS |
| Integration tests (failsafe, real secured ES) | `mvn clean verify` (failsafe) | all pass | 15 tests, 0 failures | PASS |
| Full `mvn clean verify` | exit code + final line | BUILD SUCCESS | BUILD SUCCESS (~34s) | PASS |
| Docker startup + readiness | `docker compose -p ch05 up -d`, auth health | healthy, authenticated 200 | container healthy; health HTTP 200 after 6 tries | PASS |
| API key (index privileges) created | `POST /_security/api_key` | `encoded` returned | created (encoded len 60) | PASS |
| Application startup | `java -jar …` | listening, index seeded | Netty port 8084; "Created index 'filter-products' and seeded 10 sample products" | PASS |
| cURL term/terms/range/exists | per table below | expected ids | all matched | PASS |
| cURL compose (filter + query) | per table below | expected ids + score behavior | all matched | PASS |
| Field/score assertions | `jq` on responses | 0.0 in filter context; >0 in query context | confirmed | PASS |
| Input validation (400) | blank/missing/invalid filter | HTTP 400 ProblemDetail | all 400 | PASS |
| No credential leakage | app logs | none | only startup/seed logs; no key/Authorization/host | PASS |
| Chapters 01–04 unchanged | not touched | untouched | untouched | PASS |
| Chapter 05 independent | builds/runs standalone | yes | yes | PASS |

## Actual test counts (this run)
Surefire (unit) — total **36**, 0 failures:
- `controller.JavaClientFilterControllerTest` — 9
- `controller.SpringDataFilterControllerTest` — 12
- `query.ProductFiltersTest` — 11
- `service.JavaClientFilterServiceTest` — 4

Failsafe (integration) — `integration.FilteringIT` — **15**, 0 failures (term exact, boolean term, terms OR, numeric range inclusive/exclusive, rating exclusive bounds, date range, exists + complement, compose AND, filter-context zero score, query-context positive score, compose must_not, empty result, cross-client consistency). Final line: `BUILD SUCCESS`.

## Verified cURL results (real app on :8084 + real secured ES 9.4.8)
| Scenario | Request | Result |
|---|---|---|
| term exact (keyword) | `GET term?field=category&value=laptop` (both clients) | 200, ids `{p01,p02,p03,p04,p05}` (consistent) |
| boolean term | `GET term/boolean?field=inStock&value=true` (spring-data) | 200, ids `{p01,p02,p04,p06,p07,p08,p10}` |
| terms (OR) | `GET terms?field=brand&values=Apple,Dell` (java-client) | 200, ids `{p01,p02,p03,p06,p10}` |
| range inclusive lower | `GET range?field=price&type=number&gte=1000` (spring-data) | 200, ids `{p01,p02,p03,p04,p05,p06}` (p06 @1000 included) |
| range exclusive lower | `GET range?field=price&type=number&gt=1000` (java-client) | 200, ids `{p01,p02,p03,p04,p05}` (p06 @1000 excluded) |
| range between (exclusive) | `GET range?field=rating&type=number&gt=4.0&lt=4.5` (spring-data) | 200, ids `{p04,p06,p08,p10}` |
| date range | `GET range?field=releaseDate&type=date&gte=2024-01-01&lte=2024-12-31` (java-client) | 200, ids `{p01,p02,p06,p09}` |
| exists | `GET exists?field=discountPercent` (spring-data) | 200, ids `{p01,p03,p05,p06,p08,p10}` |
| compose AND | `POST compose` terms laptop + price lte 1500 (spring-data) | 200, ids `{p02,p03,p04}` |
| compose must_not | `POST compose` terms laptop, mustNot brand Apple (java-client) | 200, ids `{p03,p04,p05}` |
| filter-context score | `GET term?field=category&value=laptop` scores | 200, unique scores `[0.0]` (both clients) |
| query + filter | `POST compose` query 'gaming' + terms laptop | 200, ids `{p05}`, score `1.460995` (> 0); java-client also > 0 |
| empty result | `GET term?field=category&value=nonexistent` | 200, `total=0`, empty hits (not 404) |
| invalid: blank field | `GET term?field=&value=laptop` | 400 `Invalid request` |
| invalid: blank exists field | `GET exists?field=` (java-client) | 400 `Invalid request` |
| invalid: bad range type | `POST compose` range type `wildcard` | 400 `Invalid request` |
| invalid: range no bound | `GET range?field=price&type=number` | 400 `Invalid filter` (`range filter on 'price' must set at least one of gte/lte/gt/lt`) |
| invalid: non-numeric bound | `GET range?field=price&type=number&gte=abc` (java-client) | 400 `Invalid filter` |

## Verified cURL requests (representative)
- **Command:** `curl -s "$base/api/spring-data/products/filter/range?field=price&type=number&gte=1000"`
  - Expected: 200, hits ids `{p01,p02,p03,p04,p05,p06}` (inclusive boundary includes `p06` @ 1000)
  - Actual: 200, ids `["p01","p02","p03","p04","p05","p06"]`
  - Assertion result: PASS
- **Command:** `curl -s -XPOST "$base/api/java-client/products/filter/compose" -d '{"query":"gaming","terms":[{"field":"category","values":["laptop"]}]}'`
  - Expected: 200, ids `{p05}`, score > 0 (query context)
  - Actual: 200, `{"client":"java-client","total":1,"id":"p05","score":1.460995}`
  - Assertion result: PASS

## No-leak verification
- App logs contained only startup/seed info — no API key, no `Authorization` header, no host/URI.
- Error bodies are generic `ProblemDetail`s (title/status/detail only). No credentials in source or committed docs; `.env` gitignored, `.env.example` has placeholders only.

## Failures, limitations, cleanup
- `_score` values are not asserted exactly (depend on Lucene scoring). Filter context is asserted as exactly `0.0`; query context is asserted as `> 0` plus membership — by design.
- Filter caching (node-level query cache) is explained conceptually in the README only; it is not asserted in tests because a cache-hit measurement is not deterministic at this scale — documented limitation, not a skipped mandatory gate.
- 401/403 auth-failure mapping is inherited from the Chapters 02–04 handler pattern and not separately re-exercised end-to-end here; the success path uses a correctly-privileged key.
- Immediate refresh on seed is a learning/demo choice, not a production default (documented).
- HTTP-layer TLS intentionally disabled for local learning (documented; production requires HTTPS).
- Cleanup performed: app process stopped, `docker compose -p ch05 down` removed the container and network (no named volume is defined, so no persistent data was deleted), temp API-key file removed.

## Overall status
**COMPLETED** — `mvn clean verify` passes with **36 unit + 15 integration** tests against a real security-enabled Elasticsearch 9.4.8 using a least-privilege API key; real cURL on a running app (:8084) confirms term/terms (keyword + boolean), range (numeric inclusive/exclusive and date), exists, and composed bool filters across both clients, with cross-client consistency, deterministic boundary behavior, filter-context `_score` = 0.0 vs query-context `_score` > 0, empty-result-as-200, and 400 validation. No credential leakage. Chapters 01–04 untouched; Chapter 05 runs independently.
