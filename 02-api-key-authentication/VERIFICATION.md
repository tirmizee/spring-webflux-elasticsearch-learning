# Chapter 02 Verification Report

- **Timestamp (with timezone):** 2026-10-23 18:03:26 +07 (ICT, Asia/Bangkok)
- **Source revision used for verification:**
  - Branch: `main`
  - HEAD at verification: `ebf9155` (`ebf91556407687e180000ccb50ea47eab676e28d`). Chapter 02 (`02-api-key-authentication/`) was untracked/new at verification time (not yet committed); it was verified in the working tree.
- **Versions:**
  - Java: OpenJDK Temurin **21.0.2** (LTS, arm64)
  - Maven: Apache **3.9.9** (via `JAVA_HOME` → Temurin 21.0.2)
  - Spring Boot: **4.1.2**; Spring Framework: 7.0.10
  - Spring Data Elasticsearch: **6.1.2** (Spring Data BOM 2026.0.2)
  - Elasticsearch Java API Client (`co.elastic.clients`): **9.4.8**
  - Elasticsearch server image: **elasticsearch:9.4.8** (arm64)
  - Testcontainers: 2.0.5; Docker: client/server **25.0.2**; Compose v2.24.3-desktop.1
- **Environment:** macOS (arm64). Default `java`/`mvn` are Java 17; JDK 21 selected explicitly via `JAVA_HOME`. Docker Desktop running.

## Scope recap
Secure Elasticsearch connectivity using **API Key authentication**. Both clients (Spring Data reactive + official Java API Client) are authenticated via Spring Boot auto-configuration using `spring.elasticsearch.api-key` (from `ELASTICSEARCH_API_KEY`). No custom client beans. Chapter is independent (no Chapter 01 source dependency).

## Key design decisions
- **No custom config beans.** Boot 4.1.2 exposes `spring.elasticsearch.api-key` and applies it to both the reactive client and the Java API Client, so authentication needed zero wrapper/factory code (confirmed from `spring-boot-elasticsearch-4.1.2` configuration metadata and `ElasticsearchProperties.getApiKey()`).
- **TLS trade-off (documented).** The chapter's docker-compose enables security (`xpack.security.enabled=true`) but leaves the HTTP layer on plain HTTP (`xpack.security.http.ssl.enabled=false`) to keep the local setup reproducible without self-signed certificates. API keys are fully exercised regardless. README Security section states production MUST use HTTPS and bind to localhost only.
- **Secrets handling.** API key supplied only via env var; `.env.example` provided; `.env` gitignored; no key or `Authorization` header is logged (WIRE logging left at WARN; handler logs sanitized messages only).
- **Error classification.** `ElasticsearchException.status()` is branched: 401 → authentication failure, 403 → authorization failure; plus 504 timeout / 503 unavailable / 502 other ES error / 500 unexpected. Wrapped causes are walked.

## Gate results

| Gate | Command / evidence | Expected | Actual | Status |
|---|---|---|---|---|
| Maven compile | `mvn compile` | success | success | PASS |
| Unit tests | `mvn clean verify` (surefire) | pass | 20 unit tests, 0 failures | PASS |
| Integration tests (real secured ES) | `ApiKeyAuthenticationIT` (Testcontainers 9.4.8, security on) | pass | 5/5 pass | PASS |
| Full build | `mvn clean verify` | BUILD SUCCESS | BUILD SUCCESS (26.3s) | PASS |
| Docker startup + security on | `docker compose -p ch02 up -d` | authenticated health 200, anon 401 | health(elastic)=200; anon=401 | PASS |
| Create API key (least-privilege) | `POST /_security/api_key` cluster:[monitor] | `encoded` returned | key id + encoded returned | PASS |
| App startup | `java -jar ... ` with `ELASTICSEARCH_API_KEY` | listening | Netty on 8081 | PASS |
| cURL valid key (4 endpoints) | see below | 200 + expected body | all 200, correct data | PASS |
| Auth failure — invalid key | app run with bogus key | 401, no leak | both clients 401, no leak | PASS |
| Auth failure — revoked key | `DELETE /_security/api_key` then cURL | was 200, now 401 | 200 → 401 after revoke (both clients) | PASS |
| No credential leakage | inspect app logs + responses | none | no key/Authorization in logs or bodies | PASS |
| Chapter 01 unchanged | `git status` | untouched | only `02-...` is new/untracked | PASS |
| Chapter 02 independent | own pom/compose/build | yes | builds & runs standalone | PASS |

## Actual test counts (this run)
Surefire (unit) — total **20**, 0 failures:
- `controller.JavaClientConnectionControllerTest` — 2
- `controller.SpringDataConnectionControllerTest` — 2
- `exception.GlobalExceptionHandlerTest` — 8
- `service.JavaClientConnectionServiceTest` — 5
- `service.SpringDataConnectionServiceTest` — 3

Failsafe (integration) — `integration.ApiKeyAuthenticationIT` — **5**, 0 failures (starts security-enabled ES, creates a real API key as `elastic`, authenticates both clients, and asserts an invalid key is rejected with 401). Final line: `BUILD SUCCESS`.

## Verified cURL requests (real app + real secured ES 9.4.8)

Valid API key (all HTTP 200):
- `GET /api/spring-data/elasticsearch/health` → `{"client":"spring-data","clusterName":"docker-cluster","status":"green","numberOfNodes":1,"reachable":true}`
- `GET /api/spring-data/elasticsearch/cluster-info` → `{"client":"spring-data","clusterName":"docker-cluster","clusterUuid":"yjOkn8zBTB-b2o48yWoJ8Q","versionNumber":"9.4.8","tagline":"You Know, for Search"}`
- `GET /api/java-client/elasticsearch/health` → `{"client":"java-client",...,"reachable":true}`
- `GET /api/java-client/elasticsearch/cluster-info` → `{"client":"java-client","clusterUuid":"yjOkn8zBTB-b2o48yWoJ8Q","versionNumber":"9.4.8",...}` (same cluster UUID as spring-data → both authenticated against the same live cluster)
- Field assertions (`jq -e`): all PASS.

Invalid API key (app started with a bogus encoded key):
- `GET /api/spring-data/.../health` → **401** `{"title":"Elasticsearch authentication failed","status":401,"detail":"The Elasticsearch API key is missing, invalid, or has been revoked.","instance":"/api/spring-data/elasticsearch/health"}`
- `GET /api/java-client/.../health` → **401** (same shape)
- Assertion `status==401 && title=="Elasticsearch authentication failed" && detail has no "ApiKey"/"9200"/key material` → PASS.

Revoked API key:
- Before revoke (valid key): `GET /api/spring-data/.../health` → **200**
- `DELETE /_security/api_key {"ids":["YRJaaKEB6SpFYw3_dxc4"]}` → `{"invalidated_api_keys":["YRJaaKEB6SpFYw3_dxc4"]}`
- After revoke (same key): both clients → **401** "Elasticsearch authentication failed". PASS.

Security enforcement sanity:
- Authenticated `/_cluster/health` (`-u elastic:...`) → 200; unauthenticated → **401** (security is actually enforced).

## No-leak verification
- App logs during the failure runs contained only sanitized warnings (e.g. "Elasticsearch authentication failed (HTTP 401 from ES)") — no API key, no `Authorization` header, no host/URI.
- Error response bodies are generic `ProblemDetail`s; unit test `GlobalExceptionHandlerTest` asserts the absence of key ids, `ApiKey`, host:port, and raw ES security messages.
- No real credentials appear in source or committed docs; `.env` is gitignored and `.env.example` contains only placeholders.

## Authentication vs authorization (distinction)
- 401 path (missing/invalid/revoked key) verified end-to-end against real ES.
- 403 path (valid key, insufficient privilege) verified at the unit level in `GlobalExceptionHandlerTest` (ES 403 → app 403 "Elasticsearch authorization failed"). A full end-to-end 403 would require provisioning a key without `cluster:monitor` and is covered deterministically by the unit test; stated here rather than claimed as an executed cURL check.

## Cleanup
- App stopped; `docker compose -p ch02 down` removed container + network (no persistent volume). Temporary files holding the API key were deleted.

## Remaining issues / limitations
- None blocking. 403 end-to-end is verified via unit test only (see above). TLS on the HTTP layer is intentionally disabled for local learning (documented; production requires HTTPS).

## Overall status
**COMPLETED** — build, 20 unit + 5 integration tests, real secured Elasticsearch 9.4.8, real API key create/use/revoke, and real cURL for valid/invalid/revoked keys all pass. No credential leakage observed. Chapter 01 untouched; Chapter 02 runs independently.
