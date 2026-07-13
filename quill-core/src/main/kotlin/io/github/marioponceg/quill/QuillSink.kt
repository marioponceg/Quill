package io.github.marioponceg.quill

/**
 * Receives every event that passes level filtering. [write] is synchronous and
 * non-suspend so logging works in an `onClick` or an `init` without a coroutine
 * scope; sinks that need I/O dispatch internally. Dispatch is sequential with
 * per-sink try/catch: a throwing sink takes down neither the app nor other sinks.
 */
public interface QuillSink {
    public fun write(event: QuillEvent)
}
