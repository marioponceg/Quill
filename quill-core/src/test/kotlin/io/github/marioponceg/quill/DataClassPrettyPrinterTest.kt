package io.github.marioponceg.quill

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataClassPrettyPrinterTest {

    @Test
    fun `pretty prints a flat data class`() {
        val result = DataClassPrettyPrinter.prettyPrintOrNull(
            "UserDto(id=42, name=Mario, roles=[admin, editor])",
        )
        assertEquals(
            """
            UserDto(
                id = 42,
                name = Mario,
                roles = [admin, editor]
            )
            """.trimIndent(),
            result,
        )
    }

    @Test
    fun `expands nested data classes`() {
        val result = DataClassPrettyPrinter.prettyPrintOrNull(
            "UserDto(id=42, address=Address(street=Main St, city=Madrid))",
        )
        assertEquals(
            """
            UserDto(
                id = 42,
                address = Address(
                    street = Main St,
                    city = Madrid
                )
            )
            """.trimIndent(),
            result,
        )
    }

    @Test
    fun `does not split on commas inside brackets or parens`() {
        val result = DataClassPrettyPrinter.prettyPrintOrNull(
            "Order(items=[Pair(a, b), Pair(c, d)], total=10)",
        )
        assertEquals(
            """
            Order(
                items = [Pair(a, b), Pair(c, d)],
                total = 10
            )
            """.trimIndent(),
            result,
        )
    }

    @Test
    fun `keeps an empty data class compact`() {
        assertEquals("Empty()", DataClassPrettyPrinter.prettyPrintOrNull("Empty()"))
    }

    @Test
    fun `returns null for input that is not a data class toString`() {
        assertNull(DataClassPrettyPrinter.prettyPrintOrNull("plain sentence"))
        assertNull(DataClassPrettyPrinter.prettyPrintOrNull("(no name)"))
        assertNull(DataClassPrettyPrinter.prettyPrintOrNull("Broken(unbalanced"))
        assertNull(DataClassPrettyPrinter.prettyPrintOrNull("""{"json":true}"""))
    }
}
