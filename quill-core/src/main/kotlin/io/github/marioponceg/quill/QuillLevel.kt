package io.github.marioponceg.quill

/**
 * Log severity. Android's five levels, nothing invented. Declaration order defines
 * severity, so `level >= minLevel` works via the enum's natural [Comparable].
 */
public enum class QuillLevel {
    Verbose,
    Debug,
    Info,
    Warn,
    Error,
}
