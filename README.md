# Quill

[![CI](https://github.com/marioponceg/Quill/actions/workflows/ci.yml/badge.svg)](https://github.com/marioponceg/Quill/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/marioponceg/Quill/branch/main/graph/badge.svg)](https://codecov.io/gh/marioponceg/Quill)

Structured, type-safe logging for Kotlin — typed tags, zero-cost log calls, and a composable sink pipeline.

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
