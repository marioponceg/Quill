package io.github.marioponceg.quill

/**
 * Renders [QuillValue.Structured] raw content for humans: JSON and data class
 * toString() are pretty-printed with 4-space indent; anything else is returned
 * unchanged. Never throws — degradation, not exceptions. Shared by LogcatSink
 * (quill-android) and QuillInterceptor (quill-conduit).
 */
public object QuillBeautifier {

    public fun beautify(raw: String): String =
        JsonPrettyPrinter.prettyPrintOrNull(raw)
            ?: DataClassPrettyPrinter.prettyPrintOrNull(raw)
            ?: raw
}
