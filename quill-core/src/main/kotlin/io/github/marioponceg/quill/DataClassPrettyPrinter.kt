package io.github.marioponceg.quill

/**
 * Pretty-printer for standard `data class` toString() output, e.g.
 * `UserDto(id=42, roles=[admin, editor], address=Address(street=Main))`. No
 * reflection and zero requirements on the consumer: it re-renders the string,
 * splitting on top-level commas while respecting paren/bracket nesting. Nested
 * data classes expand onto indented lines; lists and plain values stay inline.
 */
internal object DataClassPrettyPrinter {

    private const val INDENT = "    "
    private const val MAX_DEPTH = 200
    private val CLASS_NAME = Regex("""^[A-Za-z_$][A-Za-z0-9_$.]*$""")

    fun prettyPrintOrNull(raw: String): String? {
        val trimmed = raw.trim()
        if (!looksLikeDataClass(trimmed)) return null
        if (nestingDepth(trimmed) > MAX_DEPTH) return null
        return buildString { appendDataClass(trimmed, depth = 0) }
    }

    /** Max paren/bracket nesting depth, so absurdly deep input can be rejected before recursing. */
    private fun nestingDepth(value: String): Int {
        var depth = 0
        var maxDepth = 0
        for (c in value) {
            when (c) {
                '(', '[', '{' -> {
                    depth++
                    if (depth > maxDepth) maxDepth = depth
                }
                ')', ']', '}' -> depth--
            }
        }
        return maxDepth
    }

    private fun looksLikeDataClass(value: String): Boolean {
        val open = value.indexOf('(')
        if (open <= 0 || !value.endsWith(")")) return false
        if (!CLASS_NAME.matches(value.substring(0, open))) return false
        return isBalanced(value.substring(open))
    }

    private fun isBalanced(value: String): Boolean {
        var depth = 0
        for (c in value) {
            when (c) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> {
                    depth--
                    if (depth < 0) return false
                }
            }
        }
        return depth == 0
    }

    private fun StringBuilder.appendDataClass(value: String, depth: Int) {
        val open = value.indexOf('(')
        val name = value.substring(0, open)
        val body = value.substring(open + 1, value.length - 1)
        if (body.isBlank()) {
            append(name).append("()")
            return
        }
        append(name).append("(\n")
        val entries = splitTopLevel(body)
        entries.forEachIndexed { i, entry ->
            repeat(depth + 1) { append(INDENT) }
            appendEntry(entry.trim(), depth + 1)
            if (i < entries.lastIndex) append(',')
            append('\n')
        }
        repeat(depth) { append(INDENT) }
        append(')')
    }

    private fun StringBuilder.appendEntry(entry: String, depth: Int) {
        val eq = topLevelIndexOf(entry, '=')
        if (eq == -1) {
            append(entry)
            return
        }
        val key = entry.substring(0, eq).trim()
        val value = entry.substring(eq + 1).trim()
        append(key).append(" = ")
        if (looksLikeDataClass(value)) appendDataClass(value, depth) else append(value)
    }

    private fun splitTopLevel(body: String): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var start = 0
        body.forEachIndexed { i, c ->
            when (c) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                ',' -> if (depth == 0) {
                    parts += body.substring(start, i)
                    start = i + 1
                }
            }
        }
        parts += body.substring(start)
        return parts
    }

    private fun topLevelIndexOf(entry: String, target: Char): Int {
        var depth = 0
        entry.forEachIndexed { i, c ->
            when (c) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                target -> if (depth == 0) return i
            }
        }
        return -1
    }
}
