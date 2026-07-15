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
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}
