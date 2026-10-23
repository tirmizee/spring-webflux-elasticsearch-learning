# Chapter 01 Verification Report

> This run documents the **final code-review improvements**: documentation sync, exception
> classification (503/502/500) with secure generic bodies, additional service and exception
> unit tests (including blocking-execution isolation), and git traceability. All mandatory
> gates were re-executed against a real Elasticsearch 9.4.8.

- **Timestamp (with timezone):** 2026-10-23 16:27:11 +07 (ICT, Asia/Bangkok)
- **Git metadata:**
  - Branch: `main`
  - Committed revision at verification time: `de75b1a` (`de75b1a588eae5dc89e5757425a692d2480bb286`) — "refactor(01-basic-connection): layered structure + steering best practices"
  - The code-review changes described here were **uncommitted at verification time** (working-tree modifications), verified in the working tree. Modified: `README.md`, `exception/GlobalExceptionHandler.java`, `controller/SpringDataConnectionControllerTest.java`, `service/JavaClientConnectionServiceTest.java`; added: `exception/GlobalExceptionHandlerTest.java`, `exception/ErrorThrowingTestController.java`. Commit after approval to bind this report to a concrete revision.
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
- **Documentation sync** (`README.md`): structure tree, Mermaid diagram, and testing section now match the refactored code; added an error-classification table and error-handling takeaway.
- **Exception handling** (`GlobalExceptionHandler`): no longer maps everything to 503. Now classifies:
  - connectivity/availability failures (`ConnectException` / `UnknownHostException` / `IOException`, incl. `TransportException`) → **503**;
  - Elasticsearch error responses (`ElasticsearchException`) → **502**;
  - unexpected application errors → **500**.
  Bodies remain generic `ProblemDetail` (no host/URI/credential/stack-trace leakage); full details are logged server-side. A bounded cause-chain walk catches connectivity failures wrapped by the reactive transport.
- **Tests added:** `exception/GlobalExceptionHandlerTest` (5 cases: 503 connect, 503 IO + no-leak, 502 ES response + no-leak, 503 wrapped-connect, 500 unexpected + no-leak) and 2 `JavaClientConnectionServiceTest` cases for **blocking execution isolation** (runs on `boundedElastic-*` worker, not caller thread; `Mono.fromCallable` laziness). Updated the Spring Data controller test's failure case to use a `ConnectException` (503).
- **Preserved:** endpoint paths, successful response contracts, reactive execution strategy, versions, dependencies (none added).

## Gate results

| Gate | Command / evidence | Expected | Actual | Status |
|---|---|---|---|---|
| Maven build + unit tests | `mvn clean verify` (surefire) | BUILD SUCCESS, unit tests pass | 18 unit tests, 0 failures | PASS |
| Integration tests (real ES) | `integration/ElasticsearchConnectionIT` (Testcontainers 9.4.8) | all pass | 4/4 pass | PASS |
| Full build | `mvn clean verify` | BUILD SUCCESS | BUILD SUCCESS (27.3s) | PASS |
| Docker startup + readiness | `docker compose -p ch01 up -d` + `/_cluster/health` + `ps` | reachable, healthy | `green`, 1 node; `Up (healthy)`, `127.0.0.1:9200` | PASS |
| Application startup | `java -jar target/basic-connection.jar` | listening | Netty on 8080 | PASS |
| cURL success (4 endpoints) | see below | 200 + expected body | all 200, correct live data | PASS |
| Field assertions | `jq -e` | fields valid | all PASS | PASS |
| Failure path — unreachable | `docker compose stop elasticsearch` + cURL | 503, no leak | both clients 503 "Elasticsearch unavailable", no leak | PASS |
| Error classification (unit) | `GlobalExceptionHandlerTest` | 503/502/500 + no leak | 5/5 pass | PASS |
| Blocking execution isolation (unit) | `JavaClientConnectionServiceTest` | runs on boundedElastic, lazy | 2/2 pass | PASS |
| Regression (paths/contracts) | compare pre/post | unchanged | identical paths/statuses/success fields | PASS |
| README verification | executed up/readiness/verify/curl/down | works as documented | all commands succeeded | PASS |

## Test execution evidence (counts)
Surefire (unit) — total **18**, 0 failures:
- `controller.JavaClientConnectionControllerTest` — 2
- `controller.SpringDataConnectionControllerTest` — 3
- `exception.GlobalExceptionHandlerTest` — 5
- `service.JavaClientConnectionServiceTest` — 5
- `service.SpringDataConnectionServiceTest` — 3

Failsafe (integration): `integration.ElasticsearchConnectionIT` — 4, 0 failures. Final line: `BUILD SUCCESS`.

## Verified cURL requests (real app vs live ES 9.4.8)

Success (all HTTP 200):
- `GET /api/spring-data/elasticsearch/health` → `{"client":"spring-data","clusterName":"docker-cluster","status":"green","numberOfNodes":1,"reachable":true}`
- `GET /api/spring-data/elasticsearch/cluster-info` → `{"client":"spring-data","clusterName":"docker-cluster","clusterUuid":"1hlN1keZQBqqeNGwkUVhvA","versionNumber":"9.4.8","tagline":"You Know, for Search"}`
- `GET /api/java-client/elasticsearch/health` → `{"client":"java-client","clusterName":"docker-cluster","status":"green","numberOfNodes":1,"reachable":true}`
- `GET /api/java-client/elasticsearch/cluster-info` → `{"client":"java-client","clusterName":"docker-cluster","clusterUuid":"1hlN1keZQBqqeNGwkUVhvA","versionNumber":"9.4.8","tagline":"You Know, for Search"}`
- Field assertions (`jq -e`): all PASS. Both clients returned the same `clusterUuid`, confirming a real shared cluster.

Failure — Elasticsearch stopped (`docker compose -p ch01 stop elasticsearch`):
- `GET /api/spring-data/.../health` → **503** `{"title":"Elasticsearch unavailable","status":503,"detail":"Could not reach Elasticsearch. ...","instance":"/api/spring-data/elasticsearch/health"}`
- `GET /api/java-client/.../health` → **503** (same shape)
- Assertion: `status==503 && title=="Elasticsearch unavailable" && detail does not contain "9200"/"127.0.0.1"` → PASS (no infrastructure leak).

## Regression result
- Endpoint paths unchanged; successful response DTO fields unchanged; reactive safety unchanged (Java API Client still offloaded to `boundedElastic`, no `.block()` in request handling). The only intentional behavioral change is **error status classification** (previously always 503; now 503/502/500 by cause), which is the requested improvement. Status: **PASS**.

## Known issues / limitations / cleanup
- No test failures. No gates BLOCKED or NOT VERIFIED.
- The error-classification behavior change is intentional and documented; it does not affect success-path contracts.
- Environment note: machine default `java`/`mvn` are Java 17; JDK 21 used explicitly via `JAVA_HOME`.
- Cleanup: app stopped; `docker compose -p ch01 down` removed container + network (no persistent volume).
- Mockito self-attach warning (JDK agent dynamic-load deprecation) is informational only.

## Overall status
**COMPLETED** — all mandatory gates passed with real execution evidence (18 unit + 4 integration tests, real Dockerized ES 9.4.8, real app startup, real cURL success + classified failure paths, regression confirmed). Documentation is synchronized with the code. Changes remain uncommitted pending user approval.
