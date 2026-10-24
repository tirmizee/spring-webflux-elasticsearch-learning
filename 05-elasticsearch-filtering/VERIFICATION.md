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

---

# Chapter 05 Re-verification — input validation findings #1, #2, #3

> Separate entry for the code-review follow-up that hardened input validation. The original
> verification report above is preserved unchanged.

- **Timestamp (with timezone):** 2026-10-25 00:46:37 +07 (ICT, Asia/Bangkok)
- **Source revision:** branch `main`, repository HEAD `5aa4f32` (`feat(05-elasticsearch-filtering): add term/terms, range, exists and bool filter chapter`) at the time of this run; the validation changes were verified from the **uncommitted working tree** on top of `5aa4f32`.
- **Environment:** Java 21.0.2 (Temurin); Maven 3.9.9 (JAVA_HOME→21); Spring Boot 4.1.2; Spring Data ES 6.1.2; ES client/server 9.4.8; Testcontainers 2.0.5; Docker 25.0.2.

## Findings addressed
- **Finding #1 — reject unsupported range types.** `ProductFilters.range()` now rejects any `type` other than `number`/`date` with `InvalidFilterException` (→ 400), so an unsupported type can never silently fall back to a numeric range, even when invoked directly. The DTO `@Pattern(regexp = "number|date")` on `RangeFilter.type` continues to validate the POST compose body. Status: **FIXED**.
- **Finding #2 — reject non-finite numeric bounds.** `ProductFilters.parseDouble()` now checks `Double.isFinite(...)` after parsing, so `NaN`/`Infinity`/`-Infinity` are rejected with `InvalidFilterException` (→ 400). Malformed-string handling (`NumberFormatException`) is preserved. Status: **FIXED**.
- **Finding #3 — validate every terms value.** `TermsFilter.values` is now `List<@NotBlank String>`, rejecting `null`/empty/whitespace-only elements via the container-element constraint, activated by the existing `List<@Valid TermsFilter>` in `FilterSearchRequest` (covers both `terms` and `mustNotTerms`). The GET `/terms` controller helper was tightened to reject any null/blank element for a consistent contract. Status: **FIXED**.

## Change under test
- `query/ProductFilters.java` — `range()` now throws on unsupported type (defensive); `parseDouble()` adds a finite-value check.
- `dto/TermsFilter.java` — `values` element type is `@NotBlank`; Javadoc updated.
- `controller/SpringDataFilterController.java` and `controller/JavaClientFilterController.java` — `requireNonEmpty(...)` now rejects any null/blank element (previously passed if any element was non-blank).
- No change to mappings, Query DSL semantics, `bool.filter`/`must_not` composition, full-text behavior, scoring, reactive/blocking execution, endpoint paths, data initialization, or auth.

## Gate results (this run)
| Gate | Command / evidence | Expected | Actual | Status |
|---|---|---|---|---|
| Maven build + unit tests | `mvn clean verify` (surefire) | BUILD SUCCESS | 48 unit tests, 0 failures | PASS |
| Integration tests (failsafe, real secured ES) | `mvn clean verify` (failsafe) | all pass | 17 tests, 0 failures | PASS |
| Full `mvn clean verify` | exit code + final line | BUILD SUCCESS | BUILD SUCCESS | PASS |
| Finding #1: unsupported range type (GET both clients + compose) | real cURL → 400 | 400 | 400 (SD/JC GET, compose) | PASS |
| Finding #1: valid number/date types preserved | real cURL → 200 | 200 | 200 | PASS |
| Finding #2: NaN / Infinity / -Infinity / abc | real cURL → 400 | 400 | 400 (all four) | PASS |
| Finding #2: valid int/decimal preserved | real cURL → 200 | 200 | 200 | PASS |
| Finding #3: terms null/blank/empty (compose terms + mustNotTerms) | real cURL → 400 | 400 | 400 | PASS |
| Finding #3: GET /terms blank element | real cURL → 400 | 400 | 400 | PASS |
| Finding #3: valid terms preserved | real cURL → 200 | 200 | 200 | PASS |
| Regression: valid range/terms ids unchanged | real cURL + `jq` | same ids as before | identical | PASS |
| Sanitized error body | `jq` on 400 responses | no key/host/stack | `ProblemDetail` (title/status/detail only) | PASS |

## Actual test counts (this run)
Surefire (unit) — total **48**, 0 failures:
- `controller.JavaClientFilterControllerTest` — 12
- `controller.SpringDataFilterControllerTest` — 16
- `query.ProductFiltersTest` — 16
- `service.JavaClientFilterServiceTest` — 4

Failsafe (integration) — `integration.FilteringIT` — **17**, 0 failures. Final line: `BUILD SUCCESS`.

## Verified cURL (real app on :8084 + real secured ES 9.4.8)
| Request | Result |
|---|---|
| `GET range?field=price&type=invalid&gte=1` (both clients) | 400 `Invalid filter` (`range type must be 'number' or 'date' but was 'invalid'`) |
| `POST compose` range `type=wildcard` | 400 |
| `GET range?field=price&type=number&gte=1000` | 200, ids `{p01,p02,p03,p04,p05,p06}` (unchanged) |
| `GET range?field=releaseDate&type=date&gte=2024-01-01` | 200 |
| `GET range?field=price&type=number&gte=NaN` | 400 `Invalid filter` (`must be a finite number but was 'NaN'`) |
| `GET range?field=price&type=number&lte=Infinity` | 400 |
| `GET range?field=price&type=number&gt=-Infinity` | 400 |
| `GET range?field=price&type=number&gte=abc` | 400 |
| `GET range?field=price&type=number&gte=100` / `gte=99.99` | 200 |
| `POST compose` terms `[null]` / `[""]` / `["   "]` / `[]` | 400 |
| `POST compose` mustNotTerms `[null]` | 400 |
| `GET terms?field=brand&values=Apple&values=%20%20` | 400 |
| `POST compose` terms `["laptop"]` (valid) | 200 |
| `GET range price>1000` (exclusive boundary) | 200, ids `{p01,p02,p03,p04,p05}` (unchanged) |
| `GET terms brand=Apple,Dell` | 200, ids `{p01,p02,p03,p06,p10}` (unchanged) |

All error bodies are generic `ProblemDetail`s (title/status/detail) — no API key, host, URI, or stack trace. No invalid request reached Elasticsearch (the invalid-filter checks run before the ES call).

## New / updated tests
- `ProductFiltersTest` (+5): unsupported type when invoked directly; `NaN`; `Infinity`; `-Infinity`; finite int/decimal bounds preserved.
- `SpringDataFilterControllerTest` (+4): compose terms null element; compose terms blank element; compose mustNotTerms blank element; GET `/terms` blank element.
- `JavaClientFilterControllerTest` (+3): compose terms blank element; compose mustNotTerms null element; GET `/terms` blank element.
- `FilteringIT` (+2): unsupported range type rejected by both services; non-finite numeric bounds rejected by both services (proving fail-fast before ES).

## Cleanup
App process stopped; `docker compose -p ch05 down` removed the container and network (no named volume defined → no persistent data deleted); temp API-key file removed.

## Status
PASS — findings #1, #2, #3 fixed and verified end-to-end. Existing valid behavior (mappings, Query DSL, filter/query context, scoring, two-client consistency, endpoint contracts) preserved. Chapters 01–04 untouched; root README unchanged.
