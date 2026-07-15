package io.github.marioponceg.quill.android

import io.github.marioponceg.quill.QuillBeautifier
import io.github.marioponceg.quill.QuillEvent
import io.github.marioponceg.quill.QuillValue

/**
 * Pure formatting for [LogcatSink]: event in, logcat lines out. Kept free of any
 * Android API so it is testable on the JVM. With [boxed] (the default) each event
 * is delimited by a Unicode box so interleaved multi-line logs from concurrent
 * threads stay readable; `boxed = false` renders one flat line per event (structured values minified).
 */
internal class LogcatFormatter(
    private val boxed: Boolean = true,
) {

    fun format(event: QuillEvent): List<String> =
        if (boxed) formatBoxed(event) else listOf(formatFlat(event))

    private fun formatBoxed(event: QuillEvent): List<String> = buildList {
        val header = "┌─ ${sanitizeName(event.name)} "
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
        append(sanitizeName(event.name))
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
        is QuillValue.Structured -> QuillBeautifier.compact(value.raw)
        else -> renderBoxed(value)
    }

    private fun renderBoxed(value: QuillValue): String = when (value) {
        is QuillValue.Text -> "\"${escapeText(value.value)}\""
        is QuillValue.Number -> value.value.toString()
        is QuillValue.Bool -> value.value.toString()
        is QuillValue.Structured -> QuillBeautifier.beautify(value.raw)
        QuillValue.Null -> "null"
    }

    /** Text renders inside quotes: escape the quote, the escape char, and controls. */
    private fun escapeText(value: String): String = buildString(value.length + 8) {
        for (ch in value) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                else -> appendEscapedControl(ch)
            }
        }
    }

    /** Names are identifiers, not prose: controls are escaped, everything else passes. */
    private fun sanitizeName(name: String): String = buildString(name.length) {
        for (ch in name) appendEscapedControl(ch)
    }

    private fun StringBuilder.appendEscapedControl(ch: Char) {
        when (ch) {
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (ch.isISOControl()) append("\\u%04x".format(ch.code)) else append(ch)
        }
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
