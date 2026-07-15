package io.github.marioponceg.quill.android

import android.util.Log
import io.github.marioponceg.quill.QuillEvent
import io.github.marioponceg.quill.QuillLevel
import io.github.marioponceg.quill.QuillSink

/**
 * Renders events as pretty, boxed, multi-line logcat output:
 *
 * ```
 * quill {
 *     addSink(LogcatSink())
 * }
 * ```
 *
 * The logcat tag is `"$tagPrefix.$origin"` (default prefix `"Quill"`), so
 * Android Studio filters like `tag~:^Quill\.` match all Quill logs and
 * `tag:Quill.AuthRepository` matches one origin; pass `tagPrefix = null` for
 * bare origins. [minLevel] filters on top of the global `quill { }` level.
 * `boxed = false` renders flat single lines. Messages are chunked at 4000 UTF-8
 * bytes so large payloads survive logcat truncation.
 */
public class LogcatSink internal constructor(
    private val tagPrefix: String?,
    private val minLevel: QuillLevel,
    private val boxed: Boolean,
    private val printer: (priority: Int, tag: String, message: String) -> Unit,
) : QuillSink {

    public constructor(
        tagPrefix: String? = "Quill",
        minLevel: QuillLevel = QuillLevel.Verbose,
        boxed: Boolean = true,
    ) : this(tagPrefix, minLevel, boxed, { priority, tag, message ->
        Log.println(priority, tag, message)
    })

    private val formatter = LogcatFormatter(boxed)

    override fun write(event: QuillEvent) {
        if (event.level < minLevel) return
        val tag = if (tagPrefix == null) event.origin else "$tagPrefix.${event.origin}"
        val priority = event.level.toLogcatPriority()
        val continuationPrefix = if (boxed) "│ " else ""
        for (chunk in LogcatChunker.chunk(formatter.format(event), continuationPrefix)) {
            printer(priority, tag, chunk)
        }
    }
}
