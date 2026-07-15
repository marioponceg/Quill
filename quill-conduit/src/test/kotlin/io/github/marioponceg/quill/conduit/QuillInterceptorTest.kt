package io.github.marioponceg.quill.conduit

import io.github.marioponceg.conduit.engine.ConduitEngine
import io.github.marioponceg.conduit.http.Headers
import io.github.marioponceg.conduit.http.HttpMethod
import io.github.marioponceg.conduit.http.HttpRequest
import io.github.marioponceg.conduit.http.HttpResponse
import io.github.marioponceg.conduit.interceptor.InterceptorPipeline
import io.github.marioponceg.quill.QuillEvent
import io.github.marioponceg.quill.QuillLevel
import io.github.marioponceg.quill.QuillLogger
import io.github.marioponceg.quill.QuillValue
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource
import kotlin.time.TimeSource

class QuillInterceptorTest {

    private val sink = FakeSink()
    private val logger = QuillLogger("Http", sinks = listOf(sink))

    private fun pipeline(
        interceptor: QuillInterceptor,
        engine: ConduitEngine,
    ) = InterceptorPipeline(listOf(interceptor), engine)

    private fun interceptor(
        level: BodyLevel = BodyLevel.Basic,
        redactHeaders: Set<String> = setOf("Authorization"),
        timeSource: TimeSource = TestTimeSource(),
    ) = QuillInterceptor(
        level = level,
        redactHeaders = redactHeaders,
        logger = logger,
        timeSource = timeSource,
    )

    private val request = HttpRequest(
        url = "https://api.example.com/users",
        method = HttpMethod.POST,
        headers = Headers.of("Accept" to "application/json"),
        body = """{"name":"mario"}""".toByteArray(),
    )

    private suspend fun eventsFor(
        requestBody: ByteArray? = null,
        responseBody: ByteArray? = null,
        level: BodyLevel,
        maxBodyBytes: Long = 65_536L,
    ): List<QuillEvent> {
        val engine = ConduitEngine { HttpResponse(code = 200, body = responseBody) }
        val interceptor = QuillInterceptor(
            level = level,
            maxBodyBytes = maxBodyBytes,
            logger = logger,
        )

        pipeline(interceptor, engine).execute(
            requestBody?.let { request.copy(body = it) } ?: request,
        )

        return sink.events
    }

    @Test
    fun `emits http_request and http_response correlated by requestId`() = runTest {
        val engine = ConduitEngine { HttpResponse(code = 200) }

        pipeline(interceptor(), engine).execute(request)

        val (req, res) = sink.events.also { assertEquals(2, it.size) }
        assertEquals("http_request", req.name)
        assertEquals("http_response", res.name)
        assertEquals(QuillLevel.Info, req.level)
        assertEquals(QuillLevel.Info, res.level)
        assertEquals("Http", req.origin)
        val requestId = (req.fields.getValue("requestId") as QuillValue.Text).value
        assertEquals(8, requestId.length)
        assertTrue(requestId.all { it in "0123456789abcdef" })
        assertEquals(requestId, (res.fields.getValue("requestId") as QuillValue.Text).value)
    }

    @Test
    fun `Basic logs method url code and duration but no headers or body`() = runTest {
        val time = TestTimeSource()
        val engine = ConduitEngine {
            time += 42.milliseconds
            HttpResponse(code = 201)
        }

        pipeline(interceptor(timeSource = time), engine).execute(request)

        val (req, res) = sink.events
        assertEquals(QuillValue.Text("POST"), req.fields["method"])
        assertEquals(QuillValue.Text("https://api.example.com/users"), req.fields["url"])
        assertEquals(QuillValue.Number(201), res.fields["code"])
        assertEquals(QuillValue.Number(42L), res.fields["durationMs"])
        assertFalse("headers" in req.fields)
        assertFalse("body" in req.fields)
        assertFalse("headers" in res.fields)
        assertFalse("body" in res.fields)
    }

    @Test
    fun `Headers adds redacted request and response headers`() = runTest {
        val authed = request.copy(
            headers = Headers.of("Authorization" to "Bearer secret", "Accept" to "*/*"),
        )
        val engine = ConduitEngine {
            HttpResponse(code = 200, headers = Headers.of("Content-Type" to "application/json"))
        }

        pipeline(interceptor(level = BodyLevel.Headers), engine).execute(authed)

        val (req, res) = sink.events
        assertEquals(
            QuillValue.Structured("""{"Authorization":"██","Accept":"*/*"}"""),
            req.fields["headers"],
        )
        assertEquals(
            QuillValue.Structured("""{"Content-Type":"application/json"}"""),
            res.fields["headers"],
        )
        assertFalse("body" in req.fields)
        assertFalse("body" in res.fields)
    }

    @Test
    fun `Body adds utf-8 decoded bodies and omits null bodies`() = runTest {
        val engine = ConduitEngine {
            HttpResponse(code = 200, body = """{"id":7}""".toByteArray())
        }

        pipeline(interceptor(level = BodyLevel.Body), engine).execute(request)
        pipeline(interceptor(level = BodyLevel.Body), engine)
            .execute(request.copy(body = null))

        val (req, res) = sink.events
        assertEquals(QuillValue.Structured("""{"name":"mario"}"""), req.fields["body"])
        assertEquals(QuillValue.Structured("""{"id":7}"""), res.fields["body"])
        assertFalse("body" in sink.events[2].fields)
    }

    @Test
    fun `None is a pass-through emitting no events`() = runTest {
        val engine = ConduitEngine { HttpResponse(code = 200) }

        val response = pipeline(interceptor(level = BodyLevel.None), engine).execute(request)

        assertEquals(200, response.code)
        assertTrue(sink.events.isEmpty())
    }

    @Test
    fun `http_request is emitted before the engine runs`() = runTest {
        var eventsAtEngineTime = -1
        val engine = ConduitEngine {
            eventsAtEngineTime = sink.events.size
            HttpResponse(code = 200)
        }

        pipeline(interceptor(), engine).execute(request)

        assertEquals(1, eventsAtEngineTime)
    }

    @Test
    fun `returns the response unchanged`() = runTest {
        val body = "payload".toByteArray()
        val engine = ConduitEngine { HttpResponse(code = 200, body = body) }

        val response = pipeline(interceptor(), engine).execute(request)

        assertEquals(200, response.code)
        assertEquals(body, response.body)
    }

    @Test
    fun `non-2xx responses emit http_failure with kind http`() = runTest {
        val time = TestTimeSource()
        val engine = ConduitEngine {
            time += 10.milliseconds
            HttpResponse(code = 404, body = "not here".toByteArray())
        }

        val response = pipeline(interceptor(timeSource = time), engine).execute(request)

        assertEquals(404, response.code)
        val failure = sink.events.last()
        assertEquals("http_failure", failure.name)
        assertEquals(QuillLevel.Warn, failure.level)
        assertEquals(QuillValue.Text("http"), failure.fields["kind"])
        assertEquals(QuillValue.Number(404), failure.fields["code"])
        assertEquals(QuillValue.Number(10L), failure.fields["durationMs"])
        val requestId = (sink.events.first().fields.getValue("requestId") as QuillValue.Text).value
        assertEquals(QuillValue.Text(requestId), failure.fields["requestId"])
    }

    @Test
    fun `IOException emits http_failure with kind network and rethrows`() = runTest {
        val boom = IOException("connection reset")
        val engine = ConduitEngine { throw boom }

        val thrown = assertFailsWith<IOException> {
            pipeline(interceptor(), engine).execute(request)
        }

        assertSame(boom, thrown)
        val failure = sink.events.last()
        assertEquals("http_failure", failure.name)
        assertEquals(QuillLevel.Warn, failure.level)
        assertEquals(QuillValue.Text("network"), failure.fields["kind"])
        assertSame(boom, failure.throwable)
        assertTrue("durationMs" in failure.fields)
        assertFalse("code" in failure.fields)
    }

    @Test
    fun `failure events carry headers and body at Body level`() = runTest {
        val engine = ConduitEngine {
            HttpResponse(
                code = 500,
                headers = Headers.of("Content-Type" to "text/plain"),
                body = "oops".toByteArray(),
            )
        }

        pipeline(interceptor(level = BodyLevel.Body), engine).execute(request)

        val failure = sink.events.last()
        assertEquals("http_failure", failure.name)
        assertEquals(
            QuillValue.Structured("""{"Content-Type":"text/plain"}"""),
            failure.fields["headers"],
        )
        assertEquals(QuillValue.Text("oops"), failure.fields["body"])
    }

    @Test
    fun `None also swallows failures silently`() = runTest {
        val engine = ConduitEngine { throw IOException("reset") }

        assertFailsWith<IOException> {
            pipeline(interceptor(level = BodyLevel.None), engine).execute(request)
        }

        assertTrue(sink.events.isEmpty())
    }

    @Test
    fun `binary bodies render a placeholder instead of mojibake`() = runTest {
        val body = byteArrayOf(0x00, -0x01, -0x02, 0x42) // 0xFF is never valid UTF-8
        val events = eventsFor(responseBody = body, level = BodyLevel.Body)
        assertEquals(
            "(binary body, 4 bytes)",
            (events.last().fields.getValue("body") as QuillValue.Text).value,
        )
    }

    @Test
    fun `oversized text bodies truncate at the cap with an omission suffix`() = runTest {
        val events = eventsFor(
            responseBody = "a".repeat(100).toByteArray(),
            level = BodyLevel.Body,
            maxBodyBytes = 64,
        )
        assertEquals(
            "a".repeat(64) + "… (+36 bytes)",
            (events.last().fields.getValue("body") as QuillValue.Text).value,
        )
    }

    @Test
    fun `a body exactly at the cap passes untouched`() = runTest {
        val events = eventsFor(
            responseBody = "b".repeat(64).toByteArray(),
            level = BodyLevel.Body,
            maxBodyBytes = 64,
        )
        assertEquals("b".repeat(64), (events.last().fields.getValue("body") as QuillValue.Text).value)
    }

    @Test
    fun `a cap that cuts a multibyte character keeps the clean prefix`() = runTest {
        // "é" is 2 bytes; a 5-byte cap lands mid-character on the third é.
        val events = eventsFor(
            responseBody = "ééé".toByteArray(),
            level = BodyLevel.Body,
            maxBodyBytes = 5,
        )
        assertEquals("éé… (+2 bytes)", (events.last().fields.getValue("body") as QuillValue.Text).value)
    }

    @Test
    fun `the cap applies to request bodies too`() = runTest {
        val events = eventsFor(
            requestBody = "c".repeat(100).toByteArray(),
            level = BodyLevel.Body,
            maxBodyBytes = 64,
        )
        assertEquals(
            "c".repeat(64) + "… (+36 bytes)",
            (events.first().fields.getValue("body") as QuillValue.Text).value,
        )
    }
}
