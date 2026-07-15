plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kover.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    // Makes the typed `libs` catalog accessor available inside precompiled script plugins
    // (standard workaround for https://github.com/gradle/gradle/issues/15383).
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
