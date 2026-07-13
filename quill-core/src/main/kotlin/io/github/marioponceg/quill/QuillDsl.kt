package io.github.marioponceg.quill

/**
 * Configures the [Quill] singleton:
 *
 * ```
 * quill {
 *     minLevel = QuillLevel.Debug
 *     addSink(LogcatSink())
 * }
 * ```
 *
 * Calling it again replaces the previous configuration entirely.
 */
public fun quill(block: QuillConfigBuilder.() -> Unit) {
    Quill.config = QuillConfigBuilder().apply(block).build()
}

/** Receiver of the [quill] configuration lambda. */
public class QuillConfigBuilder internal constructor() {

    /** Events below this level are dropped without evaluating their field lambdas. */
    public var minLevel: QuillLevel = QuillLevel.Verbose

    private val sinks = mutableListOf<QuillSink>()

    public fun addSink(sink: QuillSink) {
        sinks += sink
    }

    internal fun build(): QuillConfig = QuillConfig(minLevel, sinks.toList())
}
