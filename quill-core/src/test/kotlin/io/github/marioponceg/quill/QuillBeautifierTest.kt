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
}
