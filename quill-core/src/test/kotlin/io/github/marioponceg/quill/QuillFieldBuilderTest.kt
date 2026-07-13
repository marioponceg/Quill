package io.github.marioponceg.quill

import kotlin.test.Test
import kotlin.test.assertEquals

class QuillFieldBuilderTest {

    private fun fieldsOf(block: QuillFieldBuilder.() -> Unit): Map<String, QuillValue> =
        QuillFieldBuilder().apply(block).build()

    @Test
    fun `converts primitives to typed values`() {
        val fields = fieldsOf {
            "count" to 42
            "ratio" to 0.5
            "enabled" to true
            "name" to "Mario"
            "missing" to null
        }
        assertEquals(QuillValue.Number(42), fields["count"])
        assertEquals(QuillValue.Number(0.5), fields["ratio"])
        assertEquals(QuillValue.Bool(true), fields["enabled"])
        assertEquals(QuillValue.Text("Mario"), fields["name"])
        assertEquals(QuillValue.Null, fields["missing"])
    }

    @Test
    fun `strings that parse as JSON become Structured with the raw value`() {
        val fields = fieldsOf { "payload" to """{"a":1}""" }
        assertEquals(QuillValue.Structured("""{"a":1}"""), fields["payload"])
    }

    @Test
    fun `arbitrary objects become Structured via toString`() {
        data class Point(val x: Int, val y: Int)
        val fields = fieldsOf { "point" to Point(1, 2) }
        assertEquals(QuillValue.Structured("Point(x=1, y=2)"), fields["point"])
    }

    @Test
    fun `preserves insertion order and last write wins per key`() {
        val fields = fieldsOf {
            "a" to 1
            "b" to 2
            "a" to 3
        }
        assertEquals(listOf("a", "b"), fields.keys.toList())
        assertEquals(QuillValue.Number(3), fields["a"])
    }
}
