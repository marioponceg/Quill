package io.github.marioponceg.quill

/**
 * Global entry point. Configure once in the host `Application` with [quill]; obtain
 * loggers from anywhere with [logger]. Before configuration every logger is a silent
 * no-op, so library modules can never crash on logging. Tests should prefer
 * constructing [QuillLogger] directly over configuring the singleton.
 */
public object Quill {

    public const val VERSION: String = "0.1.0-SNAPSHOT"

    private val unconfigured = QuillConfig(QuillLevel.Verbose, emptyList())

    @Volatile
    internal var config: QuillConfig = unconfigured

    /** Returns a logger whose events carry [origin] and that follows the global config. */
    public fun logger(origin: String): QuillLogger = QuillLogger(origin) { config }

    /** Returns a logger using [T]'s simple class name as origin. */
    public inline fun <reified T : Any> logger(): QuillLogger =
        logger(T::class.simpleName ?: "Anonymous")

    /** Restores the unconfigured state. Test isolation hook. */
    internal fun reset() {
        config = unconfigured
    }
}
