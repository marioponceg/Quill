package io.github.marioponceg.quill

import kotlin.test.Test
import kotlin.test.assertTrue

class QuillLevelTest {

    @Test
    fun `levels are ordered by severity`() {
        assertTrue(QuillLevel.Verbose < QuillLevel.Debug)
        assertTrue(QuillLevel.Debug < QuillLevel.Info)
        assertTrue(QuillLevel.Info < QuillLevel.Warn)
        assertTrue(QuillLevel.Warn < QuillLevel.Error)
    }
}
