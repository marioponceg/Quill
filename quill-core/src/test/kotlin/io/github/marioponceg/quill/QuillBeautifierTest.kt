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
}
