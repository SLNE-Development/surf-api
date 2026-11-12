import java.util.*

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "surf-api-gradle-plugin"

// An included build does not read the gradle.properties of the build that includes it, so the properties the build
// script depends on (group, version, javaVersion, ...) are taken from the main build. Properties given with -P win.
val mainProperties = Properties().apply {
    rootDir.resolve("../../gradle.properties").reader().use(::load)
}

gradle.beforeProject {
    val overrides = gradle.startParameter.projectProperties

    mainProperties.forEach { (key, value) ->
        val name = key as String
        if (name in overrides) return@forEach

        // Same as Gradle does for gradle.properties: built-in properties such as group and version are assigned,
        // all others become extra properties.
        if (hasProperty(name)) setProperty(name, value) else extensions.extraProperties[name] = value
    }
}
