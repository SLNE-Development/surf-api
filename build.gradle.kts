import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14" apply false
    alias(libs.plugins.dokka)
}

allprojects {
    repositories {
        mavenCentral()
    }

    if (subprojects.isEmpty()) {
        apply(plugin = rootProject.libs.plugins.dokka.get().pluginId)
    }
}

dependencies {
    dokka(project(":surf-api-core:surf-api-core-api"))
    dokka(project(":surf-api-bukkit:surf-api-bukkit-api"))
    dokka(project(":surf-api-velocity:surf-api-velocity-api"))
}

dokka {
    dokkaSourceSets.configureEach {
        documentedVisibilities = setOf(VisibilityModifier.Public, VisibilityModifier.Protected)
    }
}
