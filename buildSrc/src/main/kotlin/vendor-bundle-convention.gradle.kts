import org.gradle.api.artifacts.component.ModuleComponentIdentifier

/**
 * Bundles the vendor builds in `vendor/` into the module's own jar, the way surf-minestom-lobby
 * bundles them into its server jar.
 *
 * The vendor artifacts are built from source and published nowhere, so they cannot appear as
 * dependencies of a published module. Declare them in the `bundled` configuration instead: their
 * classes, and those of everything they pull in, are copied into the jar, except for the libraries
 * a Minestom server already provides.
 */

plugins {
    id("core-convention")
}

/** Libraries a Minestom server provides itself, which are therefore never bundled. */
private val providedGroups = setOf(
    "org.slf4j",
    "org.jspecify",
    "org.jetbrains",
    "org.jetbrains.kotlin",
    "org.jetbrains.kotlinx",
    "org.checkerframework",
    "com.google.code.gson",
    "com.google.guava",
    "com.google.errorprone",
)

/** Adventure ships with Minestom, apart from the few extensions spark brings along. */
private val providedAdventureModules = Regex("adventure-(api|key|nbt|text-(minimessage|logger-slf4j|serializer-.*))|option|examination-.*")

private fun isProvided(id: ModuleComponentIdentifier): Boolean =
    id.group in providedGroups ||
            (id.group == "net.kyori" && providedAdventureModules.matches(id.module))

val bundled: Configuration = configurations.create("bundled") {
    description = "Vendor builds copied into this module's jar instead of being published as dependencies"
    isCanBeConsumed = false
    isCanBeResolved = true
}

configurations.compileOnly { extendsFrom(bundled) }
configurations.testImplementation { extendsFrom(bundled) }

val bundledFiles = bundled.incoming.artifactView {
    componentFilter { id -> id !is ModuleComponentIdentifier || !isProvided(id) }
}.files

tasks.jar {
    inputs.files(bundledFiles)

    from(bundledFiles.elements.map { files -> files.map { zipTree(it.asFile) } }) {
        exclude(
            "module-info.class",
            "META-INF/versions/*/module-info.class",
            "META-INF/MANIFEST.MF",
            "META-INF/*.SF",
            "META-INF/*.DSA",
            "META-INF/*.RSA",
            "META-INF/maven/**",
        )
    }
}
