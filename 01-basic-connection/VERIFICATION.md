# Chapter 01 Verification Report

> This run documents the **final code-quality review**: timeout classification (504) added to the
> exception handler on top of the existing 503/502/500 mapping, matching tests, README sync, and
> git traceability. All mandatory gates were re-executed against a real Elasticsearch 9.4.8.

- **Timestamp (with timezone):** 2026-10-23 16:37:22 +07 (ICT, Asia/Bangkok)
- **Git metadata:**
  - Branch: `main`
  - Committed revision at verification time: `cc8b2ce` (`cc8b2ceae3eada351d791750aae64c6302c95970`) — "fix(01-basic-connection): classify ES errors 503/502/500 in GlobalExceptionHandler"
  - This review's changes were **uncommitted at verification time** (verified in the working tree). Modified: `README.md`, `exception/GlobalExceptionHandler.java`, `exception/GlobalExceptionHandlerTest.java`, `exception/ErrorThrowingTestController.java`. Commit after approval to bind this report to a concrete revision.
- **Versions:**
  - Java: OpenJDK Temurin **21.0.2** (LTS, arm64)
  - Maven: Apache **3.9.9** (invoked with `JAVA_HOME` → Temurin 21.0.2)
  - Spring Boot: **4.1.2**; Spring Framework: 7.0.10
  - Spring Data Elasticsearch: **6.1.2** (via Spring Data BOM 2026.0.2)
  - Elasticsearch Java API Client (`co.elastic.clients`): **9.4.8**
  - Elasticsearch server image: **elasticsearch:9.4.8** (arm64)
  - Testcontainers: 2.0.5; Docker: client/server **25.0.2**; Compose v2.24.3-desktop.1
- **Environment:** macOS (arm64). Default `java`/`mvn` resolve to Java 17; JDK 21 selected explicitly via `JAVA_HOME`. Docker Desktop daemon running.

## What changed in this run
- **Exception handling** (`GlobalExceptionHandler`): added **504 Gateway Timeout** classification for timeout failures (`SocketTimeoutException`, `java.util.concurrent.TimeoutException`), on top of the existing mapping:
  - timeout → **504** (`Elasticsearch timeout`)
  - unreachable (`ConnectException` / `UnknownHostException` / `IOException`, incl. `TransportException`) → **503** (`Elasticsearch unavailable`)
  - ES error response (`ElasticsearchException`) → **502** (`Elasticsearch returned an error`)
  - unexpected → **500** (`Internal server error`)
  Because `SocketTimeoutException` is itself an `IOException`, the wrapped-cause walk checks timeout **before** connectivity so a wrapped timeout is correctly 504 (not 503). Bodies remain generic `ProblemDetail`; no host/URI/credential/stack-trace leakage. No custom exception classes introduced.
- **Tests:** added 3 cases to `GlobalExceptionHandlerTest` (socket-timeout → 504 + no-leak, reactive `TimeoutException` → 504, wrapped-timeout → 504 not 503) and the matching routes to the test-only `ErrorThrowingTestController`.
- **README:** error table now includes the 504 row + a note on timeout-before-connectivity ordering; testing and takeaways updated.
- **Preserved:** endpoint paths, successful response contracts, controller/service separation, DTO structures, reactive integration, blocking-client isolation, all previously existing tests. No new dependencies.

## Gate results

| Gate | Command / evidence | Expected | Actual | Status |
|---|---|---|---|---|
| Maven build + unit tests | `mvn clean verify` (surefire) | BUILD SUCCESS, unit pass | 21 unit tests, 0 failures | PASS |
| Integration tests (real ES) | `integration/ElasticsearchConnectionIT` (Testcontainers 9.4.8) | all pass | 4/4 pass | PASS |
| Full build | `mvn clean verify` | BUILD SUCCESS | BUILD SUCCESS (26.2s) | PASS |
| Docker startup + readiness | `docker compose -p ch01 up -d` + `/_cluster/health` + `ps` | reachable, healthy | `green`, 1 node; `Up (healthy)`, `127.0.0.1:9200` | PASS |
| Application startup | `java -jar target/basic-connection.jar` | listening | Netty on 8080 | PASS |
| cURL success (4 endpoints) | see below | 200 + expected body | all 200, correct live data | PASS |
| Field assertions | `jq -e` | fields valid | all PASS | PASS |
| Failure path — unreachable | `docker compose stop elasticsearch` + cURL | 503, no leak | both clients 503 "Elasticsearch unavailable", no leak | PASS |
| Failure path — timeout (504) | `GlobalExceptionHandlerTest` (socket/reactive/wrapped) | 504, no leak | 3/3 pass | PASS (unit) |
| Error classification regression | `GlobalExceptionHandlerTest` | 504/503/502/500 + no leak | 8/8 pass | PASS |
| Blocking execution isolation | `JavaClientConnectionServiceTest` | boundedElastic, lazy | 2/2 pass | PASS |
| Regression (paths/contracts) | compare pre/post | unchanged | identical paths/statuses/success fields | PASS |
| README verification | executed up/readiness/verify/curl/down | works as documented | all commands succeeded | PASS |

## Test execution evidence (counts)
Surefire (unit) — total **21**, 0 failures:
- `controller.JavaClientConnectionControllerTest` — 2
- `controller.SpringDataConnectionControllerTest` — 3
- `exception.GlobalExceptionHandlerTest` — 8
- `service.JavaClientConnectionServiceTest` — 5
- `service.SpringDataConnectionServiceTest` — 3

Failsafe (integration): `integration.ElasticsearchConnectionIT` — 4, 0 failures. Final line: `BUILD SUCCESS`.

## Verified cURL requests (real app vs live ES 9.4.8)

Success (all HTTP 200):
- `GET /api/spring-data/elasticsearch/health` → `{"client":"spring-data","clusterName":"docker-cluster","status":"green","numberOfNodes":1,"reachable":true}`
- `GET /api/spring-data/elasticsearch/cluster-info` → `{"client":"spring-data","clusterName":"docker-cluster","clusterUuid":"_WpeovlHQXqGrEYOzvFhaQ","versionNumber":"9.4.8","tagline":"You Know, for Search"}`
- `GET /api/java-client/elasticsearch/health` → `{"client":"java-client","clusterName":"docker-cluster","status":"green","numberOfNodes":1,"reachable":true}`
- `GET /api/java-client/elasticsearch/cluster-info` → `{"client":"java-client","clusterName":"docker-cluster","clusterUuid":"_WpeovlHQXqGrEYOzvFhaQ","versionNumber":"9.4.8","tagline":"You Know, for Search"}`
- Field assertions (`jq -e`): all PASS. Both clients returned the same `clusterUuid`, confirming a real shared cluster.

Failure — Elasticsearch stopped (`docker compose -p ch01 stop elasticsearch`):
- `GET /api/spring-data/.../health` → **503** `{"title":"Elasticsearch unavailable","status":503,"detail":"Could not reach Elasticsearch. ...","instance":"/api/spring-data/elasticsearch/health"}`
- `GET /api/java-client/.../health` → **503** (same shape)
- Assertion `status==503 && title=="Elasticsearch unavailable" && detail has no "9200"` → PASS (no leak).

Timeout (504): verified at the **unit level** in `GlobalExceptionHandlerTest` (socket/reactive/wrapped timeout → 504, no leak). A real network timeout against a live node is non-deterministic/flaky to force in an end-to-end run, so the verification boundary for 504 is the deterministic unit tests. This is stated here rather than claimed as an executed cURL check.

## Regression result
- Endpoint paths unchanged; successful response DTO fields unchanged; reactive safety unchanged (Java API Client still offloaded to `boundedElastic`, no `.block()` in request handling). The only intentional behavioral change this run is the **added 504 timeout classification** (previously a socket timeout would have been 503). Status: **PASS**.

## Known issues / limitations / cleanup
- No test failures. No gates BLOCKED or NOT VERIFIED.
- 504 timeout path is verified via unit tests only (see above); the unreachable 503 path is verified end-to-end with real ES.
- Environment note: machine default `java`/`mvn` are Java 17; JDK 21 used explicitly via `JAVA_HOME`.
- Cleanup: app stopped; `docker compose -p ch01 down` removed container + network (no persistent volume).
- Mockito self-attach warning (JDK agent dynamic-load deprecation) is informational only.

## Overall status
**COMPLETED** — all mandatory gates passed with real execution evidence (21 unit + 4 integration tests, real Dockerized ES 9.4.8, real app startup, real cURL success + classified unreachable failure; timeout classification verified via unit tests). Documentation is synchronized with the code. Changes remain uncommitted pending user approval.
