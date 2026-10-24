# Java Code Standards

General Java coding conventions for all current and future learning chapters. These are readability
and maintainability standards — never change program behavior merely to satisfy them, and do not
reformat unrelated code solely for style. (Technology/runtime/security rules live in `tech.md`;
project/package architecture and README structure live in `structure.md`.)

## Java Coding Conventions
- Target **Java 21**; use idiomatic modern Java (records, `var` where it aids clarity, switch
  expressions, etc.) when it genuinely improves readability.
- Use **descriptive** class, method, and variable names; follow standard Java naming conventions.
- Prefer readable, maintainable code over clever or terse code.
- Use **constructor injection** with `final` dependencies (no field injection).
- Preserve an explicit constructor when it contains custom initialization logic.
- Avoid unnecessary abstractions and boilerplate: no single-implementation interfaces "just in
  case", no premature factories/patterns, no premature `common` module, no overengineering.

## Java Imports
- Prefer standard `import` statements over fully qualified class names inside method bodies
  (e.g. use `Mono.just(...)` with an import, not `reactor.core.publisher.Mono.just(...)`).
- Avoid wildcard imports.
- Remove unused imports; follow standard Java import organization.
- Fully qualified class names are acceptable when needed to resolve a genuine naming conflict or to
  genuinely improve clarity.

## Lombok Guidelines
Lombok is **optional, not mandatory** — a tool to remove genuine boilerplate, never added for uniformity.
- Prefer **Java records** for immutable DTOs; do not replace a record with a Lombok class.
- Use Lombok **only where it meaningfully reduces boilerplate** (e.g. an entity with many
  getters/setters/constructors). Having zero Lombok in a chapter is a valid outcome.
- Prefer **targeted annotations** (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`,
  `@RequiredArgsConstructor`, `@Slf4j`) over `@Data`.
- `@RequiredArgsConstructor` is appropriate for straightforward constructor injection with `final`
  fields; **preserve an explicit constructor when it contains custom initialization logic**.
- `@Slf4j` is appropriate for ordinary SLF4J logger declarations; do not change log levels, messages,
  or error handling.
- Use `@Getter`/`@Setter` only for properties that genuinely need them; keep DTOs immutable where possible.
- Avoid `@Builder` without a real need, avoid `@SneakyThrows` (do not hide checked exceptions), and do
  not introduce mutable state unnecessarily.
- Maintain compatibility with Spring Boot, Java 21, serialization frameworks (Jackson), and Spring Data
  Elasticsearch mappings; keep mapping annotations (`@Document`, `@Id`, `@Field`) and `_id` handling intact.
- When introducing Lombok to a chapter, add the `org.projectlombok:lombok` dependency **only to that
  chapter** (optional/compile-time scope), use the **Spring Boot BOM-managed version** (do not hardcode),
  and verify a clean `mvn clean verify`.
- Do not refactor previously completed chapters to add Lombok without explicit user approval.

## Multi-line Argument Formatting
Readability convention only — never change behavior to satisfy it.
- Short, clear, readable invocations may stay on **one line**.
- When a constructor call or method invocation spans multiple lines, put **one argument per line**;
  do not place several arguments on the same line within a multi-line invocation.
- Put the **closing parenthesis on its own line**, aligned with the start of the invocation statement.
- Keep argument ordering unchanged.
- Apply equally to constructor invocations, method invocations, factory method calls, and record instantiation.
- Do not reformat unrelated code solely to enforce this; keep formatting changes minimal and focused.

Preferred:
```java
return new ProductHit(
        id,
        p.getName(),
        p.getCategory(),
        p.getBrand(),
        p.getDescription(),
        p.getPrice(),
        p.getQuantity(),
        score
);
```
Avoid:
```java
return new ProductHit(
        id, p.getName(), p.getCategory(), p.getBrand(),
        p.getDescription(), p.getPrice(), p.getQuantity(), score);
```
