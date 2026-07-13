package io.github.marioponceg.quill

/**
 * One structured log event: not a sentence, an event with a name and typed fields.
 */
public data class QuillEvent(
    /** Epoch millis at which the event was created. */
    val timestamp: Long,
    val level: QuillLevel,
    /** Where the event came from — the logger's tag, e.g. "AuthRepository". */
    val origin: String,
    /** The event name, e.g. "user_login". */
    val name: String,
    val fields: Map<String, QuillValue>,
    val throwable: Throwable? = null,
)
