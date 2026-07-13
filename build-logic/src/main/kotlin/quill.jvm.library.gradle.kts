import org.gradle.accessors.dm.LibrariesForLibs

/**
 * Convention plugin for Quill's pure-JVM library modules: Kotlin/JVM with explicit API
 * mode, Kover, and the JUnit 5 platform with kotlin-test. The minimum-coverage rule is
 * added in PR #2 together with the first real code.
 */
plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.kover")
}

val libs = the<LibrariesForLibs>()

kotlin {
    explicitApi()
    jvmToolchain(21)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    testImplementation(libs.kotlin.test.junit5)
}
