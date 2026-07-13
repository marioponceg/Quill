package io.github.marioponceg.quill

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonPrettyPrinterTest {

    @Test
    fun `pretty prints a flat object`() {
        val result = JsonPrettyPrinter.prettyPrintOrNull("""{"id":42,"name":"Mario","admin":true}""")
        assertEquals(
            """
            {
                "id": 42,
                "name": "Mario",
                "admin": true
            }
            """.trimIndent(),
            result,
        )
    }

    @Test
    fun `pretty prints nested objects and arrays`() {
        val result = JsonPrettyPrinter.prettyPrintOrNull(
            """{"user":{"id":42,"roles":["admin","editor"]},"active":null}""",
        )
        assertEquals(
            """
            {
                "user": {
                    "id": 42,
                    "roles": [
                        "admin",
                        "editor"
                    ]
                },
                "active": null
            }
            """.trimIndent(),
            result,
        )
    }

    @Test
    fun `keeps empty object and array compact`() {
        assertEquals(
            """
            {
                "a": {},
                "b": []
            }
            """.trimIndent(),
            JsonPrettyPrinter.prettyPrintOrNull("""{"a":{},"b":[]}"""),
        )
    }

    @Test
    fun `preserves escaped quotes inside strings`() {
        assertEquals(
            """
            {
                "msg": "say \"hi\", ok"
            }
            """.trimIndent(),
            JsonPrettyPrinter.prettyPrintOrNull("""{"msg":"say \"hi\", ok"}"""),
        )
    }

    @Test
    fun `pretty prints a top level array`() {
        assertEquals(
            """
            [
                1,
                2.5,
                false
            ]
            """.trimIndent(),
            JsonPrettyPrinter.prettyPrintOrNull("[1,2.5,false]"),
        )
    }

    @Test
    fun `returns null for non JSON input`() {
        assertNull(JsonPrettyPrinter.prettyPrintOrNull("plain sentence"))
        assertNull(JsonPrettyPrinter.prettyPrintOrNull(""))
        assertNull(JsonPrettyPrinter.prettyPrintOrNull("42"))
    }

    @Test
    fun `returns null for malformed JSON`() {
        assertNull(JsonPrettyPrinter.prettyPrintOrNull("""{"a":1"""))          // unterminated
        assertNull(JsonPrettyPrinter.prettyPrintOrNull("""{"a" 1}"""))         // missing colon
        assertNull(JsonPrettyPrinter.prettyPrintOrNull("""{"a":oops}"""))      // bad literal
        assertNull(JsonPrettyPrinter.prettyPrintOrNull("""{"a":1} trailing"""))
        assertNull(JsonPrettyPrinter.prettyPrintOrNull("""{a:1}"""))           // unquoted key
    }
}
