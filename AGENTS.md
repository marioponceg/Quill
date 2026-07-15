# AGENTS.md

Guidance for AI agents (and humans) working in this repository. This is the source of truth
for project conventions; `CLAUDE.md` simply points here.

## Project

Quill is a structured logging library for Android and the JVM, built as an open-source
**portfolio piece demonstrating senior-level engineering practices** — API design, modern
Kotlin usage, and testing discipline. Third library of the `io.github.marioponceg` family,
alongside Conduit (networking) and Foundry (Compose design system).

Full design: `docs/superpowers/specs/2026-07-13-quill-structured-logging-design.md`.

## Settled design decisions — do not re-litigate, implement as stated

- **Events, not sentences**: each log is a `QuillEvent` (level, origin, event name, typed
  `QuillValue` fields, optional throwable). Structure survives to the sinks; formatting is
  a sink concern, never a call-site concern.
- **`quill-core` is pure Kotlin/JVM** with no dependency beyond the stdlib. No
  kotlinx.serialization in core: the JSON pretty-printer is hand-written; data classes are
  beautified by parsing their `toString()`. Both degrade to raw output, never throw.
- **Levels are Android's five** (`Verbose, Debug, Info, Warn, Error`), nothing invented.
- **Field builder lambdas are not evaluated** when the level is filtered out (zero-cost
  disabled logs). Logging before `quill { }` configuration is a silent no-op.
- **Sinks** implement `fun write(event: QuillEvent)` — synchronous, non-suspend; sinks
  needing I/O dispatch internally. Core dispatches sequentially with per-sink try/catch.
- **LogcatSink** renders boxed Unicode output, tag = `tagPrefix + "." + origin` with
  `tagPrefix = "Quill"` by default (`null` disables), chunks at 4000 chars preserving the
  box, formatting isolated in a JVM-testable `LogcatFormatter`.
- **Conduit never depends on Quill**: `quill-conduit` is the only module that knows both
  libraries (interceptor emitting `http_request` / `http_response` / `http_failure`
  events, `BodyLevel` verbosity, `Authorization` redacted by default, origin `Http`).
- **Kotlin explicit API mode** on all library modules; JVM toolchain 21; Android modules
  target Java 17, minSdk 26, compileSdk/targetSdk 37.
- **AGP 9 built-in Kotlin**: never apply `org.jetbrains.kotlin.android`.

Any design decision **not** listed above must be raised with the maintainer before
implementing.

## Module structure

- `quill-core` — pure Kotlin/JVM: event model, logger, `quill { }` DSL, beautifier.
- `quill-android` — Android library: `LogcatSink` (`api`-exposes core).
- `quill-conduit` — Kotlin/JVM: `QuillInterceptor` bridging Conduit (arrives in PR #4).
- `demo` — Android showcase app, never published.
- Wire each new module into `settings.gradle.kts` and register shared plugin versions in
  the root `build.gradle.kts` (`apply false`).

## Conventions

- One design unit per PR. PR titles follow Conventional Commits (squash-merge makes them
  the commits on `main`). PR bodies follow `.github/PULL_REQUEST_TEMPLATE.md`.
- TDD: failing test first, minimal implementation, frequent commits.
- Dependency versions live only in `gradle/libs.versions.toml`.
- Coverage via Kover, uploaded to Codecov from CI. A 90% `minBound` lands in PR #2.
- No instrumented/emulator tests in v0.1.
- Superpowers specs/plans live in `docs/superpowers/` locally but are gitignored — never
  committed nor PR'd. The relevant AGENTS.md sections are their durable record.
- Detekt is a build gate: `maxIssues: 0`, config in `config/detekt/detekt.yml`
  (deviations from defaults only), formatting rules included (fix issues manually or via
  the IDE; autoCorrect is not wired into the Gradle runs). Rule `excludes`
  override detekt's defaults — restore the default globs when adding to them. Run
  `./gradlew detekt` before pushing.
- `*.api` files (binary-compatibility-validator) are the frozen public API contract.
  Changing them is a deliberate API decision: regenerate with `./gradlew apiDump` and
  review the diff in the PR. The `demo` app is excluded. `quill-android` is also excluded
  until BCV supports AGP built-in Kotlin — its surface is `LogcatSink` plus the validated
  `quill-core` it re-exports.
- Releases are tag-driven: bump `VERSION_NAME` in `gradle.properties`, merge, push tag
  `v<version>` — the Release workflow verifies the tag matches, runs all gates, and
  uploads signed artifacts to the Central Portal; the actual release is manual from
  central.sonatype.com. Publishing lives in the `quill.publishing` convention plugin
  (Dokka + vanniktech maven-publish); coordinates and POM metadata come from
  `gradle.properties` (shared) and each module's `gradle.properties` (artifact id, name,
  description). Secrets required: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`,
  `SIGNING_KEY`, `SIGNING_KEY_PASSWORD` (same names as Conduit). `demo` is never
  published. `quill-android` publishes an empty javadoc jar (Dokka does not yet support
  AGP built-in Kotlin — see the comment in its build script); revisit on Dokka/AGP upgrades.
