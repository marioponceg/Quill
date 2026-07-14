package io.github.marioponceg.quill

import kotlin.test.Test
import kotlin.test.assertEquals

class QuillBeautifierTest {

    @Test
    fun `beautifies JSON`() {
        assertEquals(
            """
            {
                "a": 1
            }
            """.trimIndent(),
            QuillBeautifier.beautify("""{"a":1}"""),
        )
    }

    @Test
    fun `beautifies data class toString`() {
        assertEquals(
            """
            Point(
                x = 1,
                y = 2
            )
            """.trimIndent(),
            QuillBeautifier.beautify("Point(x=1, y=2)"),
        )
    }

    @Test
    fun `returns anything else raw, never throws`() {
        assertEquals("just text", QuillBeautifier.beautify("just text"))
        assertEquals("{broken json", QuillBeautifier.beautify("{broken json"))
        assertEquals("", QuillBeautifier.beautify(""))
    }

    @Test
    fun `returns absurdly deep data class input raw instead of throwing`() {
        val deeplyNested = "A(".repeat(100_000) + ")".repeat(100_000)
        assertEquals(deeplyNested, QuillBeautifier.beautify(deeplyNested))
    }

    @Test
    fun `returns deep keyed nesting raw instead of throwing`() {
        val deeplyNested = "A(k=".repeat(100_000) + ")".repeat(100_000)
        assertEquals(deeplyNested, QuillBeautifier.beautify(deeplyNested))
    }

    @Test
    fun `compacts JSON to a single line`() {
        assertEquals(
            """{"id":42,"tags":["a","b"],"meta":{"active":true}}""",
            QuillBeautifier.compact(
                """
                {
                    "id": 42,
                    "tags": [ "a", "b" ],
                    "meta": { "active": true }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `compact preserves whitespace inside JSON strings`() {
        assertEquals(
            """{"note":"line one\nline two","pad":"  x  "}""",
            QuillBeautifier.compact("""{ "note": "line one\nline two", "pad": "  x  " }"""),
        )
    }

    @Test
    fun `compact collapses newlines in non-JSON input`() {
        assertEquals(
            "UserDto( id = 42, name = \"Mario\" )",
            QuillBeautifier.compact("UserDto(\n    id = 42,\n    name = \"Mario\"\n)"),
        )
    }

    @Test
    fun `compact returns single-line non-JSON input trimmed and otherwise untouched`() {
        assertEquals("plain value", QuillBeautifier.compact("  plain value "))
    }

    @Test
    fun `compact never throws on malformed JSON, degrades to collapsing`() {
        assertEquals("{ \"broken\": }", QuillBeautifier.compact("{ \"broken\":\n}"))
    }
}
