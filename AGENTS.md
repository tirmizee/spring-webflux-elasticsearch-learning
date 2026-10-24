# OpenCode — Spring WebFlux + Elasticsearch Learning

Read and follow ALL rule documents configured in `opencode.json` before doing any work. The rules apply across sessions and chapters.

- Work only on the chapter explicitly authorized by the user; do not proceed automatically to the next chapter.
- Preserve user modifications; inspect `git status --short` and current files before editing. Do not commit or push without explicit authorization.
- Maintain independent Maven projects, Java 21, Spring Boot 4.x, WebFlux, Elasticsearch, Thai explanations, and meaningful unit/integration tests.
- Do not call `.block()` on reactor-netty event loop. For synchronous Elasticsearch calls, offload blocking work with `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`.
- Verify builds, tests, Docker readiness, running APIs, and actual cURL responses before marking COMPLETED. Never invent results or evidence; use BLOCKED / NOT VERIFIED when needed.
- Follow inspect → plan → implement → verify → report → STOP approval gates.
- Follow all six `.opencode/rules/*.md` documents as authoritative detailed requirements.

The original `.kiro/` directory is intentionally preserved for Kiro compatibility.
