# Chapter 01 Verification Report

- **Timestamp (with timezone):** 2026-10-23 15:48:40 +07 (ICT, Asia/Bangkok)
- **Git commit/branch (if available):** N/A — workspace is not a git repository (`git status` → "not a git repository"). No commits were made.
- **Versions:**
  - Java: OpenJDK Temurin **21.0.2** (LTS, arm64)
  - Maven: Apache **3.9.9** (invoked with `JAVA_HOME` → Temurin 21.0.2)
  - Spring Boot: **4.1.2**
  - Spring Framework: 7.0.10 (from Boot BOM)
  - Spring Data BOM: 2026.0.2 (Spring Data Elasticsearch reactive)
  - Elasticsearch Java API Client (`co.elastic.clients:elasticsearch-java`): **9.4.8** (Boot-managed)
  - Elasticsearch server image: **elasticsearch:9.4.8** (arm64)
  - Testcontainers: 2.0.5 (Boot-managed; modules `testcontainers-junit-jupiter`, `testcontainers-elasticsearch`)
  - Docker: client/server **25.0.2**; Docker Compose v2.24.3-desktop.1
- **Environment and preconditions:**
  - macOS (Apple Silicon / arm64). Default system `java`/`mvn` point to Java 17; JDK 21 was selected explicitly via `JAVA_HOME` for every Maven/app command.
  - Docker Desktop daemon was started before verification and confirmed running.
  - ES image `elasticsearch:9.4.8` pulled for `linux/arm64`.

## Version selection rationale
- Latest stable Spring Boot 4.x is **4.1.2** (4.2.0 is still RC per Maven Central `maven-metadata.xml`).
- The Boot 4.1.2 BOM pins `elasticsearch-client.version = 9.4.8` and uses the new `elasticsearch-rest5-client` infrastructure (replacing the deprecated low-level `RestClient`, per Boot 4.0 release notes).
- The Elasticsearch **server** image was pinned to **9.4.8** to exactly match the managed client version. Image tag confirmed to exist on Docker Hub.

## Gate results

| Gate | Command / evidence location | Expected | Actual | Status |
|---|---|---|---|---|
| Maven build + unit tests | `mvn clean test` (surefire) | BUILD SUCCESS, unit tests pass | 5 tests run, 0 failures (2 java-client controller + 3 spring-data controller) | PASS |
| Maven build + unit + integration | `mvn clean verify` (surefire + failsafe) | BUILD SUCCESS, all tests pass | BUILD SUCCESS; surefire 5/5, failsafe 4/4 | PASS |
| Docker startup + readiness | `docker compose -p ch01 up -d` then `/_cluster/health` + `docker compose ps` | cluster reachable, container healthy | `status: green`, 1 node; container `Up (healthy)`, bound `127.0.0.1:9200` | PASS |
| Application startup | `java -jar target/basic-connection.jar` | app listening | `Netty started on port 8080 (http)`, started in ~1.3s | PASS |
| Integration tests (real ES) | `ElasticsearchConnectionIT` via Testcontainers ES 9.4.8 | all pass, both clients contact ES | 4/4 pass; asserted version `9.4.8`, cluster name, status, nodes | PASS |
| cURL requests (4 endpoints) | see "Verified cURL requests" | HTTP 200 + expected body | all 200, correct real data from live ES | PASS |
| Field assertions | `jq -e` on each response | client/version/uuid/status fields valid | all 4 assertions PASS | PASS |
| Failure path | `docker compose stop elasticsearch` then cURL | HTTP 503, no leaked details | both endpoints 503 with generic ProblemDetail | PASS |
| README verification | executed up / readiness / verify / curl / down commands | works as documented | all documented commands executed successfully | PASS |

## Verified cURL requests

Real running app (`http://127.0.0.1:8080`) against live Elasticsearch 9.4.8 (`docker compose -p ch01`).

**1) `GET /api/spring-data/elasticsearch/health`**
- Expected: 200; `client=spring-data`, `reachable=true`, `status ∈ {green,yellow,red}`, `numberOfNodes>=1`
- Actual: `200` — `{"client":"spring-data","clusterName":"docker-cluster","status":"green","numberOfNodes":1,"reachable":true}`
- Assertion: PASS

**2) `GET /api/spring-data/elasticsearch/cluster-info`**
- Expected: 200; `client=spring-data`, `versionNumber=9.4.8`, non-empty `clusterUuid`, `tagline="You Know, for Search"`
- Actual: `200` — `{"client":"spring-data","clusterName":"docker-cluster","clusterUuid":"R_9rUZDrSbaYpbzawr5wqQ","versionNumber":"9.4.8","tagline":"You Know, for Search"}`
- Assertion: PASS

**3) `GET /api/java-client/elasticsearch/health`**
- Expected: 200; `client=java-client`, `reachable=true`, valid `status`
- Actual: `200` — `{"client":"java-client","clusterName":"docker-cluster","status":"green","numberOfNodes":1,"reachable":true}`
- Assertion: PASS

**4) `GET /api/java-client/elasticsearch/cluster-info`**
- Expected: 200; `client=java-client`, `versionNumber=9.4.8`, non-empty `clusterUuid`
- Actual: `200` — `{"client":"java-client","clusterName":"docker-cluster","clusterUuid":"R_9rUZDrSbaYpbzawr5wqQ","versionNumber":"9.4.8","tagline":"You Know, for Search"}`
- Assertion: PASS

> Both clients returned the **same `clusterUuid`** (`R_9rUZDrSbaYpbzawr5wqQ`), confirming they contacted the same live cluster rather than returning constant/mocked values.

**Failure path (Elasticsearch stopped):**
- Command: `docker compose -p ch01 stop elasticsearch` then cURL both health endpoints
- Expected: HTTP 503, generic message, no connection details/credentials
- Actual: both returned `503` — `{"detail":"Could not complete the request against Elasticsearch. Check that the cluster is running and reachable.","instance":"/api/.../health","status":503,"title":"Elasticsearch request failed"}`
- Assertion: PASS (proves endpoints genuinely contact ES; no leaked details)

## Test execution evidence (counts)
- Surefire (unit/controller): `Tests run: 2` (JavaClientElasticsearchControllerTest) + `Tests run: 3` (SpringDataElasticsearchControllerTest) = 5, 0 failures.
- Failsafe (integration): `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` in `ElasticsearchConnectionIT` (Testcontainers started `elasticsearch:9.4.8`, container up in ~14.6s).
- Final line: `BUILD SUCCESS`.

## Dynamic vs deterministic values
- Deterministic (asserted on exact value): HTTP status, `client`, `versionNumber=9.4.8` (pinned image), `tagline`.
- Dynamic (asserted with tolerant checks): `clusterUuid` (non-empty), `status` (one of green/yellow/red), `clusterName` (`docker-cluster` default for this image, configurable via `cluster.name`).

## Failures, limitations, cleanup
- No test failures. No gates BLOCKED or NOT VERIFIED.
- Known environment note: the machine's default `java`/`mvn` resolve to Java 17 (x86_64); JDK 21 must be selected via `JAVA_HOME`. All verification used JDK 21 explicitly. This does not affect the project, which pins `java.version=21`.
- Cleanup performed: application process stopped; `docker compose -p ch01 down` removed container and network. The compose file defines no persistent volume, so no data was deleted.
- Mockito self-attach warning appeared (JDK agent dynamic-load deprecation); informational only, tests passed.

## Overall status
**COMPLETED** — every mandatory gate passed with evidence from real execution (real Maven build, real Dockerized Elasticsearch 9.4.8, real application startup, real cURL with field assertions, and a demonstrated failure path).
