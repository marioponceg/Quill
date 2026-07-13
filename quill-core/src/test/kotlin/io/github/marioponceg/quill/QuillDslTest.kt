package io.github.marioponceg.quill

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuillDslTest {

    @AfterTest
    fun tearDown() {
        Quill.reset()
    }

    @Test
    fun `logging before configuration is a silent no-op`() {
        val log = Quill.logger("Early")
        var evaluated = false

        log.error("too_soon") { evaluated = true }

        assertFalse(evaluated)
    }

    @Test
    fun `quill configures sinks and minLevel globally`() {
        val sink = FakeSink()
        quill {
            minLevel = QuillLevel.Info
            addSink(sink)
        }

        val log = Quill.logger("Repo")
        log.debug("filtered_out")
        log.info("kept")

        assertEquals(listOf("kept"), sink.events.map { it.name })
    }

    @Test
    fun `loggers created before configuration come alive afterwards`() {
        val log = Quill.logger("Early")
        val sink = FakeSink()

        log.info("dropped")
        quill { addSink(sink) }
        log.info("delivered")

        assertEquals(listOf("delivered"), sink.events.map { it.name })
    }

    @Test
    fun `reified logger uses the simple class name as origin`() {
        val sink = FakeSink()
        quill { addSink(sink) }

        val log = Quill.logger<QuillDslTest>()
        log.info("named")

        assertEquals("QuillDslTest", sink.single().origin)
    }

    @Test
    fun `reconfiguring replaces the previous sinks`() {
        val first = FakeSink()
        val second = FakeSink()
        quill { addSink(first) }
        quill { addSink(second) }

        Quill.logger("T").info("after_reconfigure")

        assertTrue(first.events.isEmpty())
        assertEquals(listOf("after_reconfigure"), second.events.map { it.name })
    }
}
