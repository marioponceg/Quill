package io.github.marioponceg.quill.android

/**
 * Logcat truncates messages around 4 KB. [chunk] repacks an event's lines into
 * messages of at most [MAX_MESSAGE_LENGTH] chars so large payloads survive across
 * multiple `Log.println` calls; over-long lines are split and their continuation
 * pieces get [the prefix] so the box is preserved.
 */
internal object LogcatChunker {

    const val MAX_MESSAGE_LENGTH: Int = 4000

    fun chunk(
        lines: List<String>,
        continuationPrefix: String,
        maxLength: Int = MAX_MESSAGE_LENGTH,
    ): List<String> {
        val pieces = lines.flatMap { splitLine(it, continuationPrefix, maxLength) }
        val chunks = mutableListOf<String>()
        val current = StringBuilder()
        for (piece in pieces) {
            if (current.isNotEmpty() && current.length + 1 + piece.length > maxLength) {
                chunks += current.toString()
                current.setLength(0)
            }
            if (current.isNotEmpty()) current.append('\n')
            current.append(piece)
        }
        if (current.isNotEmpty()) chunks += current.toString()
        return chunks
    }

    private fun splitLine(line: String, continuationPrefix: String, maxLength: Int): List<String> {
        if (line.length <= maxLength) return listOf(line)
        val pieces = mutableListOf(line.substring(0, maxLength))
        var rest = line.substring(maxLength)
        val budget = (maxLength - continuationPrefix.length).coerceAtLeast(1)
        while (rest.length > budget) {
            pieces += continuationPrefix + rest.substring(0, budget)
            rest = rest.substring(budget)
        }
        pieces += continuationPrefix + rest
        return pieces
    }
}
