---
inclusion: always
---
# Product: Spring WebFlux + Elasticsearch Learning

## Mission
Build `spring-webflux-elasticsearch-learning`: a hands-on, progressive educational monorepo of **independent** Spring Boot Maven applications. Each chapter teaches exactly one core topic with working code, real Elasticsearch integration, tests, runnable cURL examples, and a thorough Thai README (keep technical terms, identifiers and code in English).

## Non-negotiable outcomes
- One focused, runnable project per chapter; no dependency on earlier chapter source code.
- Implement **only the explicitly requested chapter**. Initially implement Chapter 01 only.
- Require **actual** build, Docker, application, tests, and real cURL verification before `COMPLETED`.
- Preserve existing user files and git work; do not silently overwrite, reset, delete or refactor unrelated content.
- Stop after reporting a chapter; never move to the next chapter without explicit user approval.

## Roadmap (fixed order unless user changes it)
| # | Directory | Goal |
|---|---|---|
| 01 | `01-basic-connection` | Local unauthenticated connectivity, two clients, cluster information |
| 02 | `02-api-key-authentication` | API key security and client setup |
| 03 | `03-document-crud` | Index/get/update/delete |
| 04 | `04-search-queries` | Term, match, bool queries |
| 05 | `05-custom-analyzers` | Standard, Thai, ngram analysis |
| 06 | `06-sorting-pagination` | Sorting, pagination, search_after |
| 07 | `07-fuzzy-search` | Fuzzy, prefix and partial search |
| 08 | `08-index-management` | Index/mappings/settings/templates |
| 09 | `09-bulk-operations` | Batch and bulk indexing |
| 10 | `10-reactive-error-handling` | Timeout, retry, error mapping |
| 11 | `11-index-recreation` | Recreate, resync, safe lifecycle |
| 12 | `12-integration-testing` | Testcontainers and integration strategies |

## Learning design
- Practical explanations: what, why, how, execution path and pitfalls.
- Compare Spring Data Elasticsearch and official Elasticsearch Java API Client where relevant; avoid pointless duplicate implementations if the chapter is not about client comparison. In Chapter 01, both implementations are mandatory.
- Never pretend synchronous Java API Client calls are non-blocking. Explicitly explain execution models.
- Root README gives roadmap, prerequisites, chapters status and instructions. Chapter README follows `structure.md`.
