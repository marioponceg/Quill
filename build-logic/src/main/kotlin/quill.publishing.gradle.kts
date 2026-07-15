/**
 * Convention plugin for Quill's published modules: Dokka and Maven Central publishing via
 * the vanniktech plugin. Coordinates and POM metadata come from gradle.properties (GROUP,
 * VERSION_NAME, POM_*) plus each module's own gradle.properties (artifact id, name,
 * description). Applied by quill.jvm.library and directly by quill-android; never by demo.
 */
plugins {
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    // Uploads to the Central Portal; actual publication is released manually from
    // central.sonatype.com (publishing to Maven Central is irreversible by design).
    publishToMavenCentral()
    // Signing is mandatory for Maven Central but must not block local publishToMavenLocal:
    // sign only when the in-memory key is present (always true in the release workflow).
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
}
