package io.github.marioponceg.quill

/** In-memory sink for assertions in tests. */
class FakeSink : QuillSink {
    val events = mutableListOf<QuillEvent>()

    override fun write(event: QuillEvent) {
        events += event
    }

    fun single(): QuillEvent = events.single()
}
