package io.github.marioponceg.quill

import kotlin.test.Test
import kotlin.test.assertEquals

class QuillTest {

    @Test
    fun `version is the current development version`() {
        assertEquals("0.1.0-SNAPSHOT", Quill.VERSION)
    }
}
