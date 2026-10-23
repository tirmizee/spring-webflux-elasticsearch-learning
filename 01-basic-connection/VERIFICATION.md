# Chapter 01 Verification Report

> This run is a **consistency review**: it re-verifies the committed implementation (error
> classification 504/503/502/500) against source, tests, README, and a real Elasticsearch 9.4.8,
> and corrects stale Git metadata in a previous version of this report.

- **Timestamp (with timezone):** 2026-10-23 16:48:28 +07 (ICT, Asia/Bangkok)
- **Source revision used for verification:**
  - Branch: `main`
  - Verified commit: `d2dcf00` (`d2dcf0093cf1a5cc3d0272ae90051af6d7b3676a`) — "fix(01-basic-connection): classify ES timeouts as 504 gateway timeout"
  - Working tree status at verification time: **clean** (`git status --short` empty). The source code, tests, and README described here were all **committed** at `d2dcf00`.
  - `d2dcf00` is the original revision used during this verification run and is preserved here as historical evidence; it is not updated to later HEAD values.
- **Versions:**
  - Java: OpenJDK Temurin **21.0.2** (LTS, arm64)
  - Maven: Apache **3.9.9** (invoked with `JAVA_HOME` → Temurin 21.0.2)
  - Spring Boot: **4.1.2**; Spring Framework: 7.0.10
  - Spring Data Elasticsearch: **6.1.2** (via Spring Data BOM 2026.0.2)
  - Elasticsearch Java API Client (`co.elastic.clients`): **9.4.8**
  - Elasticsearch server image: **elasticsearch:9.4.8** (arm64)
  - Testcontainers: 2.0.5; Docker: client/server **25.0.2**; Compose v2.24.3-desktop.1
- **Environment:** macOS (arm64). Default `java`/`mvn` resolve to Java 17; JDK 21 selected explicitly via `JAVA_HOME`. Docker Desktop daemon running.

## Consistency review findings
Each previously-reported issue was checked against the committed working tree (`d2dcf00`):

| # | Reported issue | Verified against current code | Verdict |
|---|---|---|---|
| 1 | Handler returns 503 for all unexpected exceptions | `handleUnexpected` returns **500** for non-connectivity/non-timeout errors | NOT PRESENT (already fixed) |
| 2 | Tests expect different statuses (500/502/503/504) than code | Handler emits 504/503/502/500; `GlobalExceptionHandlerTest` asserts the same; build passes | NOT PRESENT (consistent) |
| 3 | README describes behavior not matching code | README error table matches handler exactly (504/503/502/500 + titles) | NOT PRESENT (consistent) |
| 4 | VERIFICATION.md test counts inconsistent with test files | Report said 21 unit / 4 IT; actual run = 21 unit / 4 IT | NOT PRESENT (counts correct) |
| 5 | Report references uncommitted changes | Previous report claimed HEAD `cc8b2ce` + "uncommitted"; actual HEAD is `d2dcf00`, tree clean | **CONFIRMED** — fixed in this report |

Conclusion: issues 1–4 described an earlier state already resolved by commits `cc8b2ce` and `d2dcf00`. Only issue 5 (stale Git metadata in this report) was still present; it is corrected here. No source/test/README changes were required.

## Current error classification (committed `d2dcf00`)
- timeout (`SocketTimeoutException`, `java.util.concurrent.TimeoutException`) → **504** `Elasticsearch timeout`
- unreachable (`ConnectException` / `UnknownHostException` / `IOException`, incl. `TransportException`) → **503** `Elasticsearch unavailable`
- ES error response (`ElasticsearchException`) → **502** `Elasticsearch returned an error`
- unexpected application error → **500** `Internal server error`
- Wrapped causes are walked (timeout checked before connectivity, since `SocketTimeoutException` is an `IOException`). Bodies are generic `ProblemDetail`; no host/URI/credential/stack-trace leakage.

## Gate results (this run)

| Gate | Command / evidence | Expected | Actual | Status |
|---|---|---|---|---|
| Maven build + unit tests | `mvn clean verify` (surefire) | BUILD SUCCESS, unit pass | 21 unit tests, 0 failures | PASS |
| Integration tests (real ES) | `integration/ElasticsearchConnectionIT` (Testcontainers 9.4.8) | all pass | 4/4 pass | PASS |
| Full build | `mvn clean verify` | BUILD SUCCESS | BUILD SUCCESS (30.7s) | PASS |
| Docker startup + readiness | `docker compose -p ch01 up -d` + `/_cluster/health` + `ps` | reachable, healthy | `green`, 1 node; `Up (healthy)`, `127.0.0.1:9200` | PASS |
| Application startup | `java -jar target/basic-connection.jar` | listening | Netty on 8080 | PASS |
| cURL success (4 endpoints) | see below | 200 + expected body | all 200, correct live data | PASS |
| Field assertions | `jq -e` | fields valid | all PASS | PASS |
| Failure path — unreachable | `docker compose stop elasticsearch` + cURL | 503, no leak | both clients 503 "Elasticsearch unavailable", no leak | PASS |
| Error classification (unit) | `GlobalExceptionHandlerTest` | 504/503/502/500 + no leak | 8/8 pass | PASS |
| Timeout classification (504) | `GlobalExceptionHandlerTest` (socket/reactive/wrapped) | 504, no leak | 3/3 pass | PASS (unit) |
| Blocking execution isolation | `JavaClientConnectionServiceTest` | boundedElastic, lazy | 2/2 pass | PASS |
| Regression (paths/contracts) | compare success responses | unchanged | identical paths/statuses/success fields | PASS |
| README verification | executed up/readiness/verify/curl/down | works as documented | all commands succeeded | PASS |

## Actual test counts (this run — not reused from prior runs)
Surefire (unit) — total **21**, 0 failures:
- `controller.JavaClientConnectionControllerTest` — 2
- `controller.SpringDataConnectionControllerTest` — 3
- `exception.GlobalExceptionHandlerTest` — 8
- `service.JavaClientConnectionServiceTest` — 5
- `service.SpringDataConnectionServiceTest` — 3

Failsafe (integration): `integration.ElasticsearchConnectionIT` — **4**, 0 failures. Final line: `BUILD SUCCESS`.

## Verified cURL requests (real app vs live ES 9.4.8)

Success (all HTTP 200):
- `GET /api/spring-data/elasticsearch/health` → `{"client":"spring-data","clusterName":"docker-cluster","status":"green","numberOfNodes":1,"reachable":true}`
- `GET /api/spring-data/elasticsearch/cluster-info` → `{"client":"spring-data","clusterName":"docker-cluster","clusterUuid":"Y-t0YowdQ02VnARRh0zJ7g","versionNumber":"9.4.8","tagline":"You Know, for Search"}`
- `GET /api/java-client/elasticsearch/health` → `{"client":"java-client","clusterName":"docker-cluster","status":"green","numberOfNodes":1,"reachable":true}`
- `GET /api/java-client/elasticsearch/cluster-info` → `{"client":"java-client","clusterName":"docker-cluster","clusterUuid":"Y-t0YowdQ02VnARRh0zJ7g","versionNumber":"9.4.8","tagline":"You Know, for Search"}`
- Field assertions (`jq -e`): all PASS. Both clients returned the same `clusterUuid`, confirming a real shared cluster.

Failure — Elasticsearch stopped (`docker compose -p ch01 stop elasticsearch`):
- `GET /api/spring-data/.../health` → **503** `{"title":"Elasticsearch unavailable","status":503,"detail":"Could not reach Elasticsearch. ...","instance":"/api/spring-data/elasticsearch/health"}`
- `GET /api/java-client/.../health` → **503** (same shape)
- Assertion `status==503 && title=="Elasticsearch unavailable" && detail has no "9200"` → PASS (no leak).

Timeout (504): verified at the **unit level** in `GlobalExceptionHandlerTest` (socket/reactive/wrapped timeout → 504, no leak). A real network timeout against a live node is non-deterministic to force end-to-end, so the verification boundary for 504 is the deterministic unit tests. Stated here rather than claimed as an executed cURL check.

## Regression result
Endpoint paths, successful response DTO fields, controller/service separation, DTO structures, reactive integration, and blocking-client isolation are all unchanged. No source/test/README changes were made in this review. Status: **PASS**.

## Remaining issues
- None in source, tests, or README (verified consistent with committed `d2dcf00`).
- The 504 timeout path is verified via unit tests only (real end-to-end timeout is non-deterministic).
- Environment note: machine default `java`/`mvn` are Java 17; JDK 21 used explicitly via `JAVA_HOME`.
- Cleanup: app stopped; `docker compose -p ch01 down` removed container + network (no persistent volume).

## Overall status
**COMPLETED** — the committed implementation is internally consistent (source ↔ tests ↔ README) and passes real verification (21 unit + 4 integration, real Dockerized ES 9.4.8, real cURL success + 503 failure). The only correction made was to this report's stale Git metadata; no code, test, or README changes were necessary.
