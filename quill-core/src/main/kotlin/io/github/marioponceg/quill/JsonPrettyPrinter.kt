package io.github.marioponceg.quill

/**
 * Hand-written JSON pretty-printer (no dependencies — consumers' classes need no
 * annotations). Validates while it formats: [prettyPrintOrNull] returns null for
 * anything that is not a single, complete, well-formed JSON object or array, so
 * callers can fall back to other renderings. 4-space indent.
 */
internal object JsonPrettyPrinter {

    private const val INDENT = "    "
    private const val MAX_DEPTH = 200
    private val NUMBER_REGEX = Regex("""-?(0|[1-9][0-9]*)(\.[0-9]+)?([eE][+-]?[0-9]+)?""")

    fun prettyPrintOrNull(raw: String): String? {
        val out = StringBuilder()
        return if (parse(raw, out)) out.toString() else null
    }

    /**
     * Validates that [raw] is a single, complete, well-formed JSON object or array,
     * without building the pretty-printed output. Shares the [Parser] traversal with
     * [prettyPrintOrNull] so the two never diverge on what counts as valid JSON.
     */
    internal fun isValidJson(raw: String): Boolean = parse(raw, out = null)

    /** Runs the shared grammar traversal, appending to [out] only when it is non-null. */
    private fun parse(raw: String, out: StringBuilder?): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || (trimmed.first() != '{' && trimmed.first() != '[')) return false
        val parser = Parser(trimmed)
        return try {
            parser.appendValue(out, depth = 0)
            parser.skipWhitespace()
            parser.atEnd
        } catch (_: MalformedJson) {
            false
        }
    }

    private class MalformedJson : Exception()

    private class Parser(private val source: String) {
        private var index = 0

        val atEnd: Boolean get() = index >= source.length

        fun skipWhitespace() {
            while (!atEnd && source[index].isWhitespace()) index++
        }

        fun appendValue(out: StringBuilder?, depth: Int) {
            if (depth > MAX_DEPTH) throw MalformedJson()
            skipWhitespace()
            when (peek()) {
                '{' -> appendObject(out, depth)
                '[' -> appendArray(out, depth)
                '"' -> {
                    val string = readString()
                    out?.append(string)
                }
                else -> {
                    val literal = readLiteral()
                    out?.append(literal)
                }
            }
        }

        private fun appendObject(out: StringBuilder?, depth: Int) {
            expect('{')
            skipWhitespace()
            if (peek() == '}') {
                index++
                out?.append("{}")
                return
            }
            out?.append("{\n")
            while (true) {
                skipWhitespace()
                indent(out, depth + 1)
                val key = readString()
                out?.append(key)
                skipWhitespace()
                expect(':')
                out?.append(": ")
                appendValue(out, depth + 1)
                skipWhitespace()
                when (peek()) {
                    ',' -> {
                        index++
                        out?.append(",\n")
                    }
                    '}' -> {
                        index++
                        out?.append('\n')
                        indent(out, depth)
                        out?.append('}')
                        return
                    }
                    else -> throw MalformedJson()
                }
            }
        }

        private fun appendArray(out: StringBuilder?, depth: Int) {
            expect('[')
            skipWhitespace()
            if (peek() == ']') {
                index++
                out?.append("[]")
                return
            }
            out?.append("[\n")
            while (true) {
                skipWhitespace()
                indent(out, depth + 1)
                appendValue(out, depth + 1)
                skipWhitespace()
                when (peek()) {
                    ',' -> {
                        index++
                        out?.append(",\n")
                    }
                    ']' -> {
                        index++
                        out?.append('\n')
                        indent(out, depth)
                        out?.append(']')
                        return
                    }
                    else -> throw MalformedJson()
                }
            }
        }

        /** Reads a quoted string including its quotes, honoring backslash escapes. */
        private fun readString(): String {
            if (peek() != '"') throw MalformedJson()
            val start = index
            index++
            while (!atEnd) {
                when (source[index]) {
                    '\\' -> index += 2
                    '"' -> {
                        index++
                        return source.substring(start, index)
                    }
                    else -> index++
                }
            }
            throw MalformedJson()
        }

        /** Reads true/false/null or a number; anything else is malformed. */
        private fun readLiteral(): String {
            val start = index
            while (!atEnd && source[index] !in ",]}" && !source[index].isWhitespace()) index++
            val literal = source.substring(start, index)
            val valid = literal == "true" || literal == "false" || literal == "null" ||
                NUMBER_REGEX.matches(literal)
            if (!valid) throw MalformedJson()
            return literal
        }

        private fun peek(): Char {
            if (atEnd) throw MalformedJson()
            return source[index]
        }

        private fun expect(expected: Char) {
            if (atEnd || source[index] != expected) throw MalformedJson()
            index++
        }

        private fun indent(out: StringBuilder?, depth: Int) {
            if (out == null) return
            repeat(depth) { out.append(INDENT) }
        }
    }
}
