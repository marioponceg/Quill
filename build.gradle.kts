plugins {
    // Loaded (not applied) at the root so plugin classes live in one classloader shared
    // by build-logic's convention plugins and future root-level plugins (same remedy as
    // Conduit's shared-classloader fixes).
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.binary.compatibility.validator)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

apiValidation {
    // The demo app is never published; its surface is not API.
    // quill-android uses AGP 9 built-in Kotlin (no org.jetbrains.kotlin.android plugin
    // applied); BCV 0.18.0 does not register apiDump/apiCheck for it (confirmed: no
    // such task exists for :quill-android). Its public surface (LogcatSink) is small and
    // its api(quill-core) dependency stays validated, so the gap is acceptable until BCV
    // supports built-in Kotlin. See AGENTS.md for the tracked exclusion.
    ignoredProjects += listOf("demo", "quill-android")
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}
