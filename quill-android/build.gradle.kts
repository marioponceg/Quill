import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt)
    id("quill.publishing")
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

mavenPublishing {
    configure(
        AndroidSingleVariantLibrary(
            variant = "release",
            sourcesJar = true,
            // AGP 9's built-in-Kotlin javadoc generation task (javaDocReleaseGeneration) drives
            // Dokka through descriptor-based analysis, which fails to resolve this module's
            // compiled classes. Ship an empty javadoc jar instead so the publication still
            // satisfies Maven Central's javadoc-jar requirement; revisit on Dokka/AGP/vanniktech
            // upgrades (a JavadocJar.Empty()-style option would remove the afterEvaluate below).
            publishJavadocJar = false,
        ),
    )
}

val emptyJavadocJar = tasks.register<Jar>("emptyJavadocJar") {
    archiveClassifier.set("javadoc")
}

afterEvaluate {
    publishing.publications.withType<MavenPublication>().configureEach {
        artifact(emptyJavadocJar)
    }
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
