plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt)
}

android {
    namespace = "io.github.marioponceg.quill.android"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }

    kotlin {
        explicitApi()
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

dependencies {
    api(project(":quill-core"))
    testImplementation(libs.kotlin.test.junit5)
    detektPlugins(libs.detekt.formatting)
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
