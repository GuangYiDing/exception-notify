# Repository Guidelines

## Project Structure & Module Organization
Backend source lives in `src/main/java/com/nolimit35/springkit/**`, grouped by concern (`notification`, `service`, `config`, etc.), with tests mirroring the same packages under `src/test/java`. Keep shared templates, YAML samples, and Spring metadata in `src/main/resources`; register new auto-configurations through `META-INF/spring.factories` and document property defaults in `application-example.yaml`. The `docs/` directory hosts supplemental reference material, while the `web/` workspace contains the Next.js console that consumes this starter—treat it as an isolated app with its own lockfile.

## Build, Test & Development Commands
- `mvn clean install`: Full backend build, test suite, and artifact packaging.
- `mvn test`: Fast feedback loop for Java unit and integration suites.
- `mvn -DskipTests package`: Assemble jars when tests were run elsewhere.
- `npm run dev` (inside `web/`): Launch the documentation UI on a local dev server.
- `npm run lint` (inside `web/`): Apply the Next.js ESLint rules before shipping UI tweaks.

## Coding Style & Naming Conventions
Java code uses four-space indentation, UpperCamelCase classes, and lowercase package names aligned to `com.nolimit35.springkit`. Prefer Lombok annotations already in place (`@Slf4j`, `@Data`, `@Builder`) to avoid manual boilerplate. TypeScript in `web/` follows the Next.js default ESLint configuration; keep modules typed, colocate component styles under the same folder, and name React components with PascalCase.

## Testing Guidelines
Write backend tests with JUnit 5 and Mockito, naming files with the `*Test` suffix and keeping fixtures under `src/test/resources`. Focus coverage on notification routing, property binding, and trace enrichment; use deterministic unit tests unless autoconfiguration behavior demands a Spring context. The UI currently has no mandatory harness—when adding significant front-end logic, scaffold Playwright or Vitest checks in `web/` and document a manual validation recipe in the PR.

## Commit & Pull Request Guidelines
Use conventional commits such as `feat(notification): add slack provider` or `fix(config): guard empty tokens`, keeping the scope aligned with the affected module. Pull requests should summarize the change, list validation commands (`mvn test`, `npm run lint`, manual UI steps), link relevant issues, and call out follow-up work. Wait for CI to pass before requesting review and capture any configuration updates in `application-example.yaml`.

## Configuration & Integration Tips
Expose new provider settings via `ExceptionNotifyProperties`, supply sane defaults, and surface env-variable mappings in the docs. Never hardcode secrets—prefer `@ConfigurationProperties` binding or environment placeholders. For outbound HTTP integrations, place clients under `service` subpackages, wrap them with clear retry/logging policies, and add example wiring to the sample YAML files.
