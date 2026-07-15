package io.github.marioponceg.quill

/**
 * Renders [QuillValue.Structured] raw content for humans: [beautify] pretty-prints
 * JSON and data class toString() with 4-space indent; [compact] renders them as a
 * single line. Anything unparseable is returned unchanged. Never throws — degradation,
 * not exceptions. Shared by LogcatSink (quill-android) and QuillInterceptor
 * (quill-conduit).
 */
public object QuillBeautifier {

    private val NEWLINE_RUNS = Regex("""\s*\n\s*""")

    public fun beautify(raw: String): String =
        JsonPrettyPrinter.prettyPrintOrNull(raw)
            ?: DataClassPrettyPrinter.prettyPrintOrNull(raw)
            ?: raw

    /**
     * Renders [QuillValue.Structured] raw content as a single line: valid JSON is
     * minified; anything else has newline runs collapsed to one space and is trimmed.
     * Never throws — degradation, not exceptions.
     */
    public fun compact(raw: String): String =
        JsonPrettyPrinter.compactOrNull(raw)
            ?: raw.trim().replace(NEWLINE_RUNS, " ")
}
