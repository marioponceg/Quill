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
    alias(libs.plugins.dokka) apply false
    // maven-publish additionally NEEDS the shared classloader: its Central staging build
    // service must be one type across sibling modules or publishing fails to configure.
    alias(libs.plugins.maven.publish) apply false
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

apiValidation {
    // The demo app is never published; quill-android is excluded for BCV/AGP
    // compatibility — see AGENTS.md.
    ignoredProjects += listOf("demo", "quill-android")
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}
