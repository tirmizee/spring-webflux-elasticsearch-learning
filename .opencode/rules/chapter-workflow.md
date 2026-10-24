# Required Chapter Lifecycle and Approval Gates

Apply this for Chapter 01 and **every** later chapter, including new Kiro sessions.

## Gate 0 — Inspect before touching files
1. Read root README, roadmap, current `.opencode/rules/*.md`, current files, and `git status --short` if git exists.
2. Identify requested chapter and confirm explicit user authorization for **that chapter**. Do not interpret request for a plan/README as permission to implement future chapters.
3. For Chapter 02+, inspect previous chapter's README and `VERIFICATION.md`; check that it is marked `COMPLETED` with actual recorded evidence, not just an assertion. Recheck material prerequisites relevant to the new chapter (Java, Maven, Docker, existing bindings/ports, versions). A prior verification from an older session is evidence of that past run, not evidence current services are running.
4. If the previous chapter has `FAIL`, `BLOCKED`, `NOT VERIFIED`, missing evidence or incompatible versions, disclose the issue and ask user approval to proceed or repair. Do **not** silently change prior chapters.
5. Check current branch and preserve uncommitted or unfamiliar files.

## Gate 1 — Plan
- State exact scope, project name, versions/compatibility, endpoints/API contracts and expected assertions, class layout, Docker/test approach, and command-level verification plan.
- Identify external blockers early. Continue only within chapter scope and approved parameters.

## Gate 2 — Implement
- Write minimum complete working code, Docker Compose, tests and Thai chapter README.
- Two clients must be distinctly demonstrated when required (mandatory for Chapter 01).
- Respect other chapters and avoid cross-chapter code dependencies.

## Gate 3 — Verify
Follow **all** steps in `verification-standards.md`. Run commands with tools and capture observable outputs. Do not mark success based solely on source inspection or imagined output. If an environment prevents verification, mark `BLOCKED` / `NOT VERIFIED` rather than `COMPLETED`.

## Gate 4 — Report, stop and wait
- Write `<chapter>/VERIFICATION.md` with real command/results and timestamp (include timezone).
- Summarize features, actual verification status, any blockers and precise next chapter.
- Stop. Never start the next chapter until user explicitly approves it.
- Do not claim an approval the user did not grant.

## Recovery policy
Attempt bounded diagnostic fixes for the current chapter, rerun affected checks, and log remaining failures. Never skip test execution, water down assertions, invent logs, or silently modify code in earlier chapters. If a requested action needs privilege, external credential, destructive cleanup, or modifying existing unrelated work, ask first.
