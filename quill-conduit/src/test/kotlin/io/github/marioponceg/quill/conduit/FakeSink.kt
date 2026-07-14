package io.github.marioponceg.quill.conduit

import io.github.marioponceg.quill.QuillEvent
import io.github.marioponceg.quill.QuillSink

/** In-memory sink for assertions; quill-core's copy is test-only and not published. */
class FakeSink : QuillSink {
    val events = mutableListOf<QuillEvent>()

    override fun write(event: QuillEvent) {
        events += event
    }
}
