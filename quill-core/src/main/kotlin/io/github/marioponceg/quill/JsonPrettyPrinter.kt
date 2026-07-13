package io.github.marioponceg.quill

/**
 * Hand-written JSON pretty-printer (no dependencies — consumers' classes need no
 * annotations). Validates while it formats: [prettyPrintOrNull] returns null for
 * anything that is not a single, complete, well-formed JSON object or array, so
 * callers can fall back to other renderings. 4-space indent.
 */
internal object JsonPrettyPrinter {

    private const val INDENT = "    "

    fun prettyPrintOrNull(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || (trimmed.first() != '{' && trimmed.first() != '[')) return null
        val parser = Parser(trimmed)
        return try {
            val out = StringBuilder()
            parser.appendValue(out, depth = 0)
            parser.skipWhitespace()
            if (parser.atEnd) out.toString() else null
        } catch (_: MalformedJson) {
            null
        }
    }

    private class MalformedJson : Exception()

    private class Parser(private val source: String) {
        private var index = 0

        val atEnd: Boolean get() = index >= source.length

        fun skipWhitespace() {
            while (!atEnd && source[index].isWhitespace()) index++
        }

        fun appendValue(out: StringBuilder, depth: Int) {
            skipWhitespace()
            when (peek()) {
                '{' -> appendObject(out, depth)
                '[' -> appendArray(out, depth)
                '"' -> out.append(readString())
                else -> out.append(readLiteral())
            }
        }

        private fun appendObject(out: StringBuilder, depth: Int) {
            expect('{')
            skipWhitespace()
            if (peek() == '}') {
                index++
                out.append("{}")
                return
            }
            out.append("{\n")
            while (true) {
                skipWhitespace()
                indent(out, depth + 1)
                out.append(readString())
                skipWhitespace()
                expect(':')
                out.append(": ")
                appendValue(out, depth + 1)
                skipWhitespace()
                when (peek()) {
                    ',' -> {
                        index++
                        out.append(",\n")
                    }
                    '}' -> {
                        index++
                        out.append('\n')
                        indent(out, depth)
                        out.append('}')
                        return
                    }
                    else -> throw MalformedJson()
                }
            }
        }

        private fun appendArray(out: StringBuilder, depth: Int) {
            expect('[')
            skipWhitespace()
            if (peek() == ']') {
                index++
                out.append("[]")
                return
            }
            out.append("[\n")
            while (true) {
                skipWhitespace()
                indent(out, depth + 1)
                appendValue(out, depth + 1)
                skipWhitespace()
                when (peek()) {
                    ',' -> {
                        index++
                        out.append(",\n")
                    }
                    ']' -> {
                        index++
                        out.append('\n')
                        indent(out, depth)
                        out.append(']')
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
                literal.toDoubleOrNull() != null
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

        private fun indent(out: StringBuilder, depth: Int) {
            repeat(depth) { out.append(INDENT) }
        }
    }
}
