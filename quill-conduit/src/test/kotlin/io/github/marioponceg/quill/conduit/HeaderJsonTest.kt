package io.github.marioponceg.quill.conduit

import io.github.marioponceg.conduit.http.Headers
import kotlin.test.Test
import kotlin.test.assertEquals

class HeaderJsonTest {

    @Test
    fun `renders headers as a compact json object`() {
        val headers = Headers.of("Accept" to "application/json", "X-Trace" to "abc")

        assertEquals(
            """{"Accept":"application/json","X-Trace":"abc"}""",
            headers.toRedactedJson(redact = emptySet()),
        )
    }

    @Test
    fun `empty headers render as an empty object`() {
        assertEquals("{}", Headers.of().toRedactedJson(redact = emptySet()))
    }

    @Test
    fun `repeated names render as an array in entry order`() {
        val headers = Headers.of("Set-Cookie" to "a=1", "Set-Cookie" to "b=2")

        assertEquals(
            """{"Set-Cookie":["a=1","b=2"]}""",
            headers.toRedactedJson(redact = emptySet()),
        )
    }

    @Test
    fun `redacts configured names case-insensitively`() {
        val headers = Headers.of("authorization" to "Bearer secret", "Accept" to "*/*")

        assertEquals(
            """{"authorization":"██","Accept":"*/*"}""",
            headers.toRedactedJson(redact = setOf("Authorization")),
        )
    }

    @Test
    fun `escapes quotes backslashes and control characters in values`() {
        val headers = Headers.of("X-Weird" to "say \"hi\"\\\n\tdone")

        assertEquals(
            """{"X-Weird":"say \"hi\"\\\n\tdone"}""",
            headers.toRedactedJson(redact = emptySet()),
        )
    }
}
