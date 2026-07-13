package io.github.marioponceg.quill

/** The effective logging configuration: global minimum level plus registered sinks. */
internal data class QuillConfig(
    val minLevel: QuillLevel,
    val sinks: List<QuillSink>,
)

/**
 * Emits [QuillEvent]s to sinks. Loggers obtained via [Quill.logger] read the
 * singleton's configuration at log time — created before `quill { }` they are
 * silent no-ops that come alive once configured. Constructed directly
 * (`QuillLogger("T", sinks = listOf(fakeSink))`) they use a fixed configuration,
 * which is the intended path for tests.
 *
 * Field lambdas are only evaluated when the event will actually be written, so
 * filtered-out logs cost nothing beyond a level comparison.
 */
public class QuillLogger internal constructor(
    public val origin: String,
    private val configProvider: () -> QuillConfig,
) {

    public constructor(
        origin: String,
        sinks: List<QuillSink>,
        minLevel: QuillLevel = QuillLevel.Verbose,
    ) : this(origin, { QuillConfig(minLevel, sinks) })

    public fun verbose(
        name: String,
        throwable: Throwable? = null,
        fields: QuillFieldBuilder.() -> Unit = {},
    ) {
        log(QuillLevel.Verbose, name, throwable, fields)
    }

    public fun debug(
        name: String,
        throwable: Throwable? = null,
        fields: QuillFieldBuilder.() -> Unit = {},
    ) {
        log(QuillLevel.Debug, name, throwable, fields)
    }

    public fun info(
        name: String,
        throwable: Throwable? = null,
        fields: QuillFieldBuilder.() -> Unit = {},
    ) {
        log(QuillLevel.Info, name, throwable, fields)
    }

    public fun warn(
        name: String,
        throwable: Throwable? = null,
        fields: QuillFieldBuilder.() -> Unit = {},
    ) {
        log(QuillLevel.Warn, name, throwable, fields)
    }

    public fun error(
        name: String,
        throwable: Throwable? = null,
        fields: QuillFieldBuilder.() -> Unit = {},
    ) {
        log(QuillLevel.Error, name, throwable, fields)
    }

    public fun log(
        level: QuillLevel,
        name: String,
        throwable: Throwable? = null,
        fields: QuillFieldBuilder.() -> Unit = {},
    ) {
        val config = configProvider()
        if (config.sinks.isEmpty() || level < config.minLevel) return
        val event = QuillEvent(
            timestamp = System.currentTimeMillis(),
            level = level,
            origin = origin,
            name = name,
            fields = QuillFieldBuilder().apply(fields).build(),
            throwable = throwable,
        )
        for (sink in config.sinks) {
            try {
                sink.write(event)
            } catch (_: Throwable) {
                // A sink must never take down the host app or the other sinks.
            }
        }
    }
}
