package io.github.marioponceg.quill

/**
 * A typed field value. Sinks know what they hold without type checks on arbitrary
 * objects. Users never construct these directly — the field builder converts
 * automatically ([QuillFieldBuilder]).
 */
public sealed interface QuillValue {
    /** A plain string that is not JSON. */
    public data class Text(val value: String) : QuillValue

    /** Any numeric value. */
    public data class Number(val value: kotlin.Number) : QuillValue

    /** A boolean. */
    public data class Bool(val value: Boolean) : QuillValue

    /**
     * Structured content stored raw and unformatted: a JSON string or a data class
     * `toString()`. Pretty-printing is a sink concern, so structure is never lost
     * at the call site.
     */
    public data class Structured(val raw: String) : QuillValue

    /** An explicit null. */
    public data object Null : QuillValue
}
