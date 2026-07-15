package io.github.marioponceg.quill.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogcatChunkerTest {

    @Test
    fun `short lines join into a single chunk`() {
        assertEquals(
            listOf("a\nb\nc"),
            LogcatChunker.chunk(listOf("a", "b", "c"), continuationPrefix = "| "),
        )
    }

    @Test
    fun `starts a new chunk when the next line would exceed the limit`() {
        val chunks = LogcatChunker.chunk(
            listOf("aaaa", "bbbb", "cccc"),
            continuationPrefix = "| ",
            maxBytes = 9,
        )
        assertEquals(listOf("aaaa\nbbbb", "cccc"), chunks)
    }

    @Test
    fun `splits an over-long line and prefixes continuation pieces`() {
        val chunks = LogcatChunker.chunk(
            listOf("x".repeat(25)),
            continuationPrefix = "| ",
            maxBytes = 10,
        )
        assertEquals(
            listOf("x".repeat(10), "| " + "x".repeat(8), "| " + "x".repeat(7)),
            chunks,
        )
    }

    @Test
    fun `no chunk ever exceeds the limit`() {
        val lines = List(50) { "line-$it-" + "y".repeat(it * 7) }
        val chunks = LogcatChunker.chunk(lines, continuationPrefix = "| ", maxBytes = 100)
        assertTrue(chunks.all { it.toByteArray(Charsets.UTF_8).size <= 100 })
        assertEquals(
            lines.joinToString("").filterNot { it == '\n' },
            chunks.joinToString("") { it.replace("\n", "").replace("| ", "") },
        )
    }

    @Test
    fun `default limit is logcat's 4000`() {
        assertEquals(4000, LogcatChunker.MAX_MESSAGE_BYTES)
    }

    @Test
    fun `degrades to an empty prefix when the prefix itself would overflow the limit`() {
        val chunks = LogcatChunker.chunk(
            listOf("x".repeat(30)),
            continuationPrefix = "-".repeat(25),
            maxBytes = 10,
        )
        assertTrue(chunks.all { it.toByteArray(Charsets.UTF_8).size <= 10 })
        assertEquals(
            "x".repeat(30),
            chunks.joinToString("") { it.replace("-", "") },
        )
    }

    @Test
    fun `budget counts UTF-8 bytes, not chars`() {
        // "é" is 2 bytes: four of them fit an 8-byte budget, the fifth starts a new chunk.
        val chunks = LogcatChunker.chunk(listOf("éééé", "é"), continuationPrefix = "", maxBytes = 8)
        assertEquals(listOf("éééé", "é"), chunks)
    }

    @Test
    fun `over-long multibyte lines split on code point boundaries`() {
        // "😀" is one code point, 2 chars, 4 UTF-8 bytes. Budget 9 fits two per piece
        // (8 bytes) — never five half-surrogates.
        val chunks = LogcatChunker.chunk(listOf("😀".repeat(5)), continuationPrefix = "", maxBytes = 9)
        assertEquals(listOf("😀😀", "😀😀", "😀"), chunks)
        assertTrue(chunks.all { piece -> piece.toByteArray(Charsets.UTF_8).size <= 9 })
    }

    @Test
    fun `a single code point larger than the budget degrades instead of looping`() {
        val chunks = LogcatChunker.chunk(listOf("😀😀"), continuationPrefix = "", maxBytes = 3)
        assertEquals(listOf("😀", "😀"), chunks)
    }

    @Test
    fun `an empty single line survives as one empty chunk`() {
        assertEquals(listOf(""), LogcatChunker.chunk(listOf(""), continuationPrefix = "│ "))
    }

    @Test
    fun `empty input produces no chunks`() {
        assertEquals(emptyList(), LogcatChunker.chunk(emptyList(), continuationPrefix = "│ "))
    }
}
