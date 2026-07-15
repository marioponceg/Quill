package io.github.marioponceg.quill.android

import io.github.marioponceg.quill.QuillEvent
import io.github.marioponceg.quill.QuillLevel
import io.github.marioponceg.quill.QuillValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogcatSinkTest {

    private class RecordingPrinter {
        val calls = mutableListOf<Triple<Int, String, String>>()

        fun print(priority: Int, tag: String, message: String) {
            calls += Triple(priority, tag, message)
        }
    }

    private fun event(
        level: QuillLevel = QuillLevel.Info,
        origin: String = "AuthRepository",
        name: String = "user_login",
        fields: Map<String, QuillValue> = emptyMap(),
    ): QuillEvent = QuillEvent(0L, level, origin, name, fields, null)

    @Test
    fun `tags messages with the prefix and origin`() {
        val printer = RecordingPrinter()
        LogcatSink(tagPrefix = "Quill", minLevel = QuillLevel.Verbose, boxed = true, printer = printer::print)
            .write(event())

        assertEquals("Quill.AuthRepository", printer.calls.single().second)
    }

    @Test
    fun `null prefix uses the bare origin`() {
        val printer = RecordingPrinter()
        LogcatSink(tagPrefix = null, minLevel = QuillLevel.Verbose, boxed = true, printer = printer::print)
            .write(event())

        assertEquals("AuthRepository", printer.calls.single().second)
    }

    @Test
    fun `maps the event level to the android priority`() {
        val printer = RecordingPrinter()
        LogcatSink(tagPrefix = "Quill", minLevel = QuillLevel.Verbose, boxed = true, printer = printer::print)
            .write(event(level = QuillLevel.Error))

        assertEquals(android.util.Log.ERROR, printer.calls.single().first)
    }

    @Test
    fun `per-sink minLevel filters below it`() {
        val printer = RecordingPrinter()
        val sink = LogcatSink(tagPrefix = "Quill", minLevel = QuillLevel.Warn, boxed = true, printer = printer::print)

        sink.write(event(level = QuillLevel.Info))
        sink.write(event(level = QuillLevel.Warn, name = "kept"))

        assertEquals(1, printer.calls.size)
        assertTrue(printer.calls.single().third.contains("kept"))
    }

    @Test
    fun `writes the boxed content`() {
        val printer = RecordingPrinter()
        LogcatSink(tagPrefix = "Quill", minLevel = QuillLevel.Verbose, boxed = true, printer = printer::print)
            .write(event(fields = linkedMapOf("userId" to QuillValue.Number(42))))

        val message = printer.calls.single().third
        assertTrue(message.startsWith("┌─ user_login "))
        assertTrue(message.contains("\n│ userId: 42\n"))
        assertTrue(message.endsWith("└" + "─".repeat(59)))
    }

    @Test
    fun `large events split across multiple calls of at most 4000 UTF-8 bytes`() {
        val printer = RecordingPrinter()
        val huge = QuillValue.Structured("x".repeat(12000))
        LogcatSink(tagPrefix = "Quill", minLevel = QuillLevel.Verbose, boxed = true, printer = printer::print)
            .write(event(fields = linkedMapOf("blob" to huge)))

        assertTrue(printer.calls.size > 1)
        assertTrue(printer.calls.all { it.third.toByteArray(Charsets.UTF_8).size <= LogcatChunker.MAX_MESSAGE_BYTES })
        assertTrue(printer.calls.drop(1).all { it.third.startsWith("│ ") })
    }
}
