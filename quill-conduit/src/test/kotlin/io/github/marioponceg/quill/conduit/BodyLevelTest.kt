package io.github.marioponceg.quill.conduit

import kotlin.test.Test
import kotlin.test.assertTrue

class BodyLevelTest {

    @Test
    fun `declaration order defines verbosity`() {
        assertTrue(BodyLevel.None < BodyLevel.Basic)
        assertTrue(BodyLevel.Basic < BodyLevel.Headers)
        assertTrue(BodyLevel.Headers < BodyLevel.Body)
    }
}
