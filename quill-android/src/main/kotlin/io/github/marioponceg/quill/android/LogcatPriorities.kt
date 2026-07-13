package io.github.marioponceg.quill.android

import android.util.Log
import io.github.marioponceg.quill.QuillLevel

internal fun QuillLevel.toLogcatPriority(): Int = when (this) {
    QuillLevel.Verbose -> Log.VERBOSE
    QuillLevel.Debug -> Log.DEBUG
    QuillLevel.Info -> Log.INFO
    QuillLevel.Warn -> Log.WARN
    QuillLevel.Error -> Log.ERROR
}
