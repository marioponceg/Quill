package io.github.marioponceg.quill.conduit

import io.github.marioponceg.conduit.http.Headers
import io.github.marioponceg.conduit.http.HttpRequest
import io.github.marioponceg.conduit.http.HttpResponse
import io.github.marioponceg.conduit.interceptor.ConduitInterceptor
import io.github.marioponceg.quill.Quill
import io.github.marioponceg.quill.QuillFieldBuilder
import io.github.marioponceg.quill.QuillLogger
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
        val response = chain.proceed(request)
        val durationMs = start.elapsedNow().inWholeMilliseconds

        logger.info("http_response") {
            "requestId" to requestId
            "code" to response.code
            "durationMs" to durationMs
            appendPayload(headers = { response.headers }, body = { response.body })
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
            body()?.let { "body" to it.toString(Charsets.UTF_8) }
        }
    }

    private fun newRequestId(): String =
        Random.nextBytes(REQUEST_ID_BYTES).joinToString("") { byte ->
            "%02x".format(byte)
        }

    private companion object {
        private const val ORIGIN = "Http"
        private const val REQUEST_ID_BYTES = 4
    }
}
