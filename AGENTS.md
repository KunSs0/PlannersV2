# Repository Guidelines

## Project Structure & Module Organization
Planners core code lives in `src/main/kotlin/com/gitee/planners`, with legacy helpers in `src/main/java`. Bukkit configuration, UI layouts, and scripted skills live under `src/main/resources`, grouped by feature (e.g., `skill/`, `job/`, `ui/`). Test scaffolding mirrors this layout in `src/test/(kotlin|java)`; keep new fixtures there. Build output lands in `build/`, while `build-jar/` and the configured `F:\minecraft\server\paper-1.20.1\plugins` directory receive assembled jars for server hot-loading. External jars stored in `libs/` are loaded via `compileOnly(fileTree("libs"))`.

## Build, Test, and Development Commands
- `./gradlew clean build`: compiles Kotlin and Java sources, runs checks, and produces the plugin jar under `build/libs`.
- `./gradlew test`: executes unit tests; add dependencies before relying on it for CI.
- `./gradlew jar`: rebuilds the plugin and copies it to the Paper server path configured in `build.gradle.kts`.
- `./gradlew dependencies`: inspects merged TabooLib, Bukkit, and custom dependencies when troubleshooting classpath issues.

## Coding Style & Naming Conventions
Target JVM 1.8 with Kotlin 1.8.22; keep code `UTF-8` encoded as enforced by the Gradle script. Use four-space indentation, `UpperCamelCase` for classes, and `lowerCamelCase` for functions and properties. Group public APIs under stable packages (`com.gitee.planners.api`) and keep implementation details within `core` or `util`. YAML resources should follow existing lowercase, hyphenated naming (e.g., `router-select.yml`).

## Testing Guidelines
Prefer fast Kotlin unit tests in `src/test/kotlin`; mirror package paths from `main`. Use descriptive method names such as `shouldComputePathWeight()` and annotate with JUnit 5 once the dependency is added. For data-driven logic, add fixture YAML under `src/test/resources` and load via TabooLib's resource helpers. Document expected behavior in tests when reproducing in-game scenarios.

## Commit & Pull Request Guidelines
Recent history favors short, imperative summaries ("fix", "update"); expand on context in the body when needed. Reference issue IDs or Discord threads in the body when relevant. Pull requests should describe gameplay impact, include reproduction steps, and attach screenshots or logs for UI or routing changes. Ensure CI `build` passes before requesting review and call out any manual post-merge steps (e.g., server config tweaks).

## Server Deployment Tips
Verify the `tasks.withType<Jar>.destinationDirectory` path before shipping; adjust to match your local Paper instance. After copying, reload the server with `/reload confirm` only for quick validation; prefer full restarts for production servers. Keep sensitive tokens out of `config.yml`; store environment-specific overrides outside the repo.
