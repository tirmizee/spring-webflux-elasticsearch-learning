---
inclusion: always
---
# Technology and Engineering Rules

## Pinned platform families
- Java **21**; Spring Boot **4.x**; Spring WebFlux; Maven; Elasticsearch.
- Spring Data Elasticsearch including `ReactiveElasticsearchOperations`/reactive client, and **official Elasticsearch Java API Client** (`co.elastic.clients`) used in distinct examples.
- Tests: JUnit Jupiter, Mockito, Reactor Test (`StepVerifier`), Testcontainers for integration scenarios as appropriate.

## Version-selection gate
Before writing code, inspect machine/runtime and existing project files. Determine stable, compatible, documented **concrete versions** of Spring Boot 4.x, Spring Data Elasticsearch, official Java client, and Elasticsearch server; verify API methods and Maven coordinates against available official metadata/docs. Prefer Boot's BOM and managed versions. Don't invent versions, features or copied Boot 3 configuration. Spring Boot 4 client wiring can differ (e.g. REST5 infrastructure); use supported auto-configurations or correct explicit beans. If verification is impossible, document uncertainty rather than asserting compatibility. Record exact selected versions in README and `VERIFICATION.md`.

## WebFlux semantics
- Return `Mono`/`Flux` where relevant. Never `.block()` on a Netty/reactive request thread.
- `ElasticsearchClient` synchronous methods perform blocking work: isolate via an appropriate bounded elastic scheduler / adapter, or use a documented async option. Explain limits and cancellation; wrapping blocking work in `Mono.just(...)` does NOT fix blocking.
- Keep reactive Spring Data approach clearly separate from direct official client approach in package/class names, endpoint paths and README.
- Choose sensible timeouts and errors for demonstrations; error responses must not leak credentials.

## Code conventions
- Java 21, constructor injection, configuration externalized, descriptive class names, restrained package layers (controller/service/config), no overengineering or premature `common` module.
- No hard-coded API keys, passwords, credentials; use environment variables and `.env.example` if useful, ensure `.env` ignored.
- No secret logging; no unauthenticated production deployment recommendation.
- Meaningful tests rather than trivial coverage; never disable tests or swallow failures.
- Use commands confirmed compatible with the local shell and environment.

## Java Imports & Code Readability
Applies to all current and future learning chapters.
- Prefer standard Java `import` statements over fully qualified class names inside method bodies.
- Avoid unnecessary fully qualified references such as `reactor.core.publisher.Mono.just(...)` when `Mono.just(...)` is clear with an import.
- Use explicit imports for commonly used classes; follow standard Java import organization and remove unused imports.
- Fully qualified class names are acceptable when needed to resolve a naming conflict or genuinely improve clarity.
- Do not introduce wildcard imports merely to shorten code.
- Do not change otherwise-correct application logic solely for stylistic consistency; this is a readability convention, not a mandate to refactor working code.

## Client compatibility
- Verify Spring Data Elasticsearch supports the chosen Elasticsearch server release; do not select the Elasticsearch Docker image by latest-tag guessing.
- Match local/no-auth Chapter 01's actual HTTP/TLS settings. API key Chapter 02 must explicitly explain HTTPS, secure key handling, creation/privileges and client configuration.
