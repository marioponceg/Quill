package io.github.marioponceg.quill.android

import io.github.marioponceg.quill.QuillEvent
import io.github.marioponceg.quill.QuillLevel
import io.github.marioponceg.quill.QuillValue
import kotlin.test.Test
import kotlin.test.assertEquals

class LogcatFormatterTest {

    private fun event(
        name: String = "user_login",
        fields: Map<String, QuillValue> = emptyMap(),
        throwable: Throwable? = null,
    ): QuillEvent = QuillEvent(
        timestamp = 0L,
        level = QuillLevel.Info,
        origin = "AuthRepository",
        name = name,
        fields = fields,
        throwable = throwable,
    )

    @Test
    fun `boxed event renders header, quoted and bare fields, and footer`() {
        val lines = LogcatFormatter().format(
            event(
                fields = linkedMapOf(
                    "userId" to QuillValue.Number(42),
                    "method" to QuillValue.Text("oauth"),
                    "retry" to QuillValue.Bool(false),
                    "session" to QuillValue.Null,
                ),
            ),
        )
        assertEquals(
            listOf(
                "┌─ user_login " + "─".repeat(46),
                "│ userId: 42",
                "│ method: \"oauth\"",
                "│ retry: false",
                "│ session: null",
                "└" + "─".repeat(59),
            ),
            lines,
        )
    }

    @Test
    fun `boxed event with no fields is header and footer only`() {
        val lines = LogcatFormatter().format(event(name = "app_start"))
        assertEquals(
            listOf(
                "┌─ app_start " + "─".repeat(47),
                "└" + "─".repeat(59),
            ),
            lines,
        )
    }

    @Test
    fun `long event names keep a minimum of three trailing dashes`() {
        val longName = "n".repeat(70)
        val lines = LogcatFormatter().format(event(name = longName))
        assertEquals("┌─ $longName ───", lines.first())
    }

    @Test
    fun `structured fields are beautified with box-prefixed continuation lines`() {
        val lines = LogcatFormatter().format(
            event(
                name = "sync_failed",
                fields = linkedMapOf(
                    "retries" to QuillValue.Number(3),
                    "payload" to QuillValue.Structured("UserDto(id=42, name=Mario)"),
                ),
            ),
        )
        assertEquals(
            listOf(
                "┌─ sync_failed " + "─".repeat(45),
                "│ retries: 3",
                "│ payload: UserDto(",
                "│     id = 42,",
                "│     name = Mario",
                "│ )",
                "└" + "─".repeat(59),
            ),
            lines,
        )
    }

    @Test
    fun `structured JSON fields are pretty printed inside the box`() {
        val lines = LogcatFormatter().format(
            event(fields = linkedMapOf("body" to QuillValue.Structured("""{"ok":true}"""))),
        )
        assertEquals(
            listOf(
                "┌─ user_login " + "─".repeat(46),
                "│ body: {",
                "│     \"ok\": true",
                "│ }",
                "└" + "─".repeat(59),
            ),
            lines,
        )
    }
}
