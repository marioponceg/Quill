package io.github.marioponceg.quill.conduit

/**
 * How much of each HTTP exchange [QuillInterceptor] logs, modeled after OkHttp's
 * logging interceptor levels. Declaration order defines verbosity, so
 * `level >= Headers` works via the enum's natural [Comparable].
 */
public enum class BodyLevel {
    /** Log nothing; the interceptor is a pass-through. */
    None,

    /** Method, URL, status code and duration — production default. */
    Basic,

    /** [Basic] plus request and response headers (redacted per configuration). */
    Headers,

    /** [Headers] plus request and response bodies decoded as UTF-8 text. */
    Body,
}
