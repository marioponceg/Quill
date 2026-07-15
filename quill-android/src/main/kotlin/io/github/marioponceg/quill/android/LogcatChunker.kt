package io.github.marioponceg.quill.android

/**
 * Logcat truncates messages around 4 KB of payload. [chunk] repacks an event's lines
 * into messages of at most [MAX_MESSAGE_BYTES] UTF-8 bytes so large payloads survive
 * across multiple `Log.println` calls; over-long lines are split at code-point
 * boundaries and their continuation pieces get the continuation prefix so the box is
 * preserved; a single code point or prefix that cannot fit degrades to a slight
 * overflow instead of looping.
 */
internal object LogcatChunker {

    const val MAX_MESSAGE_BYTES: Int = 4000

    // UTF-8 encoding length boundaries: code points below each threshold fit in the
    // preceding number of bytes (RFC 3629).
    private const val UTF8_ONE_BYTE_LIMIT = 0x80
    private const val UTF8_TWO_BYTE_LIMIT = 0x800
    private const val UTF8_THREE_BYTE_LIMIT = 0x10000
    private const val UTF8_THREE_BYTES = 3
    private const val UTF8_FOUR_BYTES = 4

    fun chunk(
        lines: List<String>,
        continuationPrefix: String,
        maxBytes: Int = MAX_MESSAGE_BYTES,
    ): List<String> {
        val pieces = lines.flatMap { splitLine(it, continuationPrefix, maxBytes) }
        val chunks = mutableListOf<String>()
        val current = StringBuilder()
        var currentBytes = 0
        var hasContent = false
        for (piece in pieces) {
            val pieceBytes = piece.utf8Size()
            if (hasContent && currentBytes + 1 + pieceBytes > maxBytes) {
                chunks += current.toString()
                current.setLength(0)
                currentBytes = 0
                hasContent = false
            }
            if (hasContent) {
                current.append('\n')
                currentBytes++
            }
            current.append(piece)
            currentBytes += pieceBytes
            hasContent = true
        }
        if (hasContent) chunks += current.toString()
        return chunks
    }

    private fun splitLine(line: String, continuationPrefix: String, maxBytes: Int): List<String> {
        if (line.utf8Size() <= maxBytes) return listOf(line)
        // If the prefix itself doesn't fit within maxBytes, prefixing every continuation
        // piece would overflow the limit we're meant to guarantee. Degrade rather than
        // exceed: drop the prefix so content survives even though the box no longer does.
        val prefixBytes = continuationPrefix.utf8Size()
        val effectivePrefix = if (prefixBytes >= maxBytes) "" else continuationPrefix
        val effectivePrefixBytes = effectivePrefix.utf8Size()
        val pieces = mutableListOf<String>()
        var start = 0
        var budget = maxBytes // the first piece carries no prefix
        while (start < line.length) {
            val end = endIndexWithinBudget(line, start, budget)
            pieces += if (start == 0) line.substring(start, end) else effectivePrefix + line.substring(start, end)
            start = end
            budget = (maxBytes - effectivePrefixBytes).coerceAtLeast(1)
        }
        return pieces
    }

    /**
     * Largest end index whose `[start, end)` slice fits [budget] UTF-8 bytes, always
     * advancing at least one code point: a budget smaller than a single character
     * degrades to slight overflow rather than an infinite loop.
     */
    private fun endIndexWithinBudget(line: String, start: Int, budget: Int): Int {
        var index = start
        var bytes = 0
        while (index < line.length) {
            val codePoint = line.codePointAt(index)
            val codePointBytes = utf8Bytes(codePoint)
            if (bytes + codePointBytes > budget) {
                return if (index > start) index else index + Character.charCount(codePoint)
            }
            bytes += codePointBytes
            index += Character.charCount(codePoint)
        }
        return index
    }

    private fun String.utf8Size(): Int {
        var index = 0
        var bytes = 0
        while (index < length) {
            val codePoint = codePointAt(index)
            bytes += utf8Bytes(codePoint)
            index += Character.charCount(codePoint)
        }
        return bytes
    }

    private fun utf8Bytes(codePoint: Int): Int = when {
        codePoint < UTF8_ONE_BYTE_LIMIT -> 1
        codePoint < UTF8_TWO_BYTE_LIMIT -> 2
        codePoint < UTF8_THREE_BYTE_LIMIT -> UTF8_THREE_BYTES
        else -> UTF8_FOUR_BYTES
    }
}
