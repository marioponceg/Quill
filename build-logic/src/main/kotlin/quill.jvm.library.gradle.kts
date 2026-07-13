import org.gradle.accessors.dm.LibrariesForLibs

/**
 * Convention plugin for Quill's pure-JVM library modules: Kotlin/JVM with explicit API
 * mode, Kover, and the JUnit 5 platform with kotlin-test. Verification enforces the
 * project's 90% minimum line coverage.
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

kover {
    reports {
        verify {
            rule("Minimum line coverage") {
                minBound(90)
            }
        }
    }
}

dependencies {
    testImplementation(libs.kotlin.test.junit5)
}
