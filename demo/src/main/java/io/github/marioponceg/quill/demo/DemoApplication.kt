package io.github.marioponceg.quill.demo

import android.app.Application
import io.github.marioponceg.quill.QuillLevel
import io.github.marioponceg.quill.android.LogcatSink
import io.github.marioponceg.quill.quill

class DemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        quill {
            minLevel = QuillLevel.Verbose
            addSink(LogcatSink())
        }
    }
}
