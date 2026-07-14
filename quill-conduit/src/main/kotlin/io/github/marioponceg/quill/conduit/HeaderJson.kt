package io.github.marioponceg.quill.conduit

import io.github.marioponceg.conduit.http.Headers

private const val REDACTED = "██"

/**
 * Renders these headers as a compact JSON object string so quill-core's beautifier
 * pretty-prints them. Repeated names become an array; names in [redact] (compared
 * case-insensitively) have their values replaced with `██`.
 */
internal fun Headers.toRedactedJson(redact: Set<String>): String {
    val redactLower = redact.mapTo(HashSet()) { it.lowercase() }
    return names().joinToString(separator = ",", prefix = "{", postfix = "}") { name ->
        val values =
            if (name.lowercase() in redactLower) listOf(REDACTED) else values(name)
        val rendered =
            if (values.size == 1) values.single().toJsonString()
            else values.joinToString(",", "[", "]") { it.toJsonString() }
        "${name.toJsonString()}:$rendered"
    }
}

private fun String.toJsonString(): String = buildString(length + 2) {
    append('"')
    for (char in this@toJsonString) {
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (char < ' ') append("\\u%04x".format(char.code)) else append(char)
        }
    }
    append('"')
}
