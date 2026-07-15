package io.github.marioponceg.quill.android

import io.github.marioponceg.quill.QuillEvent
import io.github.marioponceg.quill.QuillLevel
import io.github.marioponceg.quill.QuillValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun `throwable renders header and indented stack inside the box`() {
        val boom = java.io.IOException("timeout after 30s").apply {
            stackTrace = arrayOf(
                StackTraceElement("SyncWorker", "sync", "SyncWorker.kt", 87),
                StackTraceElement("SyncWorker", "run", "SyncWorker.kt", 12),
            )
        }
        val lines = LogcatFormatter().format(
            event(
                name = "sync_failed",
                fields = linkedMapOf("retries" to QuillValue.Number(3)),
                throwable = boom,
            ),
        )
        assertEquals(
            listOf(
                "┌─ sync_failed " + "─".repeat(45),
                "│ retries: 3",
                "│ ▼ IOException: timeout after 30s",
                "│     at SyncWorker.sync(SyncWorker.kt:87)",
                "│     at SyncWorker.run(SyncWorker.kt:12)",
                "└" + "─".repeat(59),
            ),
            lines,
        )
    }

    @Test
    fun `throwable without message renders only the class name`() {
        val boom = IllegalStateException().apply { stackTrace = emptyArray() }
        val lines = LogcatFormatter().format(event(throwable = boom))
        assertEquals("│ ▼ IllegalStateException", lines[1])
    }

    @Test
    fun `caused-by chains are preserved with the same indent`() {
        val cause = java.io.IOException("socket closed").apply { stackTrace = emptyArray() }
        val boom = RuntimeException("sync failed", cause).apply {
            stackTrace = arrayOf(StackTraceElement("SyncWorker", "sync", "SyncWorker.kt", 87))
        }
        val lines = LogcatFormatter().format(event(throwable = boom))
        assertEquals(
            listOf(
                "│ ▼ RuntimeException: sync failed",
                "│     at SyncWorker.sync(SyncWorker.kt:87)",
                "│     Caused by: java.io.IOException: socket closed",
            ),
            lines.subList(1, 4),
        )
    }

    @Test
    fun `flat mode renders a single line with key=value pairs`() {
        val lines = LogcatFormatter(boxed = false).format(
            event(
                fields = linkedMapOf(
                    "userId" to QuillValue.Number(42),
                    "method" to QuillValue.Text("oauth"),
                ),
            ),
        )
        assertEquals(listOf("user_login  userId=42 method=\"oauth\""), lines)
    }

    @Test
    fun `flat mode compacts structured values and appends the throwable header`() {
        val boom = java.io.IOException("timeout").apply { stackTrace = emptyArray() }
        val lines = LogcatFormatter(boxed = false).format(
            event(
                name = "sync_failed",
                fields = linkedMapOf("payload" to QuillValue.Structured("""{"a":1}""")),
                throwable = boom,
            ),
        )
        assertEquals(listOf("""sync_failed  payload={"a":1}  ▼ IOException: timeout"""), lines)
    }

    @Test
    fun `flat structured values with server-side newlines compact to one line`() {
        val line = LogcatFormatter(boxed = false).format(
            event(fields = mapOf("body" to QuillValue.Structured("{\n  \"id\": 1,\n  \"ok\": true\n}"))),
        ).single()
        assertEquals("""body={"id":1,"ok":true}""", line.substringAfter("  "))
    }

    @Test
    fun `flat non-JSON structured values collapse newlines`() {
        val line = LogcatFormatter(boxed = false).format(
            event(fields = mapOf("dto" to QuillValue.Structured("UserDto(\n    id = 1\n)"))),
        ).single()
        assertEquals("dto=UserDto( id = 1 )", line.substringAfter("  "))
    }

    @Test
    fun `flat mode with no fields is just the event name`() {
        assertEquals(listOf("app_start"), LogcatFormatter(boxed = false).format(event(name = "app_start")))
    }

    @Test
    fun `text values escape quotes, backslashes and control characters`() {
        val lines = LogcatFormatter(boxed = true).format(
            event(fields = mapOf("q" to QuillValue.Text("say \"hi\"\\now\nnext"))),
        )
        assertEquals("""│ q: "say \"hi\"\\now\nnext"""", lines[1])
    }

    @Test
    fun `event names with control characters render escaped in the box header`() {
        val lines = LogcatFormatter(boxed = true).format(event(name = "user\nlogin"))
        assertTrue(lines.first().startsWith("┌─ user\\nlogin "))
    }

    @Test
    fun `event names with control characters render escaped in flat mode`() {
        val line = LogcatFormatter(boxed = false).format(event(name = "user\nlogin")).single()
        assertTrue(line.startsWith("user\\nlogin"))
    }

    @Test
    fun `flat text values stay on one line`() {
        val line = LogcatFormatter(boxed = false).format(
            event(fields = mapOf("msg" to QuillValue.Text("a\nb"))),
        ).single()
        assertEquals("""msg="a\nb"""", line.substringAfter("  "))
    }
}
