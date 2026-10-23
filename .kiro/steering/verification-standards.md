---
inclusion: always
---
# Mandatory Runtime Verification and Evidence Standard

## Definition of done
A chapter is **COMPLETED only if every applicable mandatory gate has PASS with evidence from real execution**. `FAIL`, `BLOCKED`, or `NOT VERIFIED` are never counted as PASS. Any intentionally non-applicable check must be explained, not silently skipped. For this learning repository, real local Elasticsearch integration and cURL checks are mandatory for **every** chapter.

## Verification sequence (execute, do not merely recommend)
1. **Environment**: record `java -version`, `mvn -version`, `docker version`, selected dependency/server versions; verify tools exist.
2. **Build and tests**: run `mvn clean verify` **inside chapter directory**, with unit/integration tests configured so required ones really execute. Inspect test reports and exit codes; no `-DskipTests` or `-DskipITs`. If some ITs need running Docker, set that up before final verify or rerun verify after readiness.
3. **Docker**: run `docker compose -f docker-compose.yml up -d` from the chapter directory. Inspect `docker compose ps`, health/readiness, and logs as necessary. Use local address and matching HTTP/TLS/auth. Confirm server is ready, not just container Running.
4. **Spring Boot**: launch application using the chapter's documented command; verify startup and listening address. Track process and stop safely after test.
5. **Real cURL**: invoke **each implemented chapter endpoint** against the real running app at least once on a success path and representative meaningful failure paths. No mocks for integration proof. Include `curl --fail-with-body` or explicit HTTP status capture, and demonstrate response-field assertions (e.g. `jq -e`, a script, or integration assertions). A 200 response alone does not prove Elasticsearch was contacted.
6. **Expected vs actual**: assert deterministic values (status, returned fields, actual server/cluster name from config), tolerate dynamic cluster metadata with appropriate checks. Check HTTP status and response body. Record failure messages and retests.
7. **Documentation**: execute README setup/cURL/teardown commands or clearly identify commands not rerun; ensure port, paths, response examples and configuration reflect reality. Validate clean stop `docker compose down` without deleting persistent data by default.
8. **Regression**: rerun impacted tests after code changes. Repeat the end-to-end validation after the final fix.

## Mandatory evidence file
Write `<chapter>/VERIFICATION.md` after attempted validation, with sections:

```markdown
# Chapter XX Verification Report
- Timestamp (with timezone):
- Git commit/branch (if available):
- Java / Maven / Spring Boot / Elasticsearch / Java API Client versions:
- Environment and preconditions:

| Gate | Command / evidence location | Expected | Actual | Status |
|---|---|---|---|---|
| Maven build + unit tests | ... | BUILD SUCCESS | ... | PASS/FAIL/BLOCKED/NOT VERIFIED |
| Docker startup + readiness | ... | ready | ... | ... |
| Application startup | ... | listening | ... | ... |
| Integration tests | ... | all pass | ... | ... |
| cURL requests | ... | expected status/body | ... | ... |
| Field assertions | ... | expected fields | ... | ... |
| README verification | ... | works as documented | ... | ... |

## Verified cURL requests
- Command:
- Expected status and JSON fields:
- Actual status and JSON fields:
- Assertion result:

## Failures, limitations, cleanup
...

## Overall status
COMPLETED / FAILED / BLOCKED / NOT VERIFIED
```

Include trustworthy excerpts of actual output and exact commands; redact secrets and tokens. Never paste fabricated terminal output. Report any skipped tests clearly.

## Error/blocked policy
If tools/services are unavailable: clearly list what's missing and actions a human must take, mark corresponding gates `BLOCKED` or `NOT VERIFIED`, and overall status **not** `COMPLETED`. Do not make unsupported promises about future validation.
