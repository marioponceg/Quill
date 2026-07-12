# Quill — Structured Logging for Android & JVM: Design

**Date:** 2026-07-13
**Status:** Approved design, pending implementation plan

## Overview

Quill is a structured logging library for Android and the JVM. Instead of interpolated
message strings, each log is an **event** with a level, an origin, an event name and
**typed key-value fields**. Sinks decide how events are rendered; the built-in Android
sink renders them as pretty, boxed, multi-line logcat output with JSON and data class
beautification.

Quill is the third library in the `io.github.marioponceg` family and follows the same
conventions as Conduit (networking) and Foundry (Compose design system): modular core
with integrations in separate modules, coroutine-friendly, published under
`io.github.marioponceg:quill-*`.

## Goals (v0.1)

- Log from any layer (data, domain, UI) of any module with only a `quill-core` dependency.
- Events carry: level, origin (where it came from), event name, typed fields, optional throwable.
- Pretty logcat output: boxed events, JSON and data class values pretty-printed.
- Logcat output filterable as a group via a tag prefix (`Quill.` by default).
- First-class Conduit integration: HTTP request/response logs in the same Quill format,
  via a separate `quill-conduit` module. **Conduit itself never depends on Quill.**
- Zero-cost disabled logs: field lambdas are not evaluated when the level is filtered out.
- Never crash the host app: sink exceptions are isolated; unconfigured loggers are no-ops.

## Non-goals (v0.1 — recorded as future work)

- Maven Central publishing (added after v0.1 works end to end).
- detekt / Kover / apiCheck in CI (added later to reach parity with Conduit's pipeline).
- File/JSON sink, remote sinks.
- Coroutine-propagated ambient context (scoped fields attached to all logs in a scope).
- Kotlin Multiplatform (Android + JVM only for now).
- A published `quill-test` artifact (FakeSink stays internal until users ask for it).

## Module structure

```
quill-core      Kotlin/JVM       model, logger, DSL, beautifier      (dep: stdlib only)
quill-android   Android library  LogcatSink                          (dep: quill-core via api)
quill-conduit   Kotlin/JVM       QuillInterceptor                    (dep: quill-core + conduit-core)
demo            Android app      showcase, like Foundry's catalog    (not published)
```

The current scaffold `app` module becomes `demo`. Root namespace:
`io.github.marioponceg.quill`.

### Decoupling rule

`quill-conduit` is the **only** place that knows both libraries. Conduit contributes
nothing but its existing interceptor seam. Apps that want Quill-formatted HTTP logs add
`quill-conduit` and register the interceptor; apps that don't, pull in nothing.

```
conduit-core          quill-core
     ▲                    ▲
     └── quill-conduit ───┘        ← the ONLY module that knows both
              ▲
              └── consuming app
```

## Public API

### Setup (once, in `Application`)

```kotlin
quill {
    minLevel = QuillLevel.Debug
    addSink(LogcatSink())
}
```

### Logging (from any module; requires only quill-core)

```kotlin
private val log = Quill.logger("AuthRepository")   // or Quill.logger<AuthRepository>()

log.info("user_login") {
    "userId" to 42
    "method" to "oauth"
}

log.error("sync_failed", throwable = e) {
    "retries" to 3
    "payload" to userDto          // data class → beautified by the sink
}
```

### API decisions

- **Levels:** `Verbose, Debug, Info, Warn, Error` — Android's levels, nothing invented.
- **The message is an event name** (`"user_login"`), not an interpolated sentence. Plain
  sentences work, but the API nudges toward events.
- **Fields via builder lambda** (`"key" to value`): when the level is filtered out the
  lambda is never evaluated — zero cost in production for debug logs.
- **Hybrid static/instance:** `Quill.logger(...)` works from anywhere after a single
  `quill { }` configuration; tests construct `QuillLogger(sinks = listOf(fakeSink))`
  directly without touching the singleton.
- **Safe before configuration:** an unconfigured `Quill.logger(...)` is a silent no-op,
  so library modules can never crash on logging.

## Event model (quill-core)

```kotlin
data class QuillEvent(
    val timestamp: Long,              // epoch millis
    val level: QuillLevel,
    val origin: String,               // "AuthRepository" — the logger's tag
    val name: String,                 // "user_login"
    val fields: Map<String, QuillValue>,
    val throwable: Throwable? = null,
)

sealed interface QuillValue {
    data class Text(val value: String) : QuillValue
    data class Number(val value: kotlin.Number) : QuillValue
    data class Bool(val value: Boolean) : QuillValue
    data class Structured(val raw: String) : QuillValue   // JSON or data class toString()
    object Null : QuillValue
}

interface QuillSink {
    fun write(event: QuillEvent)
}
```

- **`QuillValue` instead of `Any?`:** sinks know what they hold without type checks on
  arbitrary objects. The field builder converts automatically: primitives →
  `Text/Number/Bool`; strings that parse as JSON → `Structured`; any other object →
  `Structured(obj.toString())`. Users never see `QuillValue`.
- **`Structured` stores the raw, unformatted value** — pretty-printing happens in the
  sink. A future remote sink would ship it compact as-is. Structure is never lost at the
  call site.
- **`write` is synchronous and non-suspend:** logging must work in an `onClick` or an
  `init` without a coroutine scope. Sinks that need I/O dispatch internally.
- **Dispatch is sequential with per-sink try/catch:** a throwing sink takes down neither
  the app nor the other sinks.

## Beautifier (quill-core)

Shared by `LogcatSink` and `QuillInterceptor`:

- **JSON:** hand-written pretty-printer (~100 lines, no dependencies), 4-space indent.
  No kotlinx.serialization dependency — consumers' classes need no annotations.
- **Data classes:** parses the standard `toString()` output — splits on commas while
  respecting paren/bracket nesting depth. Works with any data class, nested included,
  with zero requirements on the consumer (no reflection, no serialization).
- **Never fails:** anything that doesn't parse is printed raw. Degradation, not exceptions.

## LogcatSink (quill-android)

```
D/Quill.AuthRepository: ┌─ user_login ─────────────────────────
                        │ userId: 42
                        │ method: "oauth"
                        └───────────────────────────────────────
E/Quill.SyncWorker: ┌─ sync_failed ────────────────────────────
                    │ retries: 3
                    │ payload: UserDto(
                    │     id = 42,
                    │     name = "Mario",
                    │     roles = [admin, editor]
                    │ )
                    │ ▼ IOException: timeout after 30s
                    │     at SyncWorker.sync(SyncWorker.kt:87)
                    └───────────────────────────────────────────
```

- **Logcat tag = `tagPrefix + "." + origin`.** Default `tagPrefix = "Quill"`; set
  `tagPrefix = null` for bare origins. With the prefix, Android Studio logcat filters:
  - `tag~:^Quill\.` → all Quill logs
  - `tag:Quill.AuthRepository` → one origin
  - minSdk is 26, so the legacy 23-char tag limit does not apply.
- **Unicode box** (`┌ │ └`) delimits each event so interleaved multi-line logs from
  concurrent threads stay readable. `boxed = false` renders flat single lines
  (`user_login  userId=42 method="oauth"`).
- **4000-char chunking:** logcat truncates long messages; the sink splits events across
  multiple `Log.println` calls preserving the box prefix so large JSON bodies survive.
- **Optional per-sink `minLevel`** on top of the global one.
- Formatting logic lives in a pure `LogcatFormatter` class (strings in → lines out) so it
  is JVM-testable; the `Log.println` wrapper around it is trivially thin.

## Conduit integration (quill-conduit)

```kotlin
val client = conduit {
    engine = OkHttpEngine()
    baseUrl = "https://api.example.com"
    interceptors += QuillInterceptor(
        level = BodyLevel.Body,                    // None | Basic | Headers | Body
        redactHeaders = setOf("Authorization"),    // default
    )
}
```

- **Two events per call:** `http_request` on the way out, `http_response` or
  `http_failure` on the way back, correlated by a short `requestId` field. Failures map
  from Conduit's typed `ConduitResult.Failure` variants (HTTP / network / serialization).
- **`BodyLevel` like OkHttp's logging interceptor:** `Basic` for production (method, url,
  code, duration), `Body` for debug builds. Response bodies go through the shared
  beautifier.
- **Redaction by default** for `Authorization` (configurable set) — no tokens in logcat.
- **Fixed origin `Http`** → tag `Quill.Http`; all network logs filterable at once.

## Testing

- **quill-core (pure JVM, JUnit):** field builder conversions to `QuillValue`; level
  filtering including lambda non-evaluation; sink dispatch exception isolation;
  **beautifier golden tests** (exact raw-input → expected-output pairs: nested JSON,
  nested data classes, lists, malformed input degrading to raw).
- **FakeSink:** in-memory event list with assertions; internal test utility for v0.1.
- **quill-android:** `LogcatFormatter` tested on the JVM (box rendering, 4000-char
  chunking). The `Log.println` wrapper is thin enough to skip.
- **quill-conduit:** interceptor tests against a fake Conduit engine, asserting emitted
  events for success / failure / redaction. If Conduit exposes no test utilities, resolve
  during planning (likely a minimal fake engine in this repo's test sources).
- **demo:** manual verification surface; not part of CI assertions.

## Error handling summary

| Failure | Behavior |
|---|---|
| Sink throws | Caught per sink; other sinks still receive the event |
| Beautifier can't parse a value | Prints the raw value unchanged |
| Logging before `quill { }` | Silent no-op |
| Event exceeds logcat limit | Chunked across multiple writes, box preserved |
