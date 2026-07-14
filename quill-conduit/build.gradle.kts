plugins {
    id("quill.jvm.library")
}

dependencies {
    api(project(":quill-core"))
    api(libs.conduit.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
