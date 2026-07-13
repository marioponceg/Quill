package io.github.marioponceg.quill.android

import android.util.Log
import io.github.marioponceg.quill.QuillLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class LogcatPrioritiesTest {

    @Test
    fun `maps each QuillLevel to its android priority`() {
        assertEquals(Log.VERBOSE, QuillLevel.Verbose.toLogcatPriority())
        assertEquals(Log.DEBUG, QuillLevel.Debug.toLogcatPriority())
        assertEquals(Log.INFO, QuillLevel.Info.toLogcatPriority())
        assertEquals(Log.WARN, QuillLevel.Warn.toLogcatPriority())
        assertEquals(Log.ERROR, QuillLevel.Error.toLogcatPriority())
    }
}
