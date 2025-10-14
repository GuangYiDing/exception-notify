# Repository Guidelines

## Project Structure & Module Organization
This repository ships a single Spring Boot starter focused on exception notifications. Use `src/main/java/com/nolimit35/springkit/**` for production code, grouped by concern (e.g., `notification`, `service`, `config`). Shared configuration lives in `src/main/resources`, including `META-INF/spring.factories` for auto-registration and `application-example.yaml` for property samples. Keep any new sample configs or assets under `src/main/resources` and mirror Java package names when adding test fixtures in `src/test/resources`.

## Build, Test & Development Commands
- `mvn clean install`: Compiles the starter, runs the test suite, and assembles publishable artifacts.
- `mvn test`: Executes unit and integration tests only; use during iterative development.
- `mvn -DskipTests package`: Builds jars quickly when tests are already covered elsewhere.
Run Maven commands from the repository root with JDK 8+.

## Coding Style & Naming Conventions
Follow standard Java conventions with 4-space indentation and upper camel case for classes. Package structures should stay lowercase and align with `com.nolimit35.springkit`. Prefer Lombok annotations already in use (`@Slf4j`, `@Data`) instead of manual boilerplate. Format code via your IDE before committing; ensure imports are organized and unused imports removed.

## Testing Guidelines
Tests should mirror production packages under `src/test/java` and use JUnit 5 with Mockito for mocking. Name classes with the `*Test` suffix and focus on verifying notification flows, trace enrichment, and service integrations. Provide configuration stubs in `src/test/resources/application-test.yml` or module-specific YAML files. When adding new functionality, prefer deterministic unit tests over full Spring contexts unless behavior depends on autoconfiguration. Aim to maintain or improve existing coverage by exercising new branches.

## Commit & Pull Request Guidelines
Adopt the conventional commit style observed in history: `<type>(<scope>): <description>`, where `type` includes `feat`, `fix`, or `docs`, and `scope` matches module folders (e.g., `notification`, `git`). Keep messages in English when possible and include short present-tense summaries. Pull requests should describe the change, outline validation steps (commands run, screenshots of notifications if relevant), and link any tracked issues. Request review when CI passes and note any follow-up work explicitly.

## Configuration & Integration Tips
When wiring new notification providers or integrations, expose properties through `ExceptionNotifyProperties` and document defaults in `application-example.yaml`. Ensure new autoconfiguration classes are listed in `META-INF/spring.factories` to activate via Spring Boot. For extensions that call external APIs, encapsulate HTTP logic under `service` or provider-specific packages and guard secrets via environment variables rather than hardcoding values.
