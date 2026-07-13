package io.github.marioponceg.quill.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogcatChunkerTest {

    @Test
    fun `short lines join into a single chunk`() {
        assertEquals(
            listOf("a\nb\nc"),
            LogcatChunker.chunk(listOf("a", "b", "c"), continuationPrefix = "│ "),
        )
    }

    @Test
    fun `starts a new chunk when the next line would exceed the limit`() {
        val chunks = LogcatChunker.chunk(
            listOf("aaaa", "bbbb", "cccc"),
            continuationPrefix = "│ ",
            maxLength = 9,
        )
        assertEquals(listOf("aaaa\nbbbb", "cccc"), chunks)
    }

    @Test
    fun `splits an over-long line and prefixes continuation pieces`() {
        val chunks = LogcatChunker.chunk(
            listOf("x".repeat(25)),
            continuationPrefix = "│ ",
            maxLength = 10,
        )
        assertEquals(
            listOf("x".repeat(10), "│ " + "x".repeat(8), "│ " + "x".repeat(7)),
            chunks,
        )
    }

    @Test
    fun `no chunk ever exceeds the limit`() {
        val lines = List(50) { "line-$it-" + "y".repeat(it * 7) }
        val chunks = LogcatChunker.chunk(lines, continuationPrefix = "│ ", maxLength = 100)
        assertTrue(chunks.all { it.length <= 100 })
        assertEquals(
            lines.joinToString("").filterNot { it == '\n' },
            chunks.joinToString("") { it.replace("\n", "").replace("│ ", "") },
        )
    }

    @Test
    fun `default limit is logcat's 4000`() {
        assertEquals(4000, LogcatChunker.MAX_MESSAGE_LENGTH)
    }
}
