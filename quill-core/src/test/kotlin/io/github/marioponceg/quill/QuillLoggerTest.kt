package io.github.marioponceg.quill

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class QuillLoggerTest {

    @Test
    fun `writes an event with origin, name, fields and level`() {
        val sink = FakeSink()
        val log = QuillLogger("AuthRepository", sinks = listOf(sink))

        log.info("user_login") {
            "userId" to 42
            "method" to "oauth"
        }

        val event = sink.single()
        assertEquals(QuillLevel.Info, event.level)
        assertEquals("AuthRepository", event.origin)
        assertEquals("user_login", event.name)
        assertEquals(QuillValue.Number(42), event.fields["userId"])
        assertEquals(QuillValue.Text("oauth"), event.fields["method"])
        assertEquals(null, event.throwable)
        assertTrue(event.timestamp > 0)
    }

    @Test
    fun `each level method emits its level`() {
        val sink = FakeSink()
        val log = QuillLogger("T", sinks = listOf(sink))

        log.verbose("v")
        log.debug("d")
        log.info("i")
        log.warn("w")
        log.error("e")

        assertEquals(
            listOf(QuillLevel.Verbose, QuillLevel.Debug, QuillLevel.Info, QuillLevel.Warn, QuillLevel.Error),
            sink.events.map { it.level },
        )
    }

    @Test
    fun `attaches the throwable`() {
        val sink = FakeSink()
        val log = QuillLogger("T", sinks = listOf(sink))
        val boom = IllegalStateException("boom")

        log.error("sync_failed", throwable = boom) { "retries" to 3 }

        assertSame(boom, sink.single().throwable)
    }

    @Test
    fun `filtered levels emit nothing and never evaluate the field lambda`() {
        val sink = FakeSink()
        val log = QuillLogger("T", sinks = listOf(sink), minLevel = QuillLevel.Warn)
        var evaluated = false

        log.debug("expensive") {
            evaluated = true
            "cost" to "high"
        }

        assertTrue(sink.events.isEmpty())
        assertFalse(evaluated)
    }

    @Test
    fun `no sinks means the field lambda is never evaluated`() {
        val log = QuillLogger("T", sinks = emptyList())
        var evaluated = false

        log.error("anything") { evaluated = true }

        assertFalse(evaluated)
    }

    @Test
    fun `a throwing sink does not prevent later sinks from receiving the event`() {
        val throwing = object : QuillSink {
            // Deliberately generic: this test proves the logger guards against *any* sink
            // failure, not one specific exception type.
            @Suppress("TooGenericExceptionThrown")
            override fun write(event: QuillEvent) = throw RuntimeException("sink bug")
        }
        val healthy = FakeSink()
        val log = QuillLogger("T", sinks = listOf(throwing, healthy))

        log.info("resilient")

        assertEquals("resilient", healthy.single().name)
    }
}
