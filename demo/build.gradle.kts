plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.marioponceg.quill.demo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.marioponceg.quill.demo"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(project(":quill-android"))
    testImplementation(libs.junit)
}