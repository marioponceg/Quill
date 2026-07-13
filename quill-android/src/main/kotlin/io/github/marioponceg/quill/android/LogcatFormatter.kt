package io.github.marioponceg.quill.android

import io.github.marioponceg.quill.QuillBeautifier
import io.github.marioponceg.quill.QuillEvent
import io.github.marioponceg.quill.QuillValue

/**
 * Pure formatting for [LogcatSink]: event in, logcat lines out. Kept free of any
 * Android API so it is testable on the JVM. With [boxed] (the default) each event
 * is delimited by a Unicode box so interleaved multi-line logs from concurrent
 * threads stay readable; `boxed = false` renders one flat line per event.
 */
public class LogcatFormatter(
    private val boxed: Boolean = true,
) {

    public fun format(event: QuillEvent): List<String> =
        if (boxed) formatBoxed(event) else listOf(formatFlat(event))

    private fun formatBoxed(event: QuillEvent): List<String> = buildList {
        val header = "┌─ ${event.name} "
        add(header + "─".repeat((BOX_WIDTH - header.length).coerceAtLeast(MIN_TRAILING_DASHES)))
        for ((key, value) in event.fields) {
            val valueLines = renderBoxed(value).lines()
            add("│ $key: ${valueLines.first()}")
            for (line in valueLines.drop(1)) add("│ $line")
        }
        event.throwable?.let { addAll(throwableLines(it)) }
        add("└" + "─".repeat(BOX_WIDTH - 1))
    }

    private fun formatFlat(event: QuillEvent): String = buildString {
        append(event.name)
        if (event.fields.isNotEmpty()) {
            append("  ")
            append(
                event.fields.entries.joinToString(" ") { (key, value) ->
                    "$key=${renderFlat(value)}"
                },
            )
        }
        event.throwable?.let { append("  ▼ ${throwableHeader(it)}") }
    }

    private fun renderFlat(value: QuillValue): String = when (value) {
        is QuillValue.Structured -> value.raw
        else -> renderBoxed(value)
    }

    private fun renderBoxed(value: QuillValue): String = when (value) {
        is QuillValue.Text -> "\"${value.value}\""
        is QuillValue.Number -> value.value.toString()
        is QuillValue.Bool -> value.value.toString()
        is QuillValue.Structured -> QuillBeautifier.beautify(value.raw)
        QuillValue.Null -> "null"
    }

    private fun throwableLines(throwable: Throwable): List<String> = buildList {
        add("│ ▼ ${throwableHeader(throwable)}")
        for (line in throwable.stackTraceToString().lines().drop(1)) {
            if (line.isBlank()) continue
            add("│     ${line.trim()}")
        }
    }

    private fun throwableHeader(throwable: Throwable): String {
        val name = throwable::class.simpleName ?: "Throwable"
        val message = throwable.message
        return if (message == null) name else "$name: $message"
    }

    private companion object {
        const val BOX_WIDTH = 60
        const val MIN_TRAILING_DASHES = 3
    }
}
