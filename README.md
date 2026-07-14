# Quill

[![CI](https://github.com/marioponceg/Quill/actions/workflows/ci.yml/badge.svg)](https://github.com/marioponceg/Quill/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/marioponceg/Quill/branch/main/graph/badge.svg)](https://codecov.io/gh/marioponceg/Quill)

Structured logging for Android and the JVM — every log is an event with typed fields,
rendered as pretty boxed logcat output by sinks that never crash your app.

```kotlin
private val log = Quill.logger("AuthRepository")

log.info("user_login") {
    "userId" to 42
    "method" to "oauth"
}
```

```
I/Quill.AuthRepository: ┌─ user_login ─────────────────────────
                        │ userId: 42
                        │ method: "oauth"
                        └──────────────────────────────────────
```

## Why Quill

- **Events, not sentences.** Each log is a `QuillEvent` — level, origin, event name, typed
  fields, optional throwable. Structure survives all the way to the sinks; formatting is a
  sink concern, never a call-site concern.
- **Zero-cost when disabled.** Field lambdas are never evaluated for filtered-out levels —
  a verbose log in a hot path costs one level comparison in production.
- **Never crashes the host app.** Sink exceptions are caught per sink, so a broken sink
  takes down neither the app nor the other sinks. Logging before configuration is a silent
  no-op — library modules can log freely.
- **Beautified values with zero requirements.** JSON strings and data classes pretty-print
  in the sink through a hand-written beautifier — no annotations, no reflection, no
  serialization dependency. Anything that doesn't parse prints raw instead of throwing.
- **Pure Kotlin core.** `quill-core` depends on the stdlib alone; the Android and Conduit
  integrations live in their own modules.

## Modules

| Module | What it gives you |
|---|---|
| `quill-core` | Event model (`QuillEvent`, `QuillValue`), `QuillLogger`, the `quill { }` DSL and the beautifier. Pure Kotlin/JVM |
| `quill-android` | `LogcatSink`: boxed Unicode output, tag prefixing and 4000-char chunking. Exposes `quill-core` transitively (`api`) |
| `quill-conduit` | `QuillInterceptor`: every Conduit HTTP exchange logged as Quill events |
| `demo` | Android showcase app exercising every feature; never published |

> Maven Central publishing is on the way under the `io.github.marioponceg` namespace.
> Until then, consume Quill via a Gradle
> [composite build](https://docs.gradle.org/current/userguide/composite_builds.html)
> (`includeBuild`) or `mavenLocal`.

## Getting started

Configure Quill once, in your `Application`:

```kotlin
quill {
    minLevel = QuillLevel.Debug
    addSink(LogcatSink())
}
```

Loggers created before `quill { }` runs are silent no-ops that come alive with the
configuration — logging from a library module is always safe.

## Logging

Get a logger anywhere with `Quill.logger`; the origin becomes part of the logcat tag. The
message is an event name, and fields are typed key-value pairs built from plain
`"key" to value`:

```kotlin
private val log = Quill.logger("SyncWorker")

log.error("sync_failed", throwable = e) {
    "retries" to 3
    "payload" to userDto      // data class → beautified by the sink
}
```

Levels are Android's five — `Verbose`, `Debug`, `Info`, `Warn`, `Error` — nothing
invented. Values convert automatically: primitives stay primitive, `null` stays null, and
strings that parse as JSON or objects with a data-class `toString()` become structured
values the sink pretty-prints.

## Logcat output

`LogcatSink` renders each event as a Unicode box, so interleaved multi-line logs from
concurrent threads stay readable:

```
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

The tag is `tagPrefix + "." + origin` (`Quill` by default), so Android Studio filters
cover every case: `tag~:^Quill\.` for all Quill logs, `tag:Quill.SyncWorker` for one
origin. Events longer than logcat's 4000-char limit are chunked across multiple writes
with the box preserved. `LogcatSink(boxed = false)` renders flat single lines instead,
and a per-sink `minLevel` filters on top of the global one.

## Conduit integration

`quill-conduit` logs every [Conduit](https://github.com/marioponceg/Conduit) HTTP exchange
as structured events — `http_request` on the way out, `http_response` or `http_failure`
on the way back, correlated by a `requestId` field and all tagged `Quill.Http`:

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

`Authorization` headers are redacted by default, and verbosity follows OkHttp's logging
interceptor levels: `Basic` for production (method, URL, code, duration), `Body` for
debug builds. Conduit itself never depends on Quill.

## Demo app

The `demo` app exercises every feature by hand — the five levels, beautified fields,
throwables, chunking and live Conduit HTTP logging — one button per scenario:

```bash
./gradlew :demo:installDebug
```

## Design notes

Quill is a portfolio project built with senior-engineering discipline: every design unit
lands as a reviewed PR with tests first, 90% minimum line coverage enforced in CI, and
the reasoning behind each decision recorded in [AGENTS.md](AGENTS.md).

## License

[Apache 2.0](LICENSE)
