package io.github.marioponceg.quill.conduit

import io.github.marioponceg.conduit.http.Headers
import io.github.marioponceg.conduit.http.HttpRequest
import io.github.marioponceg.conduit.http.HttpResponse
import io.github.marioponceg.conduit.interceptor.ConduitInterceptor
import io.github.marioponceg.quill.Quill
import io.github.marioponceg.quill.QuillFieldBuilder
import io.github.marioponceg.quill.QuillLogger
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import kotlin.random.Random
import kotlin.time.TimeSource

/**
 * A Conduit interceptor that logs every HTTP exchange as structured Quill events:
 * `http_request` on the way out and `http_response` on the way back, correlated by a
 * short `requestId` field. [level] controls verbosity like OkHttp's logging
 * interceptor; header values named in [redactHeaders] (case-insensitive,
 * `Authorization` by default) are replaced with `██` so tokens never reach a sink.
 *
 * Interceptors observe the raw engine contract, before Conduit maps outcomes to
 * `ConduitResult` — so serialization failures (which happen after the pipeline)
 * are not observable and produce no event.
 *
 * Bodies are rendered only when they decode as UTF-8 — binary payloads log a size
 * placeholder — and at most [maxBodyBytes] bytes are rendered, with the omitted
 * remainder noted.
 *
 * Placement relative to retry interceptors decides what you observe: registered
 * *after* a `RetryInterceptor` in the list, this interceptor sits closer to the
 * engine and logs one request/response pair per attempt; registered *before* it,
 * it logs one pair per logical call, with `durationMs` spanning all attempts.
 *
 * ```
 * val client = conduit {
 *     engine = OkHttpEngine()
 *     interceptors += QuillInterceptor(level = BodyLevel.Body)
 * }
 * ```
 */
public class QuillInterceptor(
    private val level: BodyLevel = BodyLevel.Basic,
    private val redactHeaders: Set<String> = setOf("Authorization"),
    private val maxBodyBytes: Int = DEFAULT_MAX_BODY_BYTES,
    private val logger: QuillLogger = Quill.logger(ORIGIN),
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : ConduitInterceptor {

    override suspend fun intercept(chain: ConduitInterceptor.Chain): HttpResponse {
        if (level == BodyLevel.None) return chain.proceed(chain.request)

        val request = chain.request
        val requestId = newRequestId()
        logger.info("http_request") {
            "requestId" to requestId
            "method" to request.method.value
            "url" to request.url
            appendPayload(headers = { request.headers }, body = { request.body })
        }

        val start = timeSource.markNow()
        val response = try {
            chain.proceed(request)
        } catch (exception: IOException) {
            logger.warn("http_failure", throwable = exception) {
                "requestId" to requestId
                "kind" to "network"
                "durationMs" to start.elapsedNow().inWholeMilliseconds
            }
            throw exception
        }
        val durationMs = start.elapsedNow().inWholeMilliseconds

        if (response.isSuccessful) {
            logger.info("http_response") {
                "requestId" to requestId
                "code" to response.code
                "durationMs" to durationMs
                appendPayload(headers = { response.headers }, body = { response.body })
            }
        } else {
            logger.warn("http_failure") {
                "requestId" to requestId
                "kind" to "http"
                "code" to response.code
                "durationMs" to durationMs
                appendPayload(headers = { response.headers }, body = { response.body })
            }
        }
        return response
    }

    private fun QuillFieldBuilder.appendPayload(
        headers: () -> Headers,
        body: () -> ByteArray?,
    ) {
        if (level >= BodyLevel.Headers) {
            "headers" to headers().toRedactedJson(redactHeaders)
        }
        if (level >= BodyLevel.Body) {
            body()?.let { "body" to renderBody(it) }
        }
    }

    private fun renderBody(body: ByteArray): String {
        if (body.size <= maxBodyBytes) {
            return decodeUtf8OrNull(body, endOfInput = true)
                ?: "(binary body, ${body.size} bytes)"
        }
        val prefix = body.copyOf(maxBodyBytes)
        // endOfInput = false: a multibyte character cut by the cap is left undecoded
        // instead of reported as malformed, so truncation never misclassifies text
        // as binary.
        val decoded = decodeUtf8OrNull(prefix, endOfInput = false)
            ?: return "(binary body, ${body.size} bytes)"
        val omitted = body.size - decoded.toByteArray(Charsets.UTF_8).size
        return "$decoded… (+$omitted bytes)"
    }

    /** Strict UTF-8 decode: returns null on any malformed or unmappable sequence. */
    private fun decodeUtf8OrNull(bytes: ByteArray, endOfInput: Boolean): String? {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val out = CharBuffer.allocate(bytes.size)
        val result = decoder.decode(ByteBuffer.wrap(bytes), out, endOfInput)
        if (result.isError) return null
        if (endOfInput && decoder.flush(out).isError) return null
        out.flip()
        return out.toString()
    }

    private fun newRequestId(): String =
        Random.nextBytes(REQUEST_ID_BYTES).joinToString("") { byte ->
            "%02x".format(byte)
        }

    private companion object {
        private const val ORIGIN = "Http"
        private const val REQUEST_ID_BYTES = 4
        private const val DEFAULT_MAX_BODY_BYTES = 65_536
    }
}
